package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.entity.Invoice;
import com.gstbilling.gst_billing.entity.InvoiceItem;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@gstbilling.com}")
    private String fromEmail;

    @Value("${app.mail.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean sendPasswordResetEmail(String toEmail, String userName, String resetToken) {
        String subject = "Password Reset Request - GST Billing";
        String resetLink = frontendUrl + "/#reset-token=" + resetToken;

        String htmlContent = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 8px; background-color: #ffffff;">
                <div style="border-bottom: 2px solid #2563eb; padding-bottom: 12px; margin-bottom: 20px;">
                    <h2 style="color: #1e293b; margin: 0;">GST Billing System</h2>
                </div>
                <p style="font-size: 16px; color: #334155;">Hello %s,</p>
                <p style="font-size: 15px; color: #475569; line-height: 1.5;">
                    We received a request to reset your password. Use the verification token below to reset your password:
                </p>
                <div style="background-color: #f1f5f9; border-radius: 6px; padding: 16px; text-align: center; margin: 24px 0;">
                    <span style="font-size: 28px; font-weight: bold; letter-spacing: 4px; color: #2563eb;">%s</span>
                </div>
                <p style="font-size: 14px; color: #64748b;">
                    Enter this code in the password reset form on the application. This token will expire in <strong>30 minutes</strong>.
                </p>
                <p style="font-size: 14px; color: #64748b;">
                    If you did not request a password reset, you can safely ignore this email.
                </p>
                <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 24px 0;" />
                <p style="font-size: 12px; color: #94a3b8; text-align: center;">
                    GST Billing System · Keep your business financial records secure.
                </p>
            </div>
            """.formatted(userName != null ? userName : "User", resetToken);

        return sendHtmlEmail(toEmail, subject, htmlContent, "Password Reset Token: " + resetToken);
    }

    public boolean sendPasswordChangedEmail(String toEmail, String userName) {
        String subject = "Security Alert: Password Changed - GST Billing";

        String htmlContent = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 8px; background-color: #ffffff;">
                <div style="border-bottom: 2px solid #10b981; padding-bottom: 12px; margin-bottom: 20px;">
                    <h2 style="color: #1e293b; margin: 0;">GST Billing System</h2>
                </div>
                <p style="font-size: 16px; color: #334155;">Hello %s,</p>
                <p style="font-size: 15px; color: #475569; line-height: 1.5;">
                    This is a confirmation that the password for your GST Billing account (<strong>%s</strong>) has been changed successfully.
                </p>
                <div style="background-color: #ecfdf5; border-left: 4px solid #10b981; padding: 12px 16px; margin: 20px 0; border-radius: 4px;">
                    <p style="margin: 0; color: #065f46; font-size: 14px;">
                        If you made this change, no further action is needed.
                    </p>
                </div>
                <p style="font-size: 14px; color: #ef4444; line-height: 1.5;">
                    If you did not make this change, please reset your password immediately or contact your administrator.
                </p>
                <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 24px 0;" />
                <p style="font-size: 12px; color: #94a3b8; text-align: center;">
                    GST Billing System · Secure Multi-Tenant Billing Solution
                </p>
            </div>
            """.formatted(userName != null ? userName : "User", toEmail);

        return sendHtmlEmail(toEmail, subject, htmlContent, "Password changed confirmation for " + toEmail);
    }

    public boolean sendInvoiceReminderEmail(Invoice invoice, String customMessage) {
        if (invoice == null || invoice.getCustomer() == null || invoice.getCustomer().getEmail() == null || invoice.getCustomer().getEmail().isBlank()) {
            log.warn("Cannot send invoice reminder email: Customer email is missing for invoice ID {}", invoice != null ? invoice.getId() : null);
            return false;
        }

        String toEmail = invoice.getCustomer().getEmail().trim();
        String customerName = invoice.getCustomer().getName();
        String businessName = invoice.getBusiness() != null ? invoice.getBusiness().getName() : "Billing Department";
        String invoiceNumber = invoice.getInvoiceNumber();
        BigDecimal grandTotal = invoice.getGrandTotal() != null ? invoice.getGrandTotal().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        String formattedAmount = "Rs. " + grandTotal;

        String subject = "Payment Reminder: Invoice #" + invoiceNumber + " from " + businessName;

        StringBuilder itemsTable = new StringBuilder();
        if (invoice.getItems() != null && !invoice.getItems().isEmpty()) {
            itemsTable.append("<table style='width: 100%; border-collapse: collapse; margin-top: 15px;'>");
            itemsTable.append("<tr style='background-color: #f8fafc; text-align: left; font-size: 13px; color: #475569;'>")
                    .append("<th style='padding: 8px; border-bottom: 1px solid #e2e8f0;'>Product</th>")
                    .append("<th style='padding: 8px; border-bottom: 1px solid #e2e8f0;'>Qty</th>")
                    .append("<th style='padding: 8px; border-bottom: 1px solid #e2e8f0;'>Price</th>")
                    .append("<th style='padding: 8px; border-bottom: 1px solid #e2e8f0;'>Total</th>")
                    .append("</tr>");

            for (InvoiceItem item : invoice.getItems()) {
                itemsTable.append("<tr style='font-size: 13px; color: #1e293b; border-bottom: 1px solid #f1f5f9;'>")
                        .append("<td style='padding: 8px;'>").append(item.getProductName()).append("</td>")
                        .append("<td style='padding: 8px;'>").append(item.getQuantity()).append("</td>")
                        .append("<td style='padding: 8px;'>Rs. ").append(item.getUnitPrice()).append("</td>")
                        .append("<td style='padding: 8px;'>Rs. ").append(item.getTotalAmount()).append("</td>")
                        .append("</tr>");
            }
            itemsTable.append("</table>");
        }

        String noteParagraph = (customMessage != null && !customMessage.isBlank())
                ? "<p style='font-size: 14px; background: #fffbeb; border-left: 3px solid #f59e0b; padding: 10px 14px; margin: 16px 0; color: #92400e;'>" + customMessage + "</p>"
                : "";

        String htmlContent = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 8px; background-color: #ffffff;">
                <div style="border-bottom: 2px solid #2563eb; padding-bottom: 12px; margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center;">
                    <h2 style="color: #1e293b; margin: 0;">%s</h2>
                </div>
                <p style="font-size: 16px; color: #334155;">Dear <strong>%s</strong>,</p>
                <p style="font-size: 14px; color: #475569; line-height: 1.5;">
                    This is a friendly reminder that payment for invoice <strong>#%s</strong> is currently pending.
                </p>
                %s
                <div style="background-color: #f8fafc; border-radius: 6px; padding: 16px; margin: 20px 0; border: 1px solid #e2e8f0;">
                    <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
                        <span style="color: #64748b; font-size: 14px;">Invoice Date:</span>
                        <strong style="color: #1e293b; font-size: 14px;">%s</strong>
                    </div>
                    <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
                        <span style="color: #64748b; font-size: 14px;">Status:</span>
                        <span style="background-color: #fef3c7; color: #92400e; padding: 2px 8px; border-radius: 4px; font-weight: bold; font-size: 12px;">%s</span>
                    </div>
                    <div style="display: flex; justify-content: space-between; padding-top: 8px; border-top: 1px solid #cbd5e1; font-size: 16px;">
                        <strong style="color: #1e293b;">Total Amount Due:</strong>
                        <strong style="color: #2563eb; font-size: 18px;">%s</strong>
                    </div>
                </div>

                %s

                <p style="font-size: 14px; color: #475569; line-height: 1.5; margin-top: 20px;">
                    Kindly settle this invoice at your earliest convenience. If you have already made the payment, please disregard this reminder.
                </p>
                <p style="font-size: 14px; color: #475569; margin-top: 16px;">
                    Thank you for your business!<br/>
                    <strong>%s</strong>
                </p>
                <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 24px 0;" />
                <p style="font-size: 12px; color: #94a3b8; text-align: center;">
                    Generated by GST Billing System · Automated Reminder Service
                </p>
            </div>
            """.formatted(
                businessName,
                customerName != null ? customerName : "Customer",
                invoiceNumber,
                noteParagraph,
                invoice.getInvoiceDate() != null ? invoice.getInvoiceDate().toString() : "N/A",
                invoice.getStatus() != null ? invoice.getStatus() : "UNPAID",
                formattedAmount,
                itemsTable.toString(),
                businessName
        );

        String fallbackLog = "Payment reminder for Invoice #" + invoiceNumber + " (Total: " + formattedAmount + ") sent to " + toEmail;
        return sendHtmlEmail(toEmail, subject, htmlContent, fallbackLog);
    }

    private boolean sendHtmlEmail(String toEmail, String subject, String htmlContent, String fallbackDescription) {
        log.info("Sending Email to: {} | Subject: {}", toEmail, subject);

        if (mailSender == null) {
            log.info("--- [DEV MODE / NO SMTP CONFIGURED] ---");
            log.info("Email would be sent to: {}", toEmail);
            log.info("Subject: {}", subject);
            log.info("Content summary: {}", fallbackDescription);
            log.info("---------------------------------------");
            return true;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email successfully dispatched to {}", toEmail);
            return true;
        } catch (Exception e) {
            log.warn("Could not dispatch email via SMTP (will continue gracefully): {}", e.getMessage());
            log.info("--- [FALLBACK EMAIL LOG] ---");
            log.info("To: {}", toEmail);
            log.info("Subject: {}", subject);
            log.info("Summary: {}", fallbackDescription);
            log.info("----------------------------");
            return true;
        }
    }
}
