package com.axelor.apps.eq.service;

import com.axelor.apps.eq.db.Vehicles;
import java.util.Optional;

public interface VehiclesService {
  void save(Vehicles vehicle) throws Exception;

  Optional<Vehicles> findByPlateNumber(String plateNumber);
}
