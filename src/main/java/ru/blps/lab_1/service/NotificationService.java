package ru.blps.lab_1.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class NotificationService {

    private static final String TELEGRAM_SEND_URL = "https://api.telegram.org/bot%s/sendMessage";

    @Value("${telegram.bot-token:}")
    private String botToken;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void send(Recipient target, String chatId, String messageText) {
        String fullText = target.getLabel() + "\n" + messageText;
        String json = "{\"chat_id\":\"" + escapeJson(chatId) + "\",\"text\":\"" + escapeJson(fullText) + "\"}";
        String url = String.format(TELEGRAM_SEND_URL, botToken);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
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
