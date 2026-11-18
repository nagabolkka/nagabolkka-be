package com.nagabolkka.backend.domain.place.repository;

import com.nagabolkka.backend.domain.place.entity.Place;
import com.nagabolkka.backend.global.geo.GeoBox;
import com.nagabolkka.backend.global.geo.GeoPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    // 네모 안 활성 장소 (기간은 서비스에서 Place.isOpenOn 으로 → 단위 테스트 가능)
    @Query("""
            select p from Place p
            where p.active = true
              and p.lat between :minLat and :maxLat
              and p.lng between :minLng and :maxLng
            """)
    List<Place> findActiveInBox(@Param("minLat") BigDecimal minLat, @Param("maxLat") BigDecimal maxLat,
                                @Param("minLng") BigDecimal minLng, @Param("maxLng") BigDecimal maxLng);

    // 중심에서 반경 (m) 안 — 네모로 먼저 거르고, 정확한 거리는 서비스에서 한 번 더
    default List<Place> findActiveNear(GeoPoint center, int radiusM) {
        GeoBox box = center.boxAround(radiusM);
        return findActiveInBox(box.minLat(), box.maxLat(), box.minLng(), box.maxLng());
    }

    // 카카오 검색 결과 저장 : 이미 있는지
    Optional<Place> findByKakaoPlaceId(String kakaoPlaceId);

    // 직접 태깅한 시드와 이름이 같으면 연결
    Optional<Place> findFirstByNameAndKakaoPlaceIdIsNull(String name);

    // 시드 : 같은 이름 장소가 있으면 건너뜀
    boolean existsByName(String name);
}