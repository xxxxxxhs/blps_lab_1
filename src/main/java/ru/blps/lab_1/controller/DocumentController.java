package ru.blps.lab_1.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final String webdavBaseUrl;
    private final String basicAuth;
    private final String internalBaseUrl;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public DocumentController(
        @Value("${eis.nextcloud.webdav-url:http://nginx/cloud/remote.php/dav/files/admin}") String webdavBaseUrl,
        @Value("${eis.nextcloud.user:admin}") String user,
        @Value("${eis.nextcloud.password:admin}") String password,
        @Value("${eis.onlyoffice.public-base-url:http://nginx}") String internalBaseUrl
    ) {
        this.webdavBaseUrl = webdavBaseUrl;
        this.basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
        this.internalBaseUrl = internalBaseUrl;
    }

    @GetMapping("/{subDir}/{fileName}")
    public ResponseEntity<Resource> get(@PathVariable String subDir, @PathVariable String fileName) {
        if (hasTraversal(subDir) || hasTraversal(fileName)) {
            return ResponseEntity.badRequest().build();
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webdavBaseUrl + "/reports/" + subDir + "/" + fileName))
                    .header("Authorization", basicAuth)
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 404) return ResponseEntity.notFound().build();
            if (response.statusCode() >= 300) return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(new ByteArrayResource(response.body()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @GetMapping("/{subDir}/{fileName}/view")
    public ResponseEntity<String> view(@PathVariable String subDir, @PathVariable String fileName) {
        if (hasTraversal(subDir) || hasTraversal(fileName)) {
            return ResponseEntity.badRequest().build();
        }
        try {
            HttpRequest head = HttpRequest.newBuilder()
                    .uri(URI.create(webdavBaseUrl + "/reports/" + subDir + "/" + fileName))
                    .header("Authorization", basicAuth)
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();
            if (httpClient.send(head, HttpResponse.BodyHandlers.discarding()).statusCode() == 404) {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }

        String docUrl = internalBaseUrl + "/api/documents/" + subDir + "/" + fileName;
        String key = (subDir + "_" + fileName).replaceAll("[^a-zA-Z0-9_\\-]", "_");

        String html = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <title>%s</title>
              <style>html,body{margin:0;padding:0;height:100%%}#editor{height:100vh}</style>
            </head>
            <body>
              <div id="editor"></div>
              <script src="/office/web-apps/apps/api/documents/api.js"></script>
              <script>
                new DocsAPI.DocEditor("editor", {
                  "document": {
                    "fileType": "csv",
                    "key": "%s",
                    "title": "%s",
                    "url": "%s"
                  },
                  "editorConfig": { "mode": "view", "lang": "ru" },
                  "type": "desktop",
                  "width": "100%%",
                  "height": "100%%"
                });
              </script>
            </body>
            </html>
            """.formatted(fileName, key, fileName, docUrl);

        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(html);
    }

    private boolean hasTraversal(String segment) {
        return segment.contains("/") || segment.contains("..");
    }
}
