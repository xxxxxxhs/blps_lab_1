package ru.blps.lab_1.entity;

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
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_courier_decisions")
public class OrderCourierDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "courier_id", nullable = false)
    private Courier courier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CourierDecision decision;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public OrderCourierDecision() {
    }

    public OrderCourierDecision(Order order, Courier courier, CourierDecision decision, LocalDateTime createdAt) {
        this.order = order;
        this.courier = courier;
        this.decision = decision;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public Courier getCourier() {
        return courier;
    }

    public CourierDecision getDecision() {
        return decision;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public void setCourier(Courier courier) {
        this.courier = courier;
    }

    public void setDecision(CourierDecision decision) {
        this.decision = decision;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
