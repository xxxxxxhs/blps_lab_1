package ru.blps.lab_1.eis;

import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;

import javax.security.auth.Subject;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Set;

public class OnlyOfficeManagedConnectionFactory implements ManagedConnectionFactory {

    private final Path storageDir;
    private final String publicBaseUrl;
    private PrintWriter logWriter;

    public OnlyOfficeManagedConnectionFactory(Path storageDir, String publicBaseUrl) {
        this.storageDir = storageDir;
        this.publicBaseUrl = publicBaseUrl;
    }

    @Override
    public Object createConnectionFactory(ConnectionManager cxManager) {
        return new OnlyOfficeConnectionFactoryImpl(this, cxManager);
    }

    @Override
    public Object createConnectionFactory() {
        return new OnlyOfficeConnectionFactoryImpl(this, new DefaultConnectionManager());
    }

    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) {
        return new OnlyOfficeManagedConnection(storageDir, publicBaseUrl);
    }

    @Override
    public ManagedConnection matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        for (Object o : connectionSet) {
            if (o instanceof OnlyOfficeManagedConnection mc) return mc;
        }
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) { this.logWriter = out; }

    @Override
    public PrintWriter getLogWriter() { return logWriter; }
}
