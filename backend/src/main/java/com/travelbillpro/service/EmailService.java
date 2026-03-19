package com.travelbillpro.service;

import com.travelbillpro.entity.Company;
import com.travelbillpro.entity.Invoice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final LocalFileStorageService fileStorageService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendInvoiceEmail(Invoice invoice) {
        Company company = invoice.getCompany();
        String toAddress = company.getBillingEmail();

        if (toAddress == null || toAddress.trim().isEmpty()) {
            log.warn("Cannot send invoice {}: No billing email for company {}", invoice.getInvoiceNumber(), company.getName());
            return;
        }

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            // true flag indicates a multipart message (for attachments)
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(toAddress);
            helper.setSubject("Invoice " + invoice.getInvoiceNumber() + " from TravelBill Pro");

            String body = """
                Dear %s Team,
                
                Please find attached your invoice %s for the billing period %s to %s.
                
                Total Amount Due: Rs. %s
                Due Date: %s
                
                Thank you for your business.
                
                Regards,
                TravelBill Pro Billing Team
                """.formatted(
                    company.getName(),
                    invoice.getInvoiceNumber(),
                    invoice.getBillingPeriodStart(),
                    invoice.getBillingPeriodEnd(),
                    invoice.getGrandTotal(),
                    invoice.getDueDate()
            );

            helper.setText(body);

            // Attach PDF invoice
            if (invoice.getFilePath() != null) {
                Resource pdfResource = fileStorageService.loadFileAsResource(invoice.getFilePath());
                helper.addAttachment(invoice.getInvoiceNumber() + ".pdf", pdfResource.getFile());
            }

            javaMailSender.send(message);
            log.info("Invoice {} sent successfully to {}", invoice.getInvoiceNumber(), toAddress);

        } catch (MessagingException | IOException e) {
            log.error("Failed to send email for invoice {}", invoice.getInvoiceNumber(), e);
            // In a real system, we might throw a business exception here to let the caller know sending failed
        }
    }
}
