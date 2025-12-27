package io.github.ryamal4.passengerflow.service.busmodel;

import io.github.ryamal4.passengerflow.model.BusModel;

public interface IBusModelService {
    void create(BusModel model);

    BusModel findById(Long id);

    BusModel update(BusModel model);
}
