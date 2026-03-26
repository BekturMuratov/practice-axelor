package com.axelor.apps.seo.rest.mapper;

import com.axelor.apps.seo.db.Booking;
import com.axelor.apps.seo.db.CustomsCheckpoint;
import com.axelor.apps.seo.db.repo.BookingRepository;
import com.axelor.apps.seo.db.repo.CustomsCheckpointRepository;
import com.axelor.apps.seo.rest.dto.BookingRequestDTO;
import com.axelor.apps.seo.rest.exception.BusinessException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.ThreadLocalRandom;

import static com.axelor.apps.seo.utils.StatusConstants.*;

public class BookingMapper {
    private final CustomsCheckpointRepository customsCheckpointRepository;
    private final BookingRepository bookingRepository;
    private final HttpServletRequest httpRequest;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Inject
    public BookingMapper(CustomsCheckpointRepository customsCheckpointRepository, BookingRepository bookingRepository, HttpServletRequest httpRequest) {
        this.customsCheckpointRepository = customsCheckpointRepository;
        this.bookingRepository = bookingRepository;
        this.httpRequest = httpRequest;
    }

    public Booking toEntity(BookingRequestDTO request) {
        Booking booking = new Booking();
        updateEntity(request, booking);
        booking.setCodeWord(generateCodeWord());
        applyBookingStatus(booking);
//        String clientIp = getClientIp();                                                                                /* Получения IP клиента */
//        validateIpLimit(clientIp);                                                                                      /* IP-валидация */
//        booking.setClientIp(clientIp);
        return booking;
    }

    public void updateEntity(BookingRequestDTO request, Booking booking) {
        booking.setQueueType(request.getQueueType());

        if (BOOKING_TIMED_QUEUE.equalsIgnoreCase(request.getQueueType())) {
            if (request.getBookingDate() == null || request.getBookingDate().isBlank()) {
                throw new BusinessException("Booking date is required", 400);
            }
            if (request.getBookingTime() == null || request.getBookingTime().isBlank()) {
                throw new BusinessException("Booking time is required", 400);
            }
            LocalDate bookingDate;
            try {
                bookingDate = LocalDate.parse(request.getBookingDate(), DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                throw new BusinessException("Invalid date format. Use dd.MM.yyyy", 400);
            }
            if (bookingDate.isBefore(LocalDate.now())) {
                throw new BusinessException("Booking date cannot be in the past", 400);
            }
            booking.setBookingDate(bookingDate);
            booking.setBookingTime(request.getBookingTime());

        } else if (BOOKING_LIVE_QUEUE.equalsIgnoreCase(request.getQueueType())) {
            booking.setBookingDate(LocalDate.now());
            booking.setBookingTime(null);
        }

        booking.setFullNameDriver(request.getFullNameDriver());
        booking.setPhoneNumber(request.getPhoneNumber());
        booking.setPlateNo(request.getPlateNo());
        booking.setDeclarationType(request.getDeclarationType());
        booking.setDeclarationNumber(request.getDeclarationNumber());

        String typeOfCargo = request.getTypeOfCargo();
        booking.setTypeOfCargo(
                (typeOfCargo == null || typeOfCargo.isEmpty())
                        ? REGISTRATION_TYPE_OF_CARGO_NORMAL
                        : typeOfCargo
        );


        if (request.getCcpId() != null) {
            CustomsCheckpoint ccp = customsCheckpointRepository.find(request.getCcpId());
            if (ccp == null) {
                throw new BusinessException("CCP not found: " + request.getCcpId(), 400);
            }
            booking.setCcp(ccp);
        } else {
            throw new BusinessException("CCP id is required", 400);
        }
    }

    private void applyBookingStatus(Booking booking) {
        boolean isAdvance = booking.getCcp() != null &&
                        CUSTOMS_ONLINE_PAYMENT_YES.equalsIgnoreCase(booking.getCcp().getOnlinePayments());
        booking.setBookingStatus(isAdvance ? BOOKING_STATUS_ADVANCE : BOOKING_STATUS_BOOKED);
    }

    private String generateCodeWord() {
        int randomPart = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        return String.format("%06d", randomPart);
    }

    private void validateIpLimit(String clientIp) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);

        long count = bookingRepository.all()
                .filter("self.clientIp = :ip AND self.createdOn >= :threshold")
                .bind("ip", clientIp)
                .bind("threshold", threshold)
                .count();
        if (count > 0) {
            throw new BusinessException("You have already created a booking recently. Please wait.", 429);
        }
    }

    private String getClientIp() {
        String xfHeader = httpRequest.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isBlank()) {
            return httpRequest.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
