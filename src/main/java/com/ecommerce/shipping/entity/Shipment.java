package com.ecommerce.shipping.entity;

import com.ecommerce.order.entity.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "shipments", uniqueConstraints = {
    @UniqueConstraint(name = "uq_shipments_order", columnNames = {"order_id"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "carrier", length = 100)
    private String carrier;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "last_sync_time")
    private LocalDateTime lastSyncTime;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Shipment(Order order, String trackingNumber, String carrier, String status, LocalDateTime lastSyncTime) {
        this.order = order;
        this.trackingNumber = trackingNumber;
        this.carrier = carrier;
        this.status = status;
        this.lastSyncTime = lastSyncTime;
    }
}
