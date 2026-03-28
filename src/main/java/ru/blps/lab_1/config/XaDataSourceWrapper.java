package ru.blps.lab_1.config;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

public class XaDataSourceWrapper implements DataSource {

    private final XADataSource xaDataSource;
    private final TransactionManager transactionManager;

    public XaDataSourceWrapper(XADataSource xaDataSource, TransactionManager transactionManager) {
        this.xaDataSource = xaDataSource;
        this.transactionManager = transactionManager;
    }

    @Override
    public Connection getConnection() throws SQLException {
        XAConnection xaConnection = xaDataSource.getXAConnection();
        enlistIfActive(xaConnection);
        return xaConnection.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        XAConnection xaConnection = xaDataSource.getXAConnection(username, password);
        enlistIfActive(xaConnection);
        return xaConnection.getConnection();
    }

    private void enlistIfActive(XAConnection xaConnection) throws SQLException {
        try {
            Transaction tx = transactionManager.getTransaction();
            if (tx != null && tx.getStatus() == Status.STATUS_ACTIVE) {
                tx.enlistResource(xaConnection.getXAResource());
                tx.registerSynchronization(new Synchronization() {
                    @Override
                    public void beforeCompletion() {}

                    @Override
                    public void afterCompletion(int status) {
                        try {
                            xaConnection.close();
                        } catch (SQLException ignored) {}
                    }
                });
            }
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            try {
                xaConnection.close();
            } catch (SQLException ignored) {}
            throw new SQLException("Failed to enlist XA resource in JTA transaction", e);
        }
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
