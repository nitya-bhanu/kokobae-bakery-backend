package com.kokobae_bakery.kokobae_bakery.service;

import com.kokobae_bakery.kokobae_bakery.model.Order;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.whatsapp.from}")
    private String fromNumber;

    @Value("${owner.whatsapp.number}")
    private String ownerNumber;

    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
    }

    public void notifyNewOrder(Order order) {
        String shortId = order.getId()
                .substring(order.getId().length() - 8);

        String deliveryInfo = "PICKUP".equals(order.getDeliveryType())
                ? "Pickup from store"
                : "Deliver to: " + order.getDeliveryAddress()
                + " (" + order.getDeliveryPincode() + ")";

        String itemsList = order.getItems().stream()
                .map(i -> i.getProductName() + " x" + i.getQuantity())
                .collect(Collectors.joining(", "));

        // Message to customer
        String customerMsg = String.format(
                "Hi %s! Your Kokobae Bakery order #%s is confirmed!\n\n" +
                        "Items: %s\n" +
                        "Total: Rs %.0f\n" +
                        "%s\n\n" +
                        "We'll get baking right away! " +
                        "Questions? Reply to this message or call us.",
                order.getCustomerName(),
                shortId,
                itemsList,
                order.getTotalAmount(),
                deliveryInfo
        );

        // Message to owner
        String ownerMsg = String.format(
                "New order! #%s\n" +
                        "Customer: %s | %s\n" +
                        "Items: %s\n" +
                        "Total: Rs %.0f\n" +
                        "Payment: %s\n" +
                        "%s",
                shortId,
                order.getCustomerName(),
                order.getCustomerPhone(),
                itemsList,
                order.getTotalAmount(),
                order.getPaymentStatus(),
                deliveryInfo
        );

        sendWhatsApp("whatsapp:+91" + order.getCustomerPhone(), customerMsg);
        sendWhatsApp(ownerNumber, ownerMsg);
    }

    private void sendWhatsApp(String to, String body) {
        try {
            Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(fromNumber),
                    body
            ).create();
        } catch (Exception e) {
            System.err.println("WhatsApp failed to " + to + ": " + e.getMessage());
        }
    }
}
