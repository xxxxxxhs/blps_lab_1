package ru.blps.lab_1.bpm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.blps.lab_1.service.NotificationService;
import ru.blps.lab_1.service.Recipient;

@Component
public class OrderNotifier {

    private final NotificationService notificationService;

    @Value("${telegram.chatId:${telegram.chat-id:}}")
    private String telegramChatId;

    public OrderNotifier(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void send(Recipient target, Long recipientId, String messageText) {
        if (telegramChatId == null || telegramChatId.isBlank()) {
            return;
        }
        notificationService.send(target, recipientId, telegramChatId, messageText);
    }
}
