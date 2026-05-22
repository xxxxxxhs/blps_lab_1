package ru.blps.lab_1.eis;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;

import javax.security.auth.Subject;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

public class OnlyOfficeManagedConnectionFactory implements ManagedConnectionFactory, Serializable {

    private String webdavBaseUrl = "http://nginx/cloud/remote.php/dav/files/admin";
    private String user = "admin";
    private String password = "admin";

    private transient PrintWriter logWriter;

    public OnlyOfficeManagedConnectionFactory() {}

    public String getWebdavBaseUrl() { return webdavBaseUrl; }
    public void setWebdavBaseUrl(String webdavBaseUrl) { this.webdavBaseUrl = webdavBaseUrl; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

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
        return new OnlyOfficeManagedConnection(webdavBaseUrl, user, password);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OnlyOfficeManagedConnectionFactory that)) return false;
        return Objects.equals(webdavBaseUrl, that.webdavBaseUrl)
            && Objects.equals(user, that.user)
            && Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(webdavBaseUrl, user, password);
    }
}