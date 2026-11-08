package com.nagabolkka.backend.domain.district.entity;

import com.nagabolkka.backend.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "districts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class District extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal centerLat;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal centerLng;

    @Column(nullable = false)
    private int nx;

    @Column(nullable = false)
    private int ny;

    public static District of(String name, BigDecimal centerLat, BigDecimal centerLng, int nx, int ny) {
        District district = new District();
        district.name = name;
        district.centerLat = centerLat;
        district.centerLng = centerLng;
        district.nx = nx;
        district.ny = ny;
        return district;
    }
}