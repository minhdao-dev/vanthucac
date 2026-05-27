package com.vanthucac.notification.service;

import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    public EmailNotificationService(
            JavaMailSender mailSender,
            SpringTemplateEngine templateEngine
    ) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    public void sendAuctionWinnerNotification(
            String recipientEmail,
            String winnerFullName,
            String bookTitle,
            BigDecimal winningPrice,
            Long auctionItemId
    ) {
        var html = renderAuctionWinnerTemplate(winnerFullName, bookTitle, winningPrice);
        var subject = "Chúc mừng! Bạn đã thắng phiên đấu giá — " + bookTitle;

        sendHtmlEmail(recipientEmail, subject, html);
        log.info("Winner email sent to {} for auction item {}", recipientEmail, auctionItemId);
    }

    private String renderAuctionWinnerTemplate(
            String winnerFullName,
            String bookTitle,
            BigDecimal winningPrice
    ) {
        var context = new Context();
        context.setVariable("winnerFullName", winnerFullName);
        context.setVariable("bookTitle", bookTitle);
        context.setVariable("winningPrice", winningPrice.toPlainString());
        return templateEngine.process("email/auction-winner", context);
    }

    private void sendHtmlEmail(String recipientEmail, String subject, String html) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
        } catch (MessagingException ex) {
            throw new IllegalStateException("Failed to build HTML email message", ex);
        }
    }
}