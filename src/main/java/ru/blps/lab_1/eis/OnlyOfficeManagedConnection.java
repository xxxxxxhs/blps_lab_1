package ru.blps.lab_1.eis;

import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionEvent;
import jakarta.resource.spi.ConnectionEventListener;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.LocalTransaction;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class OnlyOfficeManagedConnection implements ManagedConnection {

    private static final Logger log = LoggerFactory.getLogger(OnlyOfficeManagedConnection.class);

    private final String webdavBaseUrl;
    private final String basicAuth;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final List<ConnectionEventListener> listeners = new ArrayList<>();
    private final List<OnlyOfficeConnectionImpl> handles = new ArrayList<>();
    private PrintWriter logWriter;

    public OnlyOfficeManagedConnection(String webdavBaseUrl, String user, String password) {
        this.webdavBaseUrl = webdavBaseUrl;
        this.basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    PublishedDocument publishDocument(String subDir, String fileName, byte[] body) throws ResourceException {
        try {
            ensureDirectory("reports");
            ensureDirectory("reports/" + subDir);
            String url = webdavBaseUrl + "/reports/" + subDir + "/" + fileName;
            HttpRequest put = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", basicAuth)
                    .header("Content-Type", "text/csv; charset=UTF-8")
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            HttpResponse<String> response = httpClient.send(put, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new ResourceException("Nextcloud WebDAV PUT failed: HTTP " + response.statusCode()
                        + " for " + url + " — " + response.body());
            }
            log.info("EIS: published document {} ({} bytes) -> {}", fileName, body.length, url);
            return new PublishedDocument(fileName, url, body.length);
        } catch (IOException | InterruptedException e) {
            throw new ResourceException("Failed to publish document to Nextcloud: " + e.getMessage(), e);
        }
    }

    private void ensureDirectory(String path) throws IOException, InterruptedException, ResourceException {
        HttpRequest mkcol = HttpRequest.newBuilder()
                .uri(URI.create(webdavBaseUrl + "/" + path))
                .header("Authorization", basicAuth)
                .method("MKCOL", HttpRequest.BodyPublishers.noBody())
                .build();
        int status = httpClient.send(mkcol, HttpResponse.BodyHandlers.discarding()).statusCode();
        if (status != 201 && status != 405 && status != 423) {
            throw new ResourceException("MKCOL failed for " + path + ": HTTP " + status);
        }
    }

    void closeHandle(OnlyOfficeConnectionImpl handle) {
        handles.remove(handle);
        ConnectionEvent ev = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
        ev.setConnectionHandle(handle);
        for (ConnectionEventListener l : listeners) l.connectionClosed(ev);
    }

    @Override
    public Object getConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) {
        OnlyOfficeConnectionImpl handle = new OnlyOfficeConnectionImpl(this);
        handles.add(handle);
        return handle;
    }

    @Override
    public void destroy() {
        handles.clear();
        listeners.clear();
    }

    @Override
    public void cleanup() {
        handles.clear();
    }

    @Override
    public void associateConnection(Object connection) {
        if (connection instanceof OnlyOfficeConnectionImpl impl) {
            handles.add(impl);
            impl.associate(this);
        }
    }

    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public XAResource getXAResource() throws ResourceException {
        throw new NotSupportedException("XA not supported by OnlyOffice EIS adapter");
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new NotSupportedException("Local transactions not supported");
    }

    @Override
    public ManagedConnectionMetaData getMetaData() {
        return new ManagedConnectionMetaData() {
            public String getEISProductName() { return "OnlyOffice Document Server"; }
            public String getEISProductVersion() { return "9.x"; }
            public int getMaxConnections() { return 0; }
            public String getUserName() { return "anonymous"; }
        };
    }

    @Override
    public void setLogWriter(PrintWriter out) { this.logWriter = out; }

    @Override
    public PrintWriter getLogWriter() { return logWriter; }
}
