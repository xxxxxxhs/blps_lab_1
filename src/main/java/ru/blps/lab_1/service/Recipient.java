package ru.blps.lab_1.service;

public enum Recipient {
    CLIENT("Клиент"),
    COURIER("Курьер"),
    RESTAURANT("Ресторан");

    private final String label;

    Recipient(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
