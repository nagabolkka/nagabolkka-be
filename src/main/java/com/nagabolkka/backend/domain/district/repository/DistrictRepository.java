package com.nagabolkka.backend.domain.district.repository;

import com.nagabolkka.backend.domain.district.entity.District;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DistrictRepository extends JpaRepository<District, Long> {
}