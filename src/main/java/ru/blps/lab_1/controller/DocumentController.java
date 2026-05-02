package ru.blps.lab_1.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final Path storageDir;
    private final String internalBaseUrl;

    public DocumentController(
        @Value("${eis.onlyoffice.storage-dir:/app/reports}") String storageDir,
        @Value("${eis.onlyoffice.public-base-url:http://nginx}") String internalBaseUrl
    ) {
        this.storageDir = Paths.get(storageDir);
        this.internalBaseUrl = internalBaseUrl;
    }

    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> get(@PathVariable String fileName) {
        if (fileName.contains("/") || fileName.contains("..")) {
            return ResponseEntity.badRequest().build();
        }
        Path file = storageDir.resolve(fileName);
        if (!Files.exists(file)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
            .body(new FileSystemResource(file));
    }

    @GetMapping("/{fileName}/view")
    public ResponseEntity<String> view(@PathVariable String fileName) {
        if (fileName.contains("/") || fileName.contains("..")) {
            return ResponseEntity.badRequest().build();
        }
        Path file = storageDir.resolve(fileName);
        if (!Files.exists(file)) return ResponseEntity.notFound().build();

        String docUrl = internalBaseUrl + "/api/documents/" + fileName;
        String key = fileName.replaceAll("[^a-zA-Z0-9_\\-]", "_");

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
}
