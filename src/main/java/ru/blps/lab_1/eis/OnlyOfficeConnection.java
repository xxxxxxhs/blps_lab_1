package ru.blps.lab_1.eis;

import jakarta.resource.ResourceException;
import jakarta.resource.cci.Connection;

public interface OnlyOfficeConnection extends Connection {

    PublishedDocument publishDocument(String fileName, byte[] body) throws ResourceException;
}
