package ru.blps.lab_1.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.BytesMessage;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import ru.blps.lab_1.service.NotificationService;

import java.nio.charset.StandardCharsets;

@Component
public class OrderNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationListener.class);

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @JmsListener(destination = "${activemq.queue:order.notifications}")
    public void onMessage(Message jmsMessage) {
        try {
            String json;
            if (jmsMessage instanceof TextMessage tm) {
                json = tm.getText();
            } else if (jmsMessage instanceof BytesMessage bm) {
                byte[] bytes = new byte[(int) bm.getBodyLength()];
                bm.readBytes(bytes);
                json = new String(bytes, StandardCharsets.UTF_8);
            } else {
                log.error("Unexpected JMS message type: {}", jmsMessage.getClass().getSimpleName());
                return;
            }
            OrderNotificationMessage msg = objectMapper.readValue(json, OrderNotificationMessage.class);
            notificationService.send(msg.getRecipient(), msg.getRecipientId(), msg.getChatId(), msg.getText());
        } catch (Exception e) {
            log.error("Failed to process notification: {}", e.getMessage());
        }
    }
}
