package com.axelor.apps.seo.events.listeners;

import com.axelor.apps.seo.db.EventTracker;
import com.axelor.apps.seo.db.Registration;
import com.axelor.apps.seo.db.Requisite;
import com.axelor.apps.seo.service.CrudService;
import com.axelor.auth.db.User;
import com.axelor.event.Observes;
import com.axelor.events.PostRequest;
import com.axelor.events.RequestEvent;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.PostPersist;
import javax.persistence.PreRemove;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.axelor.apps.seo.utils.StatusConstants.REQUISITE_TYPE_REGISTRATION;
import static org.jboss.resteasy.plugins.guice.i18n.LogMessages.LOGGER;

public class RegistrationListener {
    @Inject
    private CrudService crudService;
    @Inject
    private HttpServletRequest request;

    private static final ThreadLocal<List<Registration>> threadLocalRegistrations =
            ThreadLocal.withInitial(ArrayList::new);

    @PostPersist
    public void afterPersist(Registration registration) {
        String prefix = "SEO";
        String number = String.format("%09d", registration.getId());
        registration.setuId(prefix + number);

        Requisite requisite = new Requisite();
        requisite.setRequisiteType(REQUISITE_TYPE_REGISTRATION);
        registration.setRequisite(requisite);
        requisite.setRegistration(registration);
    }

    @PreRemove
    private void preRemove(Registration registration) {
        if (registration.getBooking() != null) registration.getBooking().toString();
        if (registration.getRequisite() != null) registration.getRequisite().getRequisiteNumber().toString();

        threadLocalRegistrations.get().add(registration);
        LOGGER.debug("Registration set in ThreadLocal during preRemove: " + registration);
    }

    public static String getRegistrationData() {
        List<Registration> registrations = threadLocalRegistrations.get();
        if (registrations.isEmpty()) return "No registration data available";
        return registrations.stream().map(Registration::toString)
                .collect(Collectors.joining("\n"));
    }

    public void trackDeletion(@Observes @Named(RequestEvent.REMOVE) PostRequest event) {
        try {
            List<Registration> registrations = threadLocalRegistrations.get();
            if (!registrations.isEmpty()) {
                String clientIp = request.getRemoteAddr();
                User user = event.getRequest() != null ? event.getRequest().getUser() : null;

                for (Registration r : registrations) {
                    EventTracker tracker = new EventTracker();
                    tracker.setTypeOfEvent("REMOVE");
                    tracker.setEntity("Registration");
                    tracker.setCcp(r.getCcp());
                    tracker.setEventTime(LocalDateTime.now());
                    tracker.setClientIp(clientIp);
                    tracker.setNumberOfRegistration(r.getuId());
                    tracker.setVehiclePicEntry(r.getVehiclePicEntry());
                    tracker.setVehiclePicExit(r.getVehiclePicExit());
                    if (user != null) {
                        tracker.setUserName(user);
                        if (user.getGroup() != null) tracker.setUserGroup(user.getGroup());
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append(r.toString());
                    sb.append("\nBooking: ").append(r.getBooking() != null ? r.getBooking().toString() : "[]");
                    sb.append("\nRequisite: ").append(r.getRequisite() != null ? r.getRequisite().toString() : "[]");
                    tracker.setSavedData(sb.toString());
                    crudService.persistObject(tracker);
                }
            } else {
                LOGGER.warn("No registration data found in ThreadLocal.");
            }
        } catch (Exception e) {
            LOGGER.error("Error tracking deletion for Registration: " + e.getMessage(), e);
        } finally {
            threadLocalRegistrations.remove();
        }
    }
}