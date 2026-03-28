package ru.blps.lab_1.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public class CreateOrderRequest {

    @NotNull
    private Long restaurantId;

    @NotBlank
    private String city;

    @NotBlank
    private String deliveryAddress;

    @NotBlank
    @Pattern(regexp = "\\d{11}")
    private String phone;

    private String comment;

    @Valid
    @NotEmpty
    private List<CreateOrderItemRequest> items;

    public Long getRestaurantId() {
        return restaurantId;
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

    public List<CreateOrderItemRequest> getItems() {
        return items;
    }

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setItems(List<CreateOrderItemRequest> items) {
        this.items = items;
    }
}
