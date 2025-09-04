package io.github.ryamal4.passengerflow.persistence.entities;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "routes")
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class RouteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    @Setter
    private Long id;

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StopEntity> stops = new ArrayList<>();

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BusEntity> buses = new ArrayList<>();

    public List<StopEntity> getStops() {
        return Collections.unmodifiableList(stops);
    }

    public List<BusEntity> getBuses() {
        return Collections.unmodifiableList(buses);
    }

    public void addStop(StopEntity stop) {
        stops.add(stop);
        stop.setRoute(this);
    }

    public void removeStop(StopEntity stop) {
        stops.remove(stop);
        stop.setRoute(null);
    }

    public void addBus(BusEntity bus) {
        buses.add(bus);
        bus.setRoute(this);
    }

    public void removeBus(BusEntity bus) {
        buses.remove(bus);
        bus.setRoute(null);
    }
}
