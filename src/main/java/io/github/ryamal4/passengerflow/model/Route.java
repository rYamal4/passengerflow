package io.github.ryamal4.passengerflow.model;

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
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    @Setter
    private Long id;

    @Column(nullable = false)
    @Getter
    @Setter
    private String name;

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Stop> stops = new ArrayList<>();

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bus> buses = new ArrayList<>();

    public List<Stop> getStops() {
        return Collections.unmodifiableList(stops);
    }

    public List<Bus> getBuses() {
        return Collections.unmodifiableList(buses);
    }

    public void addStop(Stop stop) {
        stops.add(stop);
        stop.setRoute(this);
    }

    public void removeStop(Stop stop) {
        stops.remove(stop);
        stop.setRoute(null);
    }

    public void addBus(Bus bus) {
        buses.add(bus);
        bus.setRoute(this);
    }

    public void removeBus(Bus bus) {
        buses.remove(bus);
        bus.setRoute(null);
    }
}
