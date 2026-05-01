package ru.blps.lab_1.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class StompSenderService {

    private static final Logger log = LoggerFactory.getLogger(StompSenderService.class);

    private final String host;
    private final int stompPort;
    private final String queue;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StompSenderService(
        @Value("${activemq.host:activemq}") String host,
        @Value("${activemq.stomp-port:61613}") int stompPort,
        @Value("${activemq.queue:order.notifications}") String queue
    ) {
        this.host = host;
        this.stompPort = stompPort;
        this.queue = queue;
    }

    public void send(OrderNotificationMessage message) {
        try (Socket socket = new Socket(host, stompPort)) {
            socket.setSoTimeout(5_000);
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            writeFrame(out, "CONNECT", Map.of(
                "accept-version", "1.2",
                "host", host
            ), null);
            readFrame(in);

            String body = objectMapper.writeValueAsString(message);
            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("destination", "/queue/" + queue);
            headers.put("content-type", "text/plain");
            headers.put("content-length", String.valueOf(bodyBytes.length));
            writeFrame(out, "SEND", headers, body);

            writeFrame(out, "DISCONNECT", Map.of(), null);
        } catch (Exception e) {
            log.error("STOMP send failed: {}", e.getMessage());
        }
    }

    private void writeFrame(OutputStream out, String command, Map<String, String> headers, String body)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(command).append('\n');
        headers.forEach((k, v) -> sb.append(k).append(':').append(v).append('\n'));
        sb.append('\n');
        if (body != null) sb.append(body);
        sb.append('\0');
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private void readFrame(InputStream in) throws IOException {
        int b;
        while ((b = in.read()) != -1 && (b == '\n' || b == '\r')) {}
        while (b != 0 && b != -1) {
            b = in.read();
        }
    }
}
