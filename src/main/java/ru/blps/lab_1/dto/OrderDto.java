package ru.blps.lab_1.dto;

import java.util.List;

public class OrderDto {

    private final Long id;
    private final String status;
    private final String restaurantAddress;
    private final String city;
    private final String deliveryAddress;
    private final String phone;
    private final String comment;
    private final List<OrderItemDto> items;

    public OrderDto(
        Long id,
        String status,
        String restaurantAddress,
        String city,
        String deliveryAddress,
        String phone,
        String comment,
        List<OrderItemDto> items
    ) {
        this.id = id;
        this.status = status;
        this.restaurantAddress = restaurantAddress;
        this.city = city;
        this.deliveryAddress = deliveryAddress;
        this.phone = phone;
        this.comment = comment;
        this.items = items;
    }

    public Long getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public String getRestaurantAddress() {
        return restaurantAddress;
    }

    public String getCity() {
        return city;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public String getPhone() {
        return phone;
    }

    public String getComment() {
        return comment;
    }

    public List<OrderItemDto> getItems() {
        return items;
    }
}

