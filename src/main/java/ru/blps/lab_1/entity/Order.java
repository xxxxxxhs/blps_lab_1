package ru.blps.lab_1.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long clientId;

    @Column(nullable = false)
    private Long courierId;

    @Column(nullable = false)
    private Long restaurantId;

    @Column(nullable = false)
    private String restaurantAddress;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String deliveryAddress;

    @Column(nullable = false, length = 11)
    private String phone;

    @Column(length = 512)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public Order() {
    }

    public Order(
        Long clientId,
        Long courierId,
        Long restaurantId,
        String restaurantAddress,
        String city,
        String deliveryAddress,
        String phone,
        String comment,
        OrderStatus status
    ) {
        this.clientId = clientId;
        this.courierId = courierId;
        this.restaurantId = restaurantId;
        this.restaurantAddress = restaurantAddress;
        this.city = city;
        this.deliveryAddress = deliveryAddress;
        this.phone = phone;
        this.comment = comment;
        this.status = status;
    }   

    public Long getId() {
        return id;
    }

    public Long getClientId() {
        return clientId;
    }

    public Long getCourierId() {
        return courierId;
    }

    public Long getRestaurantId() {
        return restaurantId;
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

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }       

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public void setCourierId(Long courierId) {
        this.courierId = courierId;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }

    public void setRestaurantAddress(String restaurantAddress) {
        this.restaurantAddress = restaurantAddress;
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

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Заказ #").append(id).append("\n");
        sb.append("Статус: ").append(status).append("\n");
        sb.append("Клиент: ").append(clientId).append("  Курьер: ").append(courierId).append("  Ресторан: ").append(restaurantId).append("\n");
        sb.append("Адрес доставки: ").append(city).append(", ").append(deliveryAddress).append("\n");
        sb.append("Забрать в ресторане: ").append(restaurantAddress).append("\n");
        sb.append("Телефон: ").append(phone);
        if (comment != null && !comment.isBlank()) {
            sb.append("\nКомментарий: ").append(comment);
        }
        sb.append("\n\nСостав заказа:\n");
        for (OrderItem item : items) {
            sb.append("  ").append(item.toString()).append("\n");
        }
        return sb.toString().trim();
    }
}