package com.axelor.apps.seo.service.impl;

import com.axelor.apps.seo.db.Booking;
import com.axelor.apps.seo.db.Payment;
import com.axelor.apps.seo.db.Registration;
import com.axelor.apps.seo.db.Requisite;
import com.axelor.apps.seo.db.repo.PaymentRepository;
import com.axelor.apps.seo.db.repo.RequisiteRepository;
import com.axelor.apps.seo.integrations.bakai.dto.PaymentNotificationDto;
import com.axelor.apps.seo.service.CrudService;
import com.axelor.apps.seo.service.PaymentNotificationService;
import com.axelor.apps.seo.websocket.PaymentWebSocketEndpoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import org.slf4j.Logger;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static com.axelor.apps.seo.utils.StatusConstants.*;

public class PaymentNotificationServiceImpl implements PaymentNotificationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final PaymentRepository paymentRepository;
    private final RequisiteRepository requisiteRepository;
    private final CrudService crudService;

    @Inject
    public PaymentNotificationServiceImpl(PaymentRepository paymentRepository, RequisiteRepository requisiteRepository,
                                          CrudService crudService) {
        this.paymentRepository = paymentRepository;
        this.requisiteRepository = requisiteRepository;
        this.crudService = crudService;
    }

    @Override
    public void processPaymentNotification(PaymentNotificationDto notification) {
        LOGGER.info("Processing payment notification: {}", notification);
        Payment originalPayment = paymentRepository.all()
                .filter("self.operationID = :operationID")
                .bind("operationID", notification.getOperationID())
                .fetchOne();

        Payment bankNotification = new Payment();
        bankNotification.setOperationID(notification.getOperationID());
        bankNotification.setAccountNo(notification.getAccountNo());
        bankNotification.setCurrencyId(notification.getCurrencyId());
        bankNotification.setAmount(notification.getAmount());
        bankNotification.setStatus(notification.getStatus());
        bankNotification.setOperationType(PAYMENT_OPERATION_BANK_NOTIFICATION);
        bankNotification.setPaymentNumber(notification.getQrTransactionId());
        bankNotification.setPaymentDate(notification.getPaymentDate() != null ? notification.getPaymentDate().toLocalDate() : LocalDate.now());
        bankNotification.setElQrId(notification.getElqrId());
        if (originalPayment != null) {
            bankNotification.setRequisite(originalPayment.getRequisite());
            bankNotification.setCcp(originalPayment.getCcp());
            bankNotification.setIpAddress(originalPayment.getIpAddress());
            bankNotification.setQrTtlUnits(PAYMENT_QR_TTL_UNITS_MINUTES);
            bankNotification.setQrTtl(originalPayment.getQrTtl());
        } else {
            bankNotification.setQrTtl(1);
        }
        crudService.persistObject(bankNotification);
        LOGGER.info("New Payment (bank_notification) created for operationID: {}", notification.getOperationID());

        if (originalPayment == null) {
            LOGGER.warn("Original payment not found for operationID: {}. Saved notification only.", notification.getOperationID());
            return;
        }

        String requisiteNumber = originalPayment.getRequisite();                                                        // 3. Найти Requisite
        if (requisiteNumber == null || requisiteNumber.isEmpty()) {
            LOGGER.warn("No requisite number in payment: {}", notification.getOperationID());
            return;
        }

        Requisite requisite = requisiteRepository.all()
                .filter("self.requisiteNumber = :num")
                .bind("num", requisiteNumber)
                .fetchOne();

        if (requisite == null) {
            LOGGER.warn("Requisite not found: {}", requisiteNumber);
            return;
        }

        String requisiteType = requisite.getRequisiteType();
        String paymentNumber = notification.getQrTransactionId();
        LocalDate paymentDate = notification.getPaymentDate() != null ? notification.getPaymentDate().toLocalDate() : LocalDate.now();

        BigDecimal paymentAmount = notification.getAmount() != null
                ? new BigDecimal(notification.getAmount().toString()) : BigDecimal.ZERO;

        if (REQUISITE_TYPE_REGISTRATION.equals(requisiteType) && requisite.getRegistration() != null) {
            updateRegistration(requisite.getRegistration(), paymentNumber, paymentDate, paymentAmount);
        } else if (REQUISITE_TYPE_BOOKING.equals(requisiteType) && requisite.getBooking() != null) {
            updateBooking(requisite.getBooking(), paymentNumber, paymentDate, paymentAmount);
        } else {
            if (requisite.getRegistration() != null) {
                updateRegistration(requisite.getRegistration(), paymentNumber, paymentDate, paymentAmount);
            } else if (requisite.getBooking() != null) {
                updateBooking(requisite.getBooking(), paymentNumber, paymentDate, paymentAmount);
            } else {
                LOGGER.warn("No Registration or Booking found for requisite: {}", requisiteNumber);
            }
        }
    }

    private void updateRegistration(Registration registration, String paymentNumber, LocalDate paymentDate, BigDecimal paymentAmount) {
        registration.setIsPaid(REGISTRATION_IS_PAID_YES);
        registration.setPaymentNumber(paymentNumber);
        registration.setPaymentDate(paymentDate);

        BigDecimal currentOnlinePayments = registration.getOnlinePayments();
        if (currentOnlinePayments == null) {
            currentOnlinePayments = BigDecimal.ZERO;
        }
        registration.setOnlinePayments(currentOnlinePayments.add(paymentAmount));

        crudService.persistObject(registration);
        LOGGER.info("Registration updated: uId={}, paymentNumber={}, onlinePayments={}",
                registration.getuId(), paymentNumber, registration.getOnlinePayments());
    }

    private void updateBooking(Booking booking, String paymentNumber, LocalDate paymentDate, BigDecimal paymentAmount) {
        booking.setIsPaid(REGISTRATION_IS_PAID_YES);
        booking.setPaymentNumber(paymentNumber);
        booking.setPaymentDate(paymentDate);
        booking.setBookingStatus(BOOKING_STATUS_BOOKED);

        BigDecimal currentOnlinePayments = booking.getOnlinePayments();
        if (currentOnlinePayments == null) {
            currentOnlinePayments = BigDecimal.ZERO;
        }
        booking.setOnlinePayments(currentOnlinePayments.add(paymentAmount));

        crudService.persistObject(booking);
        LOGGER.info("Booking updated: uIdBooking={}, paymentNumber={}, onlinePayments={}",
                booking.getuIdBooking(), paymentNumber, booking.getOnlinePayments());

        sendWebSocketNotification(booking, paymentNumber, paymentAmount);
    }

    private void sendWebSocketNotification(Booking booking, String paymentNumber, BigDecimal paymentAmount) {
        try {
            String bookingId = booking.getuIdBooking();
            if (bookingId == null) {
                LOGGER.warn("bookingId is null, skipping WS notification");
                return;
            }

            Map<String, Object> json = new HashMap<>();
            json.put("type", "PAYMENT_SUCCESS");
            json.put("bookingId", bookingId);
            json.put("paymentNumber", paymentNumber != null ? paymentNumber : "");
            json.put("amount", paymentAmount.toPlainString());
            json.put("message", "The payment was successful!");
            json.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            String jsonString = objectMapper.writeValueAsString(json);

            PaymentWebSocketEndpoint.sendToBooking(bookingId, jsonString);
            LOGGER.info("WS notification sent for bookingId={}", bookingId);

        } catch (Exception e) {
            LOGGER.error("Failed to send WS notification for booking={}", booking.getuIdBooking(), e);
        }
    }
}