package ru.blps.lab_1.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final String TELEGRAM_SEND_URL = "https://api.telegram.org/bot%s/sendMessage";

    @Value("${telegram.botToken:${telegram.bot-token:}}")
    private String botToken;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void send(Recipient target, Long recipientId, String chatId, String messageText) {
        if (botToken == null || botToken.isBlank() || chatId == null || chatId.isBlank()) {
            return;
        }
        String header = recipientId == null ? target.getLabel() : target.getLabel() + " id=" + recipientId;
        String fullText = header + "\n" + messageText;
        String json = "{\"chat_id\":\"" + escapeJson(chatId) + "\",\"text\":\"" + escapeJson(fullText) + "\"}";
        String url = String.format(TELEGRAM_SEND_URL, botToken);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
        try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            log.warn("Telegram send failed: {}", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Telegram send interrupted");
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
