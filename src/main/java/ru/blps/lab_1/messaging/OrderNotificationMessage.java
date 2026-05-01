package ru.blps.lab_1.messaging;

import ru.blps.lab_1.service.Recipient;

public class OrderNotificationMessage {

    private Recipient recipient;
    private Long recipientId;
    private String chatId;
    private String text;

    public OrderNotificationMessage() {}

    public OrderNotificationMessage(Recipient recipient, Long recipientId, String chatId, String text) {
        this.recipient = recipient;
        this.recipientId = recipientId;
        this.chatId = chatId;
        this.text = text;
    }

    public Recipient getRecipient() { return recipient; }
    public void setRecipient(Recipient recipient) { this.recipient = recipient; }

    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
