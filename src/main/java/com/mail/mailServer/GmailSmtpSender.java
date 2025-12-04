package com.mail.mailServer;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class GmailSmtpSender {

    public static void main(String[] args) {
        final String username = "XXXX@gmail.com"; // 你的 Gmail
        final String password = "hyfyolavhmsvbuvi"; // 16 碼應用程式密碼

        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true"); // TLS

        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("therule99095@gmail.com"));
            message.setSubject("測試 SMTP 永久寄信");
            message.setText("這是一封不會過期的信，不用每 7 天登入一次！");

            Transport.send(message);

            System.out.println("郵件已成功寄出！");

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}