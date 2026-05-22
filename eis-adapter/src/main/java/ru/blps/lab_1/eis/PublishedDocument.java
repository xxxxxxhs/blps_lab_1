package ru.blps.lab_1.eis;

public class PublishedDocument {

    private final String fileName;
    private final String storageUrl;
    private final long sizeBytes;

    public PublishedDocument(String fileName, String storageUrl, long sizeBytes) {
        this.fileName = fileName;
        this.storageUrl = storageUrl;
        this.sizeBytes = sizeBytes;
    }

    public String getFileName() { return fileName; }
    public String getStorageUrl() { return storageUrl; }
    public long getSizeBytes() { return sizeBytes; }
}
