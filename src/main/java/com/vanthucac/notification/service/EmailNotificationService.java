package com.vanthucac.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;

    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendAuctionWinnerNotification(
            String recipientEmail,
            String winnerFullName,
            String bookTitle,
            BigDecimal winningPrice,
            Long auctionItemId
    ) {
        var message = new SimpleMailMessage();
        message.setTo(recipientEmail);
        message.setSubject("Chúc mừng! Bạn đã thắng phiên đấu giá — " + bookTitle);
        message.setText("""
                Xin chào %s,
                
                Chúc mừng bạn đã thắng phiên đấu giá cho cuốn sách:
                
                Sách: %s
                Giá thắng: %s VND
                
                Chúng tôi sẽ liên hệ với bạn sớm để hoàn tất giao dịch.
                
                Trân trọng,
                Vạn Thư Các
                """.formatted(winnerFullName, bookTitle, winningPrice.toPlainString()));

        mailSender.send(message);
        log.info("Winner email sent to {} for auction item {}", recipientEmail, auctionItemId);
    }
}