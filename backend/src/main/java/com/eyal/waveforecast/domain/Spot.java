package com.eyal.waveforecast.domain;

import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;

import java.time.Instant;

/**
 * A forecast location along the Israeli Mediterranean coast.
 *
 * <p>Note the two points. {@code shorePoint} is the beach — used for map
 * markers and proximity search. {@code marinePoint} sits a few kilometres
 * offshore, inside the wave model's ocean grid; the marine API returns nulls
 * for land coordinates, so this is what we actually query.
 */
@Entity
@Table(name = "spot")
public class Spot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String slug;

    @Column(name = "name_he", nullable = false, length = 100)
    private String nameHe;

    @Column(name = "name_en", nullable = false, length = 100)
    private String nameEn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Region region;

    /*
     * columnDefinition is required here. Hibernate Spatial maps a JTS Point to
     * PostGIS `geometry` by default; without this it will fail schema
     * validation against our `geography` columns.
     */
    @Column(name = "shore_point", nullable = false,
            columnDefinition = "geography(Point,4326)")
    private Point shorePoint;

    @Column(name = "marine_point", nullable = false,
            columnDefinition = "geography(Point,4326)")
    private Point marinePoint;

    @Column(name = "display_order", nullable = false)
    private Short displayOrder;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false,
            insertable = false)
    private Instant createdAt;

    protected Spot() {
        // JPA
    }

    // Convenience accessors — JTS stores lon in x, lat in y.
    @Transient
    public double getMarineLatitude() {
        return marinePoint.getY();
    }

    @Transient
    public double getMarineLongitude() {
        return marinePoint.getX();
    }

    @Transient
    public double getShoreLatitude() {
        return shorePoint.getY();
    }

    @Transient
    public double getShoreLongitude() {
        return shorePoint.getX();
    }

    public Long getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getNameHe() {
        return nameHe;
    }

    public String getNameEn() {
        return nameEn;
    }

    public Region getRegion() {
        return region;
    }

    public Short getDisplayOrder() {
        return displayOrder;
    }

    public boolean isActive() {
        return active;
    }

    public enum Region {
        NORTH, CENTER, SOUTH
    }
}
