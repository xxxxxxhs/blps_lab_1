package ru.blps.lab_1.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private AppUser client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id")
    private Courier courier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

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
        AppUser client,
        Courier courier,
        Restaurant restaurant,
        String restaurantAddress,
        String city,
        String deliveryAddress,
        String phone,
        String comment,
        OrderStatus status
    ) {
        this.client = client;
        this.courier = courier;
        this.restaurant = restaurant;
        this.restaurantAddress = restaurantAddress;
        this.city = city;
        this.deliveryAddress = deliveryAddress;
        this.phone = phone;
        this.comment = comment;
        this.status = status;
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
        AppUser clientRef = new AppUser();
        clientRef.setId(clientId);
        Courier courierRef = null;
        if (courierId != null) {
            courierRef = new Courier();
            courierRef.setId(courierId);
        }
        Restaurant restaurantRef = new Restaurant();
        restaurantRef.setId(restaurantId);
        this.client = clientRef;
        this.courier = courierRef;
        this.restaurant = restaurantRef;
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

    public AppUser getClient() {
        return client;
    }

    public Long getClientId() {
        return client != null ? client.getId() : null;
    }

    public Courier getCourier() {
        return courier;
    }

    public Long getCourierId() {
        return courier != null ? courier.getId() : null;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public Long getRestaurantId() {
        return restaurant != null ? restaurant.getId() : null;
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

    public void setClient(AppUser client) {
        this.client = client;
    }

    public void setClientId(Long clientId) {
        if (clientId == null) {
            this.client = null;
            return;
        }
        if (this.client == null) {
            this.client = new AppUser();
        }
        this.client.setId(clientId);
    }

    public void setCourier(Courier courier) {
        this.courier = courier;
    }

    public void setCourierId(Long courierId) {
        if (courierId == null) {
            this.courier = null;
            return;
        }
        if (this.courier == null) {
            this.courier = new Courier();
        }
        this.courier.setId(courierId);
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setRestaurant(Restaurant restaurant) {
        this.restaurant = restaurant;
    }

    public void setRestaurantId(Long restaurantId) {
        if (restaurantId == null) {
            this.restaurant = null;
            return;
        }
        if (this.restaurant == null) {
            this.restaurant = new Restaurant();
        }
        this.restaurant.setId(restaurantId);
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
        Long clientId = client != null ? client.getId() : null;
        Long courierId = courier != null ? courier.getId() : null;
        Long restaurantId = restaurant != null ? restaurant.getId() : null;
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