package com.axelor.apps.seo.web;

import com.axelor.apps.seo.db.Booking;
import com.axelor.apps.seo.db.CustomsCheckpoint;
import com.axelor.apps.seo.db.Registration;
import com.axelor.apps.seo.db.Requisite;
import com.axelor.apps.seo.db.repo.RegistrationRepository;
import com.axelor.apps.seo.service.BookingService;
import com.axelor.apps.seo.service.RegistrationService;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.i18n.I18n;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.Set;

import static com.axelor.apps.seo.utils.StatusConstants.*;

public class RegisterController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final RegistrationService registrationService;
    private final BookingService bookingService;
    private final RegistrationRepository registrationRepository;

    @Inject
    public RegisterController(RegistrationService registrationService, BookingService bookingService, RegistrationRepository registrationRepository) {
        this.registrationService = registrationService;
        this.bookingService = bookingService;
        this.registrationRepository = registrationRepository;
    }

    public void validationOnRegistration(ActionRequest request, ActionResponse response) {                              // Методы корректировки правильности написания
        registrationService.validation(request, response, REGISTRATION_TRANSPORT_TYPE_FIELD_PLATE_NO);
        registrationService.validation(request, response, REGISTRATION_TRANSPORT_TYPE_FIELD_PLATE_NO_TRAILER);

        String fullNameDriver = (String) request.getContext().get(REGISTRATION_TRANSPORT_TYPE_FIELD_FULL_NAME_DRIVER);
        if (fullNameDriver != null) {
            fullNameDriver = fullNameDriver.replaceAll("[^а-яА-Яa-zA-Z\\s]", "");
        }
        response.setValue(REGISTRATION_TRANSPORT_TYPE_FIELD_FULL_NAME_DRIVER, fullNameDriver);
    }

    public void registrationOnRefresh(ActionRequest request, ActionResponse response) {
        response.setSignal("refresh-tab", null);
    }

    public void checkPlateAndFindBookingOnArrival(ActionRequest request, ActionResponse response) {
        String registrationStatus = (String) request.getContext().get("registrationStatus");
        String isPaid = (String) request.getContext().get("isPaid");
        if (!"pending".equals(registrationStatus)) {
            LOGGER.info("Search conditions not met");
            return;
        }

        Booking booking = findBooking(request);

        if (booking != null) {
            setBookingValues(response, booking, isPaid);
        } else {
            response.setValue("queueType", "liveQueue");
            response.setValue("registrationStatus", REGISTRATION_STATUS_FORMULATION);
        }
    }

    public void checkPlateAndFindBookingManual(ActionRequest request, ActionResponse response) {
        Booking booking = findBooking(request);
        String isPaid = (String) request.getContext().get("isPaid");

        if (booking != null) {
            setBookingValues(response, booking, isPaid);
        } else {
            response.setInfo(I18n.get("No booking found for plate"));
            LOGGER.info("Manual booking check: no booking found for plate " + request.getContext().get("plateNo"));
        }
    }

    private Booking findBooking(ActionRequest request) {
        String plateNoCamera = (String) request.getContext().get("plateNoCameraEntry");
        String plateNo = (String) request.getContext().get("plateNo");
        LocalDateTime createdOn = (LocalDateTime) request.getContext().get("createdOn");
        CustomsCheckpoint ccp = (CustomsCheckpoint) request.getContext().get("ccp");

        LocalDateTime actualTime = createdOn != null ? createdOn : LocalDateTime.now();
        String actualPlate = plateNo != null ? plateNo : plateNoCamera;

        if (actualPlate == null) {
            return null;
        }

        return bookingService.findByPlateNo(actualPlate, actualTime, ccp);
    }

    private void setBookingValues(ActionResponse response, Booking booking, String isPaid) {

        if(!"yes".equals(isPaid)){
            response.setValue("isPaid", booking.getIsPaid());
            response.setValue("paymentNumber", booking.getPaymentNumber());
            response.setValue("paymentDate", booking.getPaymentDate());
            response.setValue("onlinePayments",  booking.getOnlinePayments());
        }

        response.setValue("queueType", determineQueueType(booking));
        response.setValue("declarationType", booking.getDeclarationType());
        response.setValue("declarationNumber", booking.getDeclarationNumber());
        response.setValue("plateNo", booking.getPlateNo());
        response.setValue("fullNameDriver", booking.getFullNameDriver());
        response.setValue("phoneNumber", booking.getPhoneNumber());
        response.setValue("typeOfCargo", booking.getTypeOfCargo());
        response.setValue("booking", booking);
        response.setValue("bookingDate", booking.getBookingDate());
        response.setValue("bookingTime", booking.getBookingTime());
        response.setValue("registrationStatus", REGISTRATION_STATUS_ARRIVED);
    }

    private String determineQueueType(Booking booking) {
        String slotStr = booking.getBookingTime();
        int arrivalSlot = LocalDateTime.now().getHour();
        if(slotStr != null) {
            int bookingSlot = Integer.parseInt(slotStr);
            return arrivalSlot < bookingSlot ? booking.getQueueType() : "liveQueue";
        }
        return "liveQueue";
    }

    public void openEntryBarrier(ActionRequest request, ActionResponse response) {
        LOGGER.info("The barrier is open for entry");
        response.setValue("entryGateOpenedAt", LocalDateTime.now());
        response.setNotify(I18n.get("The barrier is open"));
    }

    public void openExitBarrier(ActionRequest request, ActionResponse response) {

        String registrationStatus = (String) request.getContext().get("registrationStatus");

        if(!REGISTRATION_STATUS_OFFICIAL_CAR.equals(registrationStatus)){
            response.setValue("registrationStatus" , REGISTRATION_STATUS_DEPARTED);
        }

        LOGGER.info("The barrier is open");
        response.setValue("exitGateOpenedAt", LocalDateTime.now());
        response.setNotify(I18n.get("The barrier is open"));
    }

    public void callVehicleToCheckout(ActionRequest request, ActionResponse response) {
        LOGGER.info("The vehicle is called to checkout");
        response.setValue("registrationStatus" , REGISTRATION_STATUS_CALLED);
        response.setValue("dateTimeCall", LocalDateTime.now());
        response.setValue("sortingType", REGISTRATION_SORTING_TYPE_EMERGENCY);
        response.setNotify(I18n.get("The vehicle is called to checkout"));
    }

    public void movePlateNoCameraToPlateNo(ActionRequest request, ActionResponse response) {
        try {
            String plateNoCamera = (String) request.getContext().get("plateNoCameraEntry");
            String plateNo = (String) request.getContext().get("plateNo");

            if (plateNoCamera != null && !plateNoCamera.isEmpty()) {
                if (plateNo == null || plateNo.isEmpty()) {
                    response.setValue("plateNo", plateNoCamera);
                }
            }

        }catch (Exception exception) {
            LOGGER.error("Error in movePlateNoCameraToPlateNo method: {}", exception.getMessage(), exception);
        }
    }

    public void callingQueue(ActionRequest request, ActionResponse response) {
        //При isCall=true – сортирует список по типу сортировки и createdOn, берёт первую запись, помечает её как вызванную и сохраняет.
        registrationService.recalculateQueueSorting(true);
        response.setReload(true);
    }

    public void recalculateQueueSortingCont(ActionRequest request, ActionResponse response){
        long start = System.currentTimeMillis();
        registrationService.recalculateQueueSorting(false);
        long end = System.currentTimeMillis();
        long duration = end - start;
        long seconds = duration / 1000;
        LOGGER.info("Queue sorting recalculation finished in {} seconds", seconds);
        response.setReload(true);
    }

    public void getTDAndDTFromSmartSystem(ActionRequest request, ActionResponse response) {

        try {
            String declarationNumber = (String) request.getContext().get("declarationNumber");

            if (declarationNumber == null) {
                response.addError("declarationNumber", I18n.get("Not found declaration"));
                return;
            }

            if(!registrationService.isValidTDAndDTFormat(declarationNumber)){
                response.addError("declarationNumber", I18n.get("The declaration number format is incorrect!"));
                return;
            }
        }catch (RuntimeException exception) {
            response.setNotify(exception.getMessage());
            response.setException(exception);
        }
    }

    public void setRegistrationStatusArrived(ActionRequest request, ActionResponse response) {
        String registrationStatus = (String) request.getContext().get("registrationStatus");
        String isPaid = (String) request.getContext().get("isPaid");

        if("yes".equals(isPaid)) {
            if (Set.of(
                    REGISTRATION_STATUS_PENDING,
                    REGISTRATION_STATUS_FORMULATION
            ).contains(registrationStatus)) {

                response.setValue("registrationStatus", REGISTRATION_STATUS_ARRIVED);
            }
        }else{
            response.setValue("registrationStatus", REGISTRATION_STATUS_FORMULATION);
        }
    }

    public void deleteBookingInRegistration(ActionRequest request, ActionResponse response) {
        Long regId = (Long) request.getContext().get("id");
        User user = AuthUtils.getUser();
        String code = user.getGroup().getCode();

        if (!("AD".equals(code) || "admins".equals(code) || "SU".equals(code))) {
            throw new IllegalStateException(I18n.get("Please contact the monitoring center operators to perform the adjustment"));
        }

        boolean check = registrationService.deleteBookingInRegistration(regId);

        if (Boolean.FALSE.equals(check)) {
            throw new RuntimeException(I18n.get("Error deleting record from database"));
        }
        response.setReload(true);
    }

    public Response deleteRecord(ActionRequest request, ActionResponse response) {
        Long id = (Long) request.getContext().get("id");
        if (id != null) {
            registrationService.deleteRecord(id);
            response.setNotify(I18n.get("Registration with pending has been successfully deleted."));
        } else {
            response.setError(I18n.get("Invalid request: ID is missing."));
        }
        return response;
    }

    public void generateQr(ActionRequest request, ActionResponse response) {
        try {
            Long id = (Long) request.getContext().get("id");
            Registration dbRegistration = registrationRepository.find(id);

            Requisite requisite = dbRegistration.getRequisite();

            if (requisite == null || requisite.getRequisiteNumber() == null) {
                response.setError(I18n.get("Requisite not found."));
                return;
            }

            if ("yes".equals(dbRegistration.getIsPaid())) {
                response.setAlert(I18n.get("Payment has already been made."));
                return;
            }

            String requisiteNumber = requisite.getRequisiteNumber();
            LOGGER.info("Opening payment page for requisite: {}", requisiteNumber);

            String url = "ws/public/api/seo/payment/qr-page?requisite=" + requisiteNumber;

            response.setView(ActionView.define(I18n.get("Payment QR Code"))
                    .add("html", url)
                    .param("target", "_blank")
                    .map());

        } catch (Exception e) {
            LOGGER.error("Error opening payment page: {}", e.getMessage(), e);
            response.setError(I18n.get("Error: ") + e.getMessage());
        }
    }
}
