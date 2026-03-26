package com.axelor.apps.svh.service;

import com.axelor.apps.svh.db.Registration;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface RegistrationService {


    BigDecimal getAllTotalOfCars(LocalDate from, LocalDate to);

    void exportRegistrationsToExcel(LocalDate from, LocalDate to, OutputStream outputStream) throws Exception;
}
