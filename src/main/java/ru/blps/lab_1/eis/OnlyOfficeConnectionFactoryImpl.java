package ru.blps.lab_1.eis;

import jakarta.resource.ResourceException;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.ConnectionSpec;
import jakarta.resource.cci.RecordFactory;
import jakarta.resource.cci.ResourceAdapterMetaData;
import jakarta.resource.spi.ConnectionManager;

import javax.naming.NamingException;
import javax.naming.Reference;

public class OnlyOfficeConnectionFactoryImpl implements OnlyOfficeConnectionFactory {

    private final OnlyOfficeManagedConnectionFactory mcf;
    private final ConnectionManager cm;
    private Reference reference;

    public OnlyOfficeConnectionFactoryImpl(OnlyOfficeManagedConnectionFactory mcf, ConnectionManager cm) {
        this.mcf = mcf;
        this.cm = cm;
    }

    @Override
    public OnlyOfficeConnection getConnection(OnlyOfficeConnectionSpec spec) throws ResourceException {
        Object handle = cm.allocateConnection(mcf, null);
        return (OnlyOfficeConnection) handle;
    }

    @Override
    public Connection getConnection() throws ResourceException {
        return getConnection(new OnlyOfficeConnectionSpec("generic"));
    }

    @Override
    public Connection getConnection(ConnectionSpec properties) throws ResourceException {
        if (properties instanceof OnlyOfficeConnectionSpec spec) return getConnection(spec);
        return getConnection();
    }

    @Override
    public RecordFactory getRecordFactory() throws ResourceException {
        throw new jakarta.resource.NotSupportedException("RecordFactory not supported");
    }

    @Override
    public ResourceAdapterMetaData getMetaData() {
        return new ResourceAdapterMetaData() {
            public String getAdapterVersion() { return "1.0"; }
            public String getAdapterVendorName() { return "blps-lab1"; }
            public String getAdapterName() { return "OnlyOffice JCA Adapter"; }
            public String getAdapterShortDescription() { return "Publishes reports to OnlyOffice"; }
            public String getSpecVersion() { return "2.1"; }
            public String[] getInteractionSpecsSupported() { return new String[0]; }
            public boolean supportsExecuteWithInputAndOutputRecord() { return false; }
            public boolean supportsExecuteWithInputRecordOnly() { return false; }
            public boolean supportsLocalTransactionDemarcation() { return false; }
        };
    }

    @Override
    public void setReference(Reference reference) { this.reference = reference; }

    @Override
    public Reference getReference() throws NamingException { return reference; }
}
