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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class OnlyOfficeManagedConnection implements ManagedConnection {

    private static final Logger log = LoggerFactory.getLogger(OnlyOfficeManagedConnection.class);

    private final Path storageDir;
    private final String publicBaseUrl;
    private final List<ConnectionEventListener> listeners = new ArrayList<>();
    private final List<OnlyOfficeConnectionImpl> handles = new ArrayList<>();
    private PrintWriter logWriter;

    public OnlyOfficeManagedConnection(Path storageDir, String publicBaseUrl) {
        this.storageDir = storageDir;
        this.publicBaseUrl = publicBaseUrl;
    }

    PublishedDocument publishDocument(String fileName, byte[] body) throws ResourceException {
        try {
            Files.createDirectories(storageDir);
            Path target = storageDir.resolve(fileName);
            Files.write(target, body, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            String url = publicBaseUrl + "/api/documents/" + fileName;
            log.info("EIS: published document {} ({} bytes) -> {}", fileName, body.length, url);
            return new PublishedDocument(fileName, url, body.length);
        } catch (IOException e) {
            throw new ResourceException("Failed to publish document: " + e.getMessage(), e);
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
