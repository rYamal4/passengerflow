package io.github.ryamal4.passengerflow.service.stop;

import io.github.ryamal4.passengerflow.model.Stop;
import io.github.ryamal4.passengerflow.repository.IStopsRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class StopsService implements IStopsService {
    private static final int NEARBY_STOPS_COUNT = 5;

    private final IStopsRepository stopsRepository;

    public StopsService(IStopsRepository stopsRepository) {
        this.stopsRepository = stopsRepository;
    }

    @Override
    public List<Stop> getNearbyStops(double lat, double lon) {
        return stopsRepository.findNearbyStops(lat, lon, NEARBY_STOPS_COUNT);
    }
}
