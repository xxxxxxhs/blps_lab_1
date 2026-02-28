package ru.blps.lab_1.dto;

public class OrderItemDto {

    private final String name;
    private final int quantity;

    public OrderItemDto(String name, int quantity) {
        this.name = name;
        this.quantity = quantity;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }
}

