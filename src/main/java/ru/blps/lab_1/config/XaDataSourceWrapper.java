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
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

public class XaDataSourceWrapper implements DataSource {

    private static final Method CONNECTION_COMMIT;
    private static final Method CONNECTION_ROLLBACK;
    private static final Method CONNECTION_CLOSE;
    private static final Method CONNECTION_ABORT;

    static {
        try {
            CONNECTION_COMMIT = Connection.class.getMethod("commit");
            CONNECTION_ROLLBACK = Connection.class.getMethod("rollback");
            CONNECTION_CLOSE = Connection.class.getMethod("close");
            Method abort = null;
            try {
                abort = Connection.class.getMethod("abort", java.util.concurrent.Executor.class);
            } catch (NoSuchMethodException ignored) {
            }
            CONNECTION_ABORT = abort;
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

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
                        if (method.equals(CONNECTION_COMMIT)
                            || method.equals(CONNECTION_ROLLBACK)
                            || method.equals(CONNECTION_CLOSE)
                            || (CONNECTION_ABORT != null && method.equals(CONNECTION_ABORT))) {
                            return null;
                        }
                        try {
                            return method.invoke(real, args);
                        } catch (InvocationTargetException e) {
                            Throwable c = e.getCause();
                            if (c == null) {
                                throw e;
                            }
                            if (c instanceof SQLException sqlEx) {
                                String m = sqlEx.getMessage();
                                if (m != null && m.contains("Connection has been closed automatically")) {
                                    if (method.equals(CONNECTION_COMMIT) || method.equals(CONNECTION_ROLLBACK)) {
                                        return null;
                                    }
                                }
                            }
                            throw c;
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
