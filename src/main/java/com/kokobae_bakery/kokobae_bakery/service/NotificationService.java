package com.kokobae_bakery.kokobae_bakery.service;

import com.kokobae_bakery.kokobae_bakery.model.Order;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${bakery.email.from}")
    private String fromEmail;

    @Value("${bakery.email.fromName}")
    private String fromName;

    // Called when order is first placed (COD or Online)
    public void notifyOrderPlaced(Order order) {
        String shortId = lastEight(order.getId());
        String subject = "Order Confirmed! #" + shortId + " — Kokobae Bakery";

        String deliveryInfo = buildDeliveryInfo(order);
        String paymentInfo = buildPaymentInfo(order);
        String itemsHtml = buildItemsHtml(order);

        String body = buildEmail(
                "Hi " + order.getCustomerName() + "!",
                "Your order is confirmed and we're getting started.",
                shortId,
                itemsHtml,
                order.getTotalAmount(),
                deliveryInfo,
                paymentInfo,
                "We'll email you again when your order is on its way!"
        );

        sendEmail(order.getCustomerEmail(), subject, body);
    }

    // Called when admin updates order status
    public void notifyStatusUpdate(Order order) {
        String shortId = lastEight(order.getId());
        StatusMessage statusMessage = getStatusMessage(order.getStatus(), order.getDeliveryType());

        if (statusMessage == null) return;
        // Don't email for every status

        String subject = statusMessage.subject + " #" + shortId + " — Kokobae Bakery";

        String body = buildEmail(
                "Hi " + order.getCustomerName() + "!",
                statusMessage.subtitle,
                shortId,
                buildItemsHtml(order),
                order.getTotalAmount(),
                buildDeliveryInfo(order),
                buildPaymentInfo(order),
                statusMessage.footer
        );

        sendEmail(order.getCustomerEmail(), subject, body);
    }

    // Determines what to say per status
    private StatusMessage getStatusMessage(String status, String deliveryType) {
        return switch (status) {
            case "PROCESSING" -> new StatusMessage(
                    "Your order is being prepared",
                    "Great news — we've confirmed your order!",
                    "We'll notify you when it's ready."
            );
            case "SHIPPED" -> new StatusMessage(
                    "Your order is on its way",
                    "Your fresh bakes are out for delivery!",
                    "Expect delivery soon. Questions? WhatsApp us at +91 79959 78220."
            );
            case "READY_PICKUP" -> new StatusMessage(
                    "Your order is ready for pickup",
                    "Your order is packed and waiting for you!",
                    "Pick up from our Sainikpuri store. Mon-Sat 9AM-7PM."
            );
            case "DELIVERED" -> new StatusMessage(
                    "Order delivered!",
                    "Your Kokobae order has been delivered.",
                    "Hope you enjoy every bite! Order again at kokobae.netlify.app"
            );
            case "COLLECTED" -> new StatusMessage(
                    "Order collected!",
                    "Thanks for picking up your order!",
                    "Hope you enjoy every bite! Order again at kokobae.netlify.app"
            );
            case "CANCELLED" -> new StatusMessage(
                    "Order cancelled",
                    "Your order #" + "has been cancelled.",
                    "If you have questions, WhatsApp us at +91 79959 78220."
            );
            default -> null;
        };
    }

    record StatusMessage(String subject, String subtitle, String footer) {}

    private String buildEmail(String greeting, String subtitle, String shortId, String itemsHtml,
                              double total, String deliveryInfo, String paymentInfo, String footerMessage) {

        return """
        <!DOCTYPE html>
        <html>
        <body style="font-family:Georgia,serif; background:#fdf6f0; margin:0; padding:20px;">
          <div style="max-width:520px; margin:0 auto; background:white; border-radius:12px;
                      overflow:hidden; border:1px solid #e8d5c0;">

            <div style="background:#8B1A1A; padding:24px; text-align:center;">
              <h1 style="color:white; margin:0; font-size:22px; letter-spacing:1px;">
                Kokobae Bakery
              </h1>
              <p style="color:#f5c4b3; margin:6px 0 0; font-size:13px;">
                Freshly baked with love
              </p>
            </div>

            <div style="padding:28px 32px;">
              <h2 style="color:#8B1A1A; margin:0 0 6px; font-size:20px;">
                %s
              </h2>
              <p style="color:#666; margin:0 0 24px; font-size:14px;">
                %s
              </p>

              <div style="background:#fdf6f0; border-radius:8px; padding:12px 16px; margin-bottom:20px;">
                <span style="font-size:11px; color:#999; text-transform:uppercase; letter-spacing:0.05em;">
                  Order ID
                </span>
                <p style="margin:4px 0 0; font-size:20px; font-weight:bold; color:#8B1A1A;">
                  #%s
                </p>
              </div>

              <div style="margin-bottom:20px;">
                <p style="font-size:11px; color:#999; text-transform:uppercase; letter-spacing:0.05em; margin:0 0 8px;">
                  Your items
                </p>
                <div style="font-size:14px; color:#333; line-height:2.2;">
                  %s
                </div>
                <div style="border-top:1px solid #eee; margin-top:12px; padding-top:12px;">
                  <strong style="color:#8B1A1A; font-size:16px;">
                    Total: &#8377;%.0f
                  </strong>
                </div>
              </div>

              <div style="background:#fdf6f0; border-radius:8px; padding:12px 16px; margin-bottom:16px; font-size:14px; color:#333;">
                %s
              </div>

              <div style="font-size:13px; color:#666; margin-bottom:20px;">
                Payment: %s
              </div>

              <p style="font-size:13px; color:#555; margin:0; line-height:1.6;">
                %s
              </p>

              <div style="margin-top:24px; padding-top:20px; border-top:1px solid #eee;">
                <p style="font-size:13px; color:#666; margin:0;">
                  Questions?
                  <a href="https://wa.me/917995978220" style="color:#8B1A1A;">
                    WhatsApp us
                  </a>
                  or reply to this email.
                </p>
              </div>
            </div>

            <div style="background:#fdf6f0; padding:16px 32px; text-align:center; border-top:1px solid #e8d5c0;">
              <p style="font-size:12px; color:#999; margin:0;">
                Kokobae Bakery · Sainikpuri, Hyderabad
              </p>
              <p style="font-size:12px; color:#999; margin:4px 0 0;">
                Mon-Sat 9AM-7PM · +91 79959 78220
              </p>
            </div>

          </div>
        </body>
        </html>
        """.formatted(greeting, subtitle, shortId, itemsHtml, total, deliveryInfo, paymentInfo, footerMessage);
    }

    private String buildItemsHtml(Order order) {
        return order.getItems().stream()
                .map(i -> i.getProductName() + " &times; " + i.getQuantity() +
                        " &nbsp;=&nbsp; &#8377;" + (int)(i.getPrice() * i.getQuantity()))
                .collect(Collectors.joining("<br>"));
    }

    private String buildDeliveryInfo(Order order) {
        if ("PICKUP".equals(order.getDeliveryType())) {
            return "<strong>Pickup</strong> from Sainikpuri store";
        }
        return "<strong>Deliver to:</strong> " + order.getDeliveryAddress() + ", " + order.getDeliveryPincode();
    }

    private String buildPaymentInfo(Order order) {
        return switch (order.getPaymentStatus()) {
            case "PAID" -> "Online payment — confirmed";
            case "COD" -> "Cash on delivery";
            default -> "Pending";
        };
    }

    private String lastEight(String id) {
        return id.substring(id.length() - 8);
    }

    private void sendEmail(String to, String subject, String htmlBody) {
        if (to == null || to.isBlank()) {
            System.err.println("No email address for order");
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            helper.setReplyTo("kokobae.bakery@gmail.com");
            mailSender.send(message);
            System.out.println("Email sent to: " + to);
        } catch (Exception e) {
            System.err.println("Email failed to " + to + ": " + e.getMessage());
        }
    }
}
