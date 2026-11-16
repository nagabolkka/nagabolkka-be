package com.nagabolkka.backend.domain.place.repository;

import com.nagabolkka.backend.domain.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    // 구의 활성 장소 (기간은 서비스에서 Place.isOpenOn 으로 → 단위 테스트 가능)
    @Query("select p from Place p where p.district.id = :districtId and p.active = true")
    List<Place> findActiveByDistrictId(@Param("districtId") Long districtId);

    // 시드 : 같은 이름 장소가 있으면 건너뜀
    boolean existsByName(String name);
}