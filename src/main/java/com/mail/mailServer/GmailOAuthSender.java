package com.mail.mailServer;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class GmailOAuthSender {
    // 應用程式名稱 (顯示在 Google 授權頁面上)
    private static final String APPLICATION_NAME = "myGmailServer";
    
    // 用於處理 JSON 的工廠類別
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    
    // 定義授權範圍 (Scope)：這裡只請求 "寄送郵件" 的權限
    private static final List<String> SCOPES = Collections.singletonList("https://www.googleapis.com/auth/gmail.send");

    public static void main(String[] args) throws Exception {
        // 1. 取得經過 OAuth 驗證的 Gmail API 服務物件
        Gmail service = getGmailService();
        try {
            // 2. 建立郵件內容並發送
            // 參數說明: (Gmail服務, userId "me" 代表當前授權用戶, 建立好的 MimeMessage 物件)
            sendMessage(service, "me", createEmail("therule99095@gmail.com", "信件標題", "信件內容"));
            System.out.println("郵件已成功寄出！");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("郵件寄送失敗：" + e.getMessage());
        }
    }

    /**
     * 初始化並取得 Gmail API 客戶端服務
     */
    public static Gmail getGmailService() throws Exception {
        // 注意：此行會刪除舊的 Token 檔案，導致每次執行程式都需要重新彈出瀏覽器登入
        // 若希望記住登入狀態，請註解掉這一行
//        clearStoredTokens(); 

        // 讀取 resources 目錄下的 credentials.json (OAuth 2.0 客戶端 ID 憑證)
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(GmailOAuthSender.class.getResourceAsStream("/credentials.json")));
        
        // 設定 OAuth 2.0 授權流程
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, clientSecrets, SCOPES)
                .setAccessType("offline") // 允許離線存取 (取得 Refresh Token)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File("tokens"))) // 設定 Token 儲存的資料夾名稱
                .build();
        
        // 啟動本地接收器 (LocalServerReceiver)，這會自動開啟瀏覽器進行授權流程
        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
        
        // 建立並回傳 Gmail 客戶端
        return new Gmail.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * 建立 MIME 格式的電子郵件物件
     * @param to 收件者信箱
     * @param subject 信件主旨
     * @param bodyText 信件內文
     */
    public static MimeMessage createEmail(String to, String subject, String bodyText) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
//        email.setFrom(new InternetAddress(from)); // 若不設定 From，Gmail API 會自動使用授權帳號作為寄件者
        email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        
        // --- 功能特色：自動加入時間戳記 ---
        // 取得當前日期與時間
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = now.format(formatter);

        // 將日期與時間附加到信件內容後方
        String fullBodyText = bodyText + "\n這是一封測式信件\n\nSent on: " + formattedDateTime;
        email.setText(fullBodyText);
        
        return email;
    }

    /**
     * 將 MimeMessage 編碼並透過 Gmail API 發送 
     */
    public static Message sendMessage(Gmail service, String userId, MimeMessage email) throws MessagingException, IOException {
        // 將 MimeMessage 寫入 ByteArray 緩衝區
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        
        // Gmail API 要求使用 Base64 URL Safe 編碼格式
        String encodedEmail = Base64.getUrlEncoder().encodeToString(bytes);
        
        Message message = new Message();
        message.setRaw(encodedEmail);
        
        // 呼叫 API 執行發送動作
        return service.users().messages().send(userId, message).execute();
    }
    
    /**
     * 清除儲存在本地 "tokens" 資料夾中的憑證檔案
     * 用途：強制使用者在下次執行時重新進行 OAuth 授權
     */
    public static void clearStoredTokens() throws IOException {
        Path tokensPath = Paths.get("tokens");
        if (Files.exists(tokensPath)) {
            Files.walk(tokensPath)
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }
}