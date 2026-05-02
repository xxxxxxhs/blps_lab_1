package ru.blps.lab_1.eis;

import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.cci.ConnectionMetaData;
import jakarta.resource.cci.Interaction;
import jakarta.resource.cci.LocalTransaction;
import jakarta.resource.cci.ResultSetInfo;

public class OnlyOfficeConnectionImpl implements OnlyOfficeConnection {

    private OnlyOfficeManagedConnection mc;

    public OnlyOfficeConnectionImpl(OnlyOfficeManagedConnection mc) {
        this.mc = mc;
    }

    void associate(OnlyOfficeManagedConnection mc) {
        this.mc = mc;
    }

    @Override
    public PublishedDocument publishDocument(String fileName, byte[] body) throws ResourceException {
        if (mc == null) throw new ResourceException("Connection is closed");
        return mc.publishDocument(fileName, body);
    }

    @Override
    public void close() {
        if (mc != null) {
            OnlyOfficeManagedConnection released = mc;
            mc = null;
            released.closeHandle(this);
        }
    }

    @Override
    public Interaction createInteraction() throws ResourceException {
        throw new NotSupportedException("CCI Interaction not used; call publishDocument() directly");
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new NotSupportedException("Local transactions not supported");
    }

    @Override
    public ConnectionMetaData getMetaData() {
        return new ConnectionMetaData() {
            public String getEISProductName() { return "OnlyOffice Document Server"; }
            public String getEISProductVersion() { return "9.x"; }
            public String getUserName() { return "anonymous"; }
        };
    }

    @Override
    public ResultSetInfo getResultSetInfo() throws ResourceException {
        throw new NotSupportedException("ResultSetInfo not supported");
    }
}
