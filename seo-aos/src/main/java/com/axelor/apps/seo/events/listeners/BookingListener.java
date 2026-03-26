package com.axelor.apps.seo.events.listeners;

import com.axelor.apps.seo.db.Booking;
import com.axelor.apps.seo.db.EventTracker;
import com.axelor.apps.seo.db.Requisite;
import com.axelor.apps.seo.service.CrudService;
import com.axelor.auth.db.User;
import com.axelor.event.Observes;
import com.axelor.events.PostRequest;
import com.axelor.events.RequestEvent;
import com.google.inject.name.Named;

import javax.inject.Inject;
import javax.persistence.PostPersist;
import javax.persistence.PreRemove;
import javax.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static com.axelor.apps.seo.utils.StatusConstants.REQUISITE_TYPE_BOOKING;
import static org.jboss.resteasy.plugins.guice.i18n.LogMessages.LOGGER;

public class BookingListener {
    @Inject
    private CrudService crudService;
    @Inject
    private HttpServletRequest request;

    private static final ThreadLocal<List<Booking>> threadLocalBookings =
            ThreadLocal.withInitial(ArrayList::new);

    @PostPersist
    public void afterPersist(Booking booking) {
        String prefix = "BKG";
        String number = String.format("%09d", booking.getId());
        booking.setuIdBooking(prefix + number);

        Requisite requisite = new Requisite();
        requisite.setRequisiteType(REQUISITE_TYPE_BOOKING);
        booking.setRequisite(requisite);
        requisite.setBooking(booking);
    }

    @PreRemove
    private void preRemove(Booking booking) {
        threadLocalBookings.get().add(booking);
        LOGGER.debug("Booking set in ThreadLocal during preRemove: " + booking);
    }

    public static String getBookingData() {
        List<Booking> bookings = threadLocalBookings.get();
        if (bookings.isEmpty()) return "No booking data available";
        return bookings.stream().map(Booking::toString)
                .collect(Collectors.joining("\n"));
    }

    public void trackDeletion(@Observes @Named(RequestEvent.REMOVE) PostRequest event) {
        try {
            List<Booking> bookings = threadLocalBookings.get();
            if (!bookings.isEmpty()) {
                String clientIp = request.getRemoteAddr();
                User user = event.getRequest() != null ? event.getRequest().getUser() : null;
                for (Booking b : bookings) {
                    EventTracker tracker = new EventTracker();

                    tracker.setTypeOfEvent("REMOVE");
                    tracker.setEntity("Booking");
                    tracker.setEventTime(LocalDateTime.now());
                    tracker.setClientIp(clientIp);
                    tracker.setNumberOfRegistration(b.getuIdBooking());
                    tracker.setCcp(b.getCcp());
                    if (user != null) {
                        tracker.setUserName(user);
                        if (user.getGroup() != null) tracker.setUserGroup(user.getGroup());
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append(b.toString());
                    sb.append("\nRequisite: ").append(b.getRequisite() != null ? b.getRequisite().toString() : "[]");
                    tracker.setSavedData(sb.toString());
                    crudService.persistObject(tracker);
                }
            } else {
                LOGGER.warn("No booking data found in ThreadLocal.");
            }
        } catch (Exception e) {
            LOGGER.error("Error tracking deletion for Booking: " + e.getMessage(), e);
        } finally {
            threadLocalBookings.remove();
        }
    }
}