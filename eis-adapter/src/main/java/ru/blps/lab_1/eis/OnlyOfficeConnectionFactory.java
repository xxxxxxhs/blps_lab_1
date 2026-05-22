package ru.blps.lab_1.eis;

import jakarta.resource.ResourceException;
import jakarta.resource.cci.ConnectionFactory;

public interface OnlyOfficeConnectionFactory extends ConnectionFactory {

    OnlyOfficeConnection getConnection(OnlyOfficeConnectionSpec spec) throws ResourceException;
}
