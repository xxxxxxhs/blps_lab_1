package ru.blps.lab_1.config;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

public class XaDataSourceWrapper implements DataSource {

    private final XADataSource xaDataSource;
    private final TransactionManager transactionManager;
    private final ThreadLocal<Connection> txConnection = new ThreadLocal<>();

    public XaDataSourceWrapper(XADataSource xaDataSource, TransactionManager transactionManager) {
        this.xaDataSource = xaDataSource;
        this.transactionManager = transactionManager;
    }

    @Override
    public Connection getConnection() throws SQLException {
        try {
            Transaction tx = transactionManager.getTransaction();
            if (tx != null && tx.getStatus() == Status.STATUS_ACTIVE) {
                Connection cached = txConnection.get();
                if (cached != null) {
                    return cached;
                }
                XAConnection xaConnection = xaDataSource.getXAConnection();
                tx.enlistResource(xaConnection.getXAResource());
                Connection real = xaConnection.getConnection();
                Connection proxy = (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (p, method, args) -> {
                        if ("close".equals(method.getName())) {
                            return null; // no-op: connection is closed after tx completion
                        }
                        try {
                            return method.invoke(real, args);
                        } catch (InvocationTargetException e) {
                            throw e.getCause();
                        }
                    }
                );
                txConnection.set(proxy);
                tx.registerSynchronization(new Synchronization() {
                    @Override
                    public void beforeCompletion() {}

                    @Override
                    public void afterCompletion(int status) {
                        txConnection.remove();
                        try { xaConnection.close(); } catch (SQLException ignored) {}
                    }
                });
                return proxy;
            }
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException("Failed to enlist XA resource in JTA transaction", e);
        }
        // No active JTA transaction — return a plain connection
        return xaDataSource.getXAConnection().getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return getConnection();
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException { return null; }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {}

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {}

    @Override
    public int getLoginTimeout() throws SQLException { return 0; }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw new SQLException("Not a wrapper for " + iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }
}
