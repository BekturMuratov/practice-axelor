package com.axelor.apps.seo.service.impl;

import com.axelor.apps.seo.db.Booking;
import com.axelor.apps.seo.db.CustomsCheckpoint;
import com.axelor.apps.seo.db.Registration;
import com.axelor.apps.seo.db.repo.BookingRepository;
import com.axelor.apps.seo.db.repo.RegistrationRepository;
import com.axelor.apps.seo.rest.dto.*;
import com.axelor.apps.seo.rest.exception.BusinessException;
import com.axelor.apps.seo.rest.exception.responce.ApiResponse;
import com.axelor.apps.seo.rest.mapper.BookingMapper;
import com.axelor.apps.seo.service.*;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.axelor.apps.seo.utils.StatusConstants.*;

public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final CrudService crudService;
    private final BookingMapper bookingMapper;
    private final BandwidthSettingService bandwidthSettingService;
    private final RegistrationRepository registrationRepository;
    private final RegistrationService registrationService;
    private final SelectService selectService;

    @Inject
    public BookingServiceImpl(BookingRepository bookingRepository, CrudService crudService, BookingMapper bookingMapper,
                              BandwidthSettingService bandwidthSettingService, RegistrationRepository registrationRepository,
                              RegistrationService registrationService, SelectService selectService) {
        this.bookingRepository = bookingRepository;
        this.crudService = crudService;
        this.bookingMapper = bookingMapper;
        this.bandwidthSettingService = bandwidthSettingService;
        this.registrationRepository = registrationRepository;
        this.registrationService = registrationService;
        this.selectService = selectService;
    }

    @Override
    public Booking findByPlateNo(String plateNo, LocalDateTime createdOn, CustomsCheckpoint ccp) {
        if (plateNo == null || createdOn == null || ccp == null) {
            return null;
        }
        LocalDate startDate = createdOn.toLocalDate();
        LocalDate endDate = startDate.plusDays(2);

        List<Booking> bookings = bookingRepository.all()
                .filter("self.ccp = :ccp " +
                        "AND self.plateNo = :plateNo " +
                        "AND self.bookingDate >= :startDate " +
                        "AND self.bookingDate <= :endDate " +
                        "AND self.bookingStatus = :status")
                .bind("ccp", ccp)
                .bind("plateNo", plateNo)
                .bind("startDate", startDate)
                .bind("endDate", endDate)
                .bind("status", BOOKING_STATUS_BOOKED)
                .fetch();

        return  bookings.stream()
                .filter(b -> b.getBookingTime() != null)
                .min(Comparator.comparingInt(b -> Integer.parseInt(b.getBookingTime())))
                .orElse(null);
    }

    @Override
    public Response create(BookingRequestDTO request) {
        Registration registration;
        Booking booking = bookingMapper.toEntity(request);
        registration = findActiveRegistrationForLiveQueue(request.getPlateNo(), request.getCcpId());                    /* Найти последнюю регистрацию */

        String queueType = request.getQueueType();

        if (BOOKING_LIVE_QUEUE.equalsIgnoreCase(queueType) && registration == null) {
            throw new BusinessException(
                    "Vehicle must be physically present in the waiting zone to use live queue booking", 400);
        }

        if (BOOKING_TIMED_QUEUE.equalsIgnoreCase(queueType) && registration != null) {
            throw new BusinessException("Vehicle already in waiting zone cannot use timed queue booking", 400);
        }

        Booking savedBooking = crudService.persistObject(booking);


        if (registration != null) {
            registrationService.linkBookingToRegistration(registration, savedBooking);                                  /* Привязываем бронирование к регистрации */
        }

        List<MetaSelectDTO> slots = selectService.getSelection(BOOKING_SLOTS_SELECT);
        return Response.ok(BookingDTO.fromEntity(savedBooking, slots)).build();
    }

    @Override
    public Response update(Long id, String codeWord, BookingRequestDTO request) {
        Booking booking = getBookingOrThrow(id, codeWord);
        bookingMapper.updateEntity(request, booking);
        crudService.persistObject(booking);
        List<MetaSelectDTO> slots = selectService.getSelection(BOOKING_SLOTS_SELECT);
        return Response.ok(BookingDTO.fromEntity(booking, slots)).build();
    }

    @Override
    public Response cancel(Long id, String codeWord) {
        Booking booking = getBookingOrThrow(id, codeWord);
        if (BOOKING_STATUS_CANCELLED.equals(booking.getBookingStatus())) {
            throw new BusinessException("Booking already cancelled", 409);
        }
        booking.setBookingStatus(BOOKING_STATUS_CANCELLED);
        String formattedTime = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        booking.setTimeOfCancelBooking(formattedTime);
        crudService.persistObject(booking);
        return Response.ok(ApiResponse.successMessage("Booking cancelled successfully")).build();
    }

    @Override
    public Response getSlots(String bookingDateStr, Long ccpId) {
        if (bookingDateStr == null || bookingDateStr.isBlank()) {
            throw new BusinessException("Booking date is required", 400);
        }
        LocalDate bookingDate;
        try {
            bookingDate = LocalDate.parse(bookingDateStr);
        } catch (DateTimeParseException e) {
            throw new BusinessException("Invalid date format, expected yyyy-MM-dd", 400);
        }
        List<TimeSlotAvailabilityDto> slots = bandwidthSettingService.generateSlotsForDate(bookingDate, ccpId);
        return Response.ok(slots).build();
    }

    @Override
    public Response getByDate(String bookingDate, Long ccpId) {
        if (bookingDate == null || bookingDate.isBlank()) {
            throw new BusinessException("Date parameter is required", 400);
        }
        LocalDate date;
        try {
            date = LocalDate.parse(bookingDate);
        } catch (DateTimeParseException e) {
            throw new BusinessException("Invalid date format. Use yyyy-MM-dd", 400);
        }

        List<Booking> bookings = bookingRepository.all()
                .filter("self.bookingDate = :date AND (:ccpId IS NULL OR self.ccp.id = :ccpId) " +
                        "AND self.bookingStatus = :status")
                .bind("date", date)
                .bind("ccpId", ccpId)
                .bind("status", BOOKING_STATUS_BOOKED)
                .order("bookingTime")
                .fetch();

        List<MetaSelectDTO> slots = selectService.getSelection(BOOKING_SLOTS_SELECT);
        List<BookingResponseDTO> dtoList = bookings.stream()
                .map(b -> BookingResponseDTO.fromEntity(b, slots)).collect(Collectors.toList());

        return Response.ok(dtoList).build();
    }

    @Override
    public Response getByPlateAndCode(String plateNo, String codeWord) {
        if (plateNo == null || plateNo.isBlank() || codeWord == null || codeWord.isBlank()) {
            throw new BusinessException("plateNo and codeWord are required", 400);
        }
        try {
            Booking booking = bookingRepository.all()
                    .filter("self.plateNo = :plateNo AND self.codeWord = :codeWord AND self.bookingStatus != :cancelledStatus")
                    .bind("plateNo", plateNo)
                    .bind("codeWord", codeWord)
                    .bind("cancelledStatus", BOOKING_STATUS_CANCELLED)
                    .fetchOne();

            if (booking == null) {
                throw new BusinessException("Booking with this number and code word was not found", 404);
            }
            List<MetaSelectDTO> slots = selectService.getSelection(BOOKING_SLOTS_SELECT);

            return Response.ok(BookingDTO.fromEntity(booking, slots)).build();
        } catch (NoResultException e) {
            throw new BusinessException("Booking not found", 404);
        }
    }

    @Override
    public Response activeRegistrationForBooking(String plateNo, Long ccpId){
        Registration registration = findActiveRegistrationForLiveQueue(plateNo, ccpId);
        if (registration != null) {
            throw new BusinessException("This vehicle is located in the waiting area", 404);
        }
        return Response.ok(ApiResponse.successMessage("Booking are allowed")).build();
    }

    private Booking getBookingOrThrow(Long id, String codeWord) {
        if (id == null || codeWord == null || codeWord.isBlank()) {
            throw new BusinessException("Id and codeWord are required", 400);
        }
        Booking booking = bookingRepository.find(id);

        if (booking == null) {
            throw new BusinessException("Booking not found", 404);
        }
        if (!booking.getCodeWord().equals(codeWord)) {
            throw new BusinessException("Invalid code word", 403);
        }
        return booking;
    }

    private Registration findActiveRegistrationForLiveQueue(String plateNo, Long ccpId) {
        if (plateNo == null || plateNo.isBlank()) {
            throw new IllegalArgumentException("Plate number is required for live queue");
        }
        LocalDateTime limitTime = LocalDateTime.now().minusHours(72);
        List<String> allowedStatuses = List.of(REGISTRATION_STATUS_PENDING, REGISTRATION_STATUS_FORMULATION,
                REGISTRATION_STATUS_ARRIVED, REGISTRATION_STATUS_CALLED);

        return registrationRepository.all()
                .filter("self.plateNo = :plateNo AND self.ccp.id = :ccpId " +
                        "AND self.createdOn >= :limitTime " +
                        "AND self.registrationStatus IN :statuses")
                .bind("plateNo", plateNo)
                .bind("ccpId", ccpId)
                .bind("limitTime", limitTime)
                .bind("statuses", allowedStatuses)
                .order("-createdOn")
                .fetchOne();
    }
}
