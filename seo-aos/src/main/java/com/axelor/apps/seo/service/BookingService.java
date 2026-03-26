package com.axelor.apps.seo.service;

import com.axelor.apps.seo.db.Booking;
import com.axelor.apps.seo.db.CustomsCheckpoint;
import com.axelor.apps.seo.rest.dto.BookingRequestDTO;

import javax.ws.rs.core.Response;
import java.time.LocalDateTime;

public interface BookingService {

    Booking findByPlateNo (String plateNo, LocalDateTime createdOn, CustomsCheckpoint cpp);

    Response create(BookingRequestDTO request);

    Response update(Long id, String codeWord, BookingRequestDTO request);

    Response cancel(Long id, String codeWord);

    Response getSlots(String bookingDateStr, Long ccpId);

    Response getByDate(String bookingDate, Long ccpId);

    Response getByPlateAndCode(String plateNo, String codeWord);

    Response activeRegistrationForBooking(String plateNo, Long ccpId);
}
