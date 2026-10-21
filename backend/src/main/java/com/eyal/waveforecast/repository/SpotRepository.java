package com.eyal.waveforecast.repository;

import com.eyal.waveforecast.domain.Spot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpotRepository extends JpaRepository<Spot, Long> {

    /** Used by the hourly ingestion run. Ordering keeps logs readable. */
    List<Spot> findByActiveTrueOrderByDisplayOrderAsc();

    /**
     * Slug lookup for the API. Public URLs use {@code /api/spots/netanya},
     * never {@code /api/spots/7} — slugs are stable across reseeds and the
     * numeric ids are not.
     */
    Optional<Spot> findBySlugAndActiveTrue(String slug);

    List<Spot> findByRegionAndActiveTrueOrderByDisplayOrderAsc(Spot.Region region);

    /**
     * Nearest active spots to a coordinate, closest first.
     *
     * <p>This is the whole reason PostGIS is in the stack. Because
     * {@code shore_point} is {@code geography} rather than {@code geometry},
     * the {@code <->} operator returns metres on the spheroid — no projection,
     * no degree-to-kilometre fudge factor. Swap to {@code geometry} and this
     * silently starts returning degrees, which are not a distance.
     *
     * <p>The GIST index on {@code shore_point} makes the KNN ordering an index
     * scan rather than a full table sort.
     *
     * <p>Native query: JPQL has no spatial operators.
     *
     * @param lon longitude — note the order, {@code ST_MakePoint} is (x, y)
     * @param lat latitude
     */
    @Query(value = """
            SELECT * FROM spot
            WHERE active = true
            ORDER BY shore_point <-> ST_MakePoint(:lon, :lat)::geography
            LIMIT :limit
            """, nativeQuery = true)
    List<Spot> findNearest(@Param("lat") double lat,
                           @Param("lon") double lon,
                           @Param("limit") int limit);

    /**
     * Distance in metres from a coordinate to one spot's shore point.
     * Handy for a "23 km away" label next to the nearest-spot result.
     */
    @Query(value = """
            SELECT ST_Distance(shore_point, ST_MakePoint(:lon, :lat)::geography)
            FROM spot WHERE id = :spotId
            """, nativeQuery = true)
    Double distanceMeters(@Param("spotId") Long spotId,
                          @Param("lat") double lat,
                          @Param("lon") double lon);
}
