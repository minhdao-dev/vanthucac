package com.vanthucac.notification.service;

import com.vanthucac.auction.entity.AuctionItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;

    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendAuctionWinnerNotification(AuctionItem item) {
        if (item.getWinner() == null) {
            log.info("Auction item {} has no winner — skipping email", item.getId());
            return;
        }

        var winner = item.getWinner();
        var bookTitle = item.getBookCatalog().getTitle();
        var winningPrice = item.getCurrentPrice();

        try {
            var message = new SimpleMailMessage();
            message.setTo(winner.getEmail());
            message.setSubject("Chúc mừng! Bạn đã thắng phiên đấu giá — " + bookTitle);
            message.setText("""
                    Xin chào %s,
                    
                    Chúc mừng bạn đã thắng phiên đấu giá cho cuốn sách:
                    
                    📚 Sách: %s
                    💰 Giá thắng: %s VND
                    
                    Chúng tôi sẽ liên hệ với bạn sớm để hoàn tất giao dịch.
                    
                    Trân trọng,
                    Vạn Thư Các
                    """.formatted(winner.getFullName(), bookTitle, winningPrice.toPlainString()));

            mailSender.send(message);
            log.info("Winner email sent to {} for auction item {}", winner.getEmail(), item.getId());

        } catch (Exception e) {
            log.error("Failed to send winner email to {} — {}", winner.getEmail(), e.getMessage());
        }
    }
}