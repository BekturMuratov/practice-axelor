package com.axelor.apps.seo.service.impl;

import com.axelor.apps.seo.db.*;
import com.axelor.apps.seo.db.repo.BookingRepository;
import com.axelor.apps.seo.db.repo.CameraRepository;
import com.axelor.apps.seo.db.repo.RegistrationRepository;
import com.axelor.apps.seo.service.CrudService;
import com.axelor.apps.seo.service.RegistrationService;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.axelor.apps.seo.utils.StatusConstants.*;

public class RegistrationServiceImpl implements RegistrationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final CrudService crudService;
    private final RegistrationRepository registrationRepository;
    private final BookingRepository bookingRepository;
    private final CameraRepository cameraRepository;
    private final Provider<HttpServletRequest> request;

    @Inject
    public RegistrationServiceImpl(CrudService crudService, RegistrationRepository registrationRepository, BookingRepository bookingRepository, CameraRepository cameraRepository, Provider<HttpServletRequest> request) {
        this.crudService = crudService;
        this.registrationRepository = registrationRepository;
        this.bookingRepository = bookingRepository;
        this.cameraRepository = cameraRepository;
        this.request = request;
    }

    @Override
    public void validation(ActionRequest request, ActionResponse response, String parameterName) {
        try {
            String parameterValue = (String) request.getContext().get(parameterName);
            if (parameterValue != null && !parameterValue.isEmpty()) {
                String sanitizedValue = parameterValue.replaceAll("[^a-zA-Z0-9/]", "");
                sanitizedValue = sanitizedValue.toUpperCase(Locale.ROOT);
                switch (parameterName) {
                    case REGISTRATION_TRANSPORT_TYPE_FIELD_PLATE_NO:
                    case REGISTRATION_TRANSPORT_TYPE_FIELD_PLATE_NO_TRAILER:
                        sanitizedValue = StringUtils.substring(sanitizedValue, 0, 15);
                        break;
                    default:
                        break;
                }
                response.setValue(parameterName, sanitizedValue);
            }
        } catch (Exception e) {
            LOGGER.error("Error processing parameter", e);
        }
    }

    @Override
    public void linkBookingToRegistration(Registration registration, Booking booking) {
        if (registration.getBooking() != null) {
            throw new IllegalStateException(
                    "Registration already linked to a booking"
            );
        }
        registration.setBooking(booking);
        crudService.persistObject(registration);

        LOGGER.info("Booking {} linked to registration {}", booking.getId(), registration.getId());
    }

    @Override
    public void recalculateQueueSorting(boolean isCall) {

        List<Registration> list = registrationRepository.all()
                .filter("self.registrationStatus = :status")
                .bind("status", REGISTRATION_STATUS_ARRIVED)
                .fetch();

        LocalDateTime now = LocalDateTime.now();
        int currentHour = now.getHour();

        for (Registration r : list) {

            String newSortingType;

            Booking booking = r.getBooking();
            if(booking != null){


                String bookingTime = booking.getBookingTime();

                if (bookingTime == null || bookingTime.isEmpty()) {
                    continue;
                }

                int slot = Integer.parseInt(bookingTime);

                if (REGISTRATION_TYPE_OF_CARGO_PRIORITY.equals(booking.getTypeOfCargo())) {
                    if (currentHour == slot) {
                        newSortingType = REGISTRATION_SORTING_TYPE_PRIORITY;
                    } else {
                        newSortingType = null;
                    }

                } else if (currentHour > slot) {
                    newSortingType = REGISTRATION_SORTING_TYPE_EMERGENCY;

                } else if (currentHour == slot) {
                    newSortingType = REGISTRATION_SORTING_TYPE_TIMED_QUEUE;

                } else {
                    newSortingType = null;
                }

            } else {
                newSortingType = REGISTRATION_SORTING_TYPE_LIVE_QUEUE;
            }

            if (!isCall && !Objects.equals(newSortingType, r.getSortingType())) {
                r.setSortingType(newSortingType);
                crudService.persistObject(r);
            }
        }

        if (isCall) {
            // Сортируем список по типу сортировки и времени создания
            List<Registration> sortedList = list.stream()
                    .sorted(Comparator
                            .comparing((Registration r) -> {
                                if (r.getSortingType() == null) return Integer.MAX_VALUE;
                                try {
                                    return Integer.parseInt(r.getSortingType().split("\\.")[0].trim());
                                } catch (NumberFormatException e) {
                                    return Integer.MAX_VALUE;
                                }
                            })
                            .thenComparing(Registration::getCreatedOn)
                    )
                    .collect(Collectors.toList());

            if (!sortedList.isEmpty()) {
                Registration first = sortedList.get(0);
                first.setRegistrationStatus(REGISTRATION_STATUS_CALLED);
                first.setDateTimeCall(LocalDateTime.now());
                crudService.persistObject(first);

                LOGGER.info("Vehicle {} called at {}", first.getPlateNo(), first.getDateTimeCall());
            }
        }

    }

    @Override
    public void updateRegistrationSortingAndLiveQueue() {
        // Фоновый процесс, который проверяет «зависшие» машины
        processStalledRegistrations();
        // Фоновый процесс, который меняет статус бронирования
        checkAndCancelAdvanceBookings();
        // Для каждой регистрации вычислить сортировку
        recalculateQueueSorting(false);
    }

    private void processStalledRegistrations() {
        LocalDateTime now = LocalDateTime.now();

        List<Registration> list = registrationRepository.all()
                .filter("self.registrationStatus = :status")
                .bind("status", REGISTRATION_STATUS_CALLED)
                .fetch();

        for (Registration reg : list) {
            if(reg.getDateTimeCall() != null ){
                long minutesSinceCall = Duration.between(reg.getDateTimeCall(), now).toMinutes();
                if (minutesSinceCall <= 30) continue;
                reg.setRegistrationStatus(REGISTRATION_STATUS_ARRIVED);
                reg.setQueueType(BOOKING_LIVE_QUEUE);
                reg.setSortingType(null);
                reg.setDateTimeCall(null);
                crudService.persistObject(reg);
            }
        }
    }

    private void checkAndCancelAdvanceBookings() {
        LocalDateTime expiryTime = LocalDateTime.now().minusMinutes(6);

        List<Booking> bookings = bookingRepository.all()
                .filter("self.bookingStatus = 'advanceBooking' AND self.createdOn <= :expiry")
                .bind("expiry", expiryTime)
                .fetch();
        for (Booking booking : bookings) {
            booking.setBookingStatus(BOOKING_STATUS_CANCELLED);
            crudService.persistObject(booking);
        }
    }

    @Override
    public boolean isValidTDAndDTFormat(String number) {

        String regexPattern = "\\d{8}/\\d{6}/\\d{7}";
        Pattern pattern = Pattern.compile(regexPattern);
        Matcher matcher = pattern.matcher(number);
        return matcher.matches();
    }

    @Override
    public boolean deleteBookingInRegistration(Long regId) {
        try {
            Registration registration = registrationRepository.find(regId);
            if (registration == null) {
                return false;
            }

            registration.setBooking(null);
            crudService.persistObject(registration);
            return true;
        }catch (Exception e){
            LOGGER.error("Error deleting booking in registration");
            return false;
        }
    }

    @Override
    public void deleteRecord(Long id) {
        String entryNumber = null;
        try {
            Registration registration = registrationRepository.find(id);
            if (registration == null) {
                throw new RuntimeException("Registration not found with ID: " + id);
            }
            entryNumber = registration.getuId();

            Camera camera = cameraRepository.all()
                    .filter("self.registration = ?", registration)
                    .fetchOne();

            String cameraPlate = null;
            if (camera != null) {
                cameraPlate = camera.getPlateNo();
                camera.setRegistration(null);
                crudService.persistObject(camera);
            }

            Requisite requisite = registration.getRequisite();
            if (requisite != null) {
                requisite.setRegistration(null);
                crudService.persistObject(requisite);
            }

            String entryPhotoUri = registration.getVehiclePicEntry();
            String exitPhotoUri = registration.getVehiclePicExit();

            String clientIp = request.get().getRemoteAddr();
            User user = AuthUtils.getUser();

            EventTracker tracker = new EventTracker();
            tracker.setTypeOfEvent("REMOVE");
            tracker.setEntity("Registration");
            tracker.setCcp(registration.getCcp());
            tracker.setEventTime(LocalDateTime.now());
            tracker.setClientIp(clientIp);
            tracker.setUserName(user);
            tracker.setUserGroup(user.getGroup());
            tracker.setNumberOfRegistration(registration.getuId());

            if (entryPhotoUri != null) tracker.setVehiclePicEntry(entryPhotoUri);
            if (exitPhotoUri != null) tracker.setVehiclePicExit(exitPhotoUri);

            tracker.setSavedData(getCombinedData(registration, cameraPlate));
            crudService.persistObject(tracker);
            crudService.removeObject(registration);
        }catch (Exception e) {
            throw new RuntimeException("Ошибка при удалении записи с номером въезда: " + entryNumber, e);
        }
    }

    private String getCombinedData(Registration reg, String cameraPlate) {
        StringBuilder sb = new StringBuilder();
        sb.append(reg.toString());

        if (cameraPlate != null) {
            sb.append("\nCameraPlateNo: ").append(cameraPlate);
        }
        return sb.toString();
    }
}
