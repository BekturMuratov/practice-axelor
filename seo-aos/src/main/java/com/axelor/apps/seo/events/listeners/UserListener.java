package com.axelor.apps.seo.events.listeners;

import com.axelor.apps.seo.db.EventTracker;
import com.axelor.apps.seo.service.CrudService;
import com.axelor.auth.db.User;
import com.axelor.event.Observes;
import com.axelor.events.PostRequest;
import com.axelor.events.RequestEvent;
import com.google.inject.name.Named;

import javax.inject.Inject;
import javax.persistence.PreRemove;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.jboss.resteasy.plugins.guice.i18n.LogMessages.LOGGER;

public class UserListener {
    @Inject
    private CrudService crudService;
    @Inject
    private HttpServletRequest request;

    private static final ThreadLocal<List<User>> threadLocalUser = ThreadLocal.withInitial(ArrayList::new);

    @PreRemove
    private void preRemove(User user) {
        List<User> userList = threadLocalUser.get();
        userList.add(user);
        LOGGER.debug("User added to ThreadLocal during preRemove: " + user);
    }

    public static String getUserData() {
        List<User> userList = threadLocalUser.get();
        if (userList.isEmpty()) {
            return "No user data available";
        }
        return userList.stream().map(User::toString)
                .collect(Collectors.joining("\n"));
    }

    public void trackDeletion(@Observes @Named(RequestEvent.REMOVE) PostRequest event) {
        try {
            List<User> userList = threadLocalUser.get();
            if (!userList.isEmpty()) {
                String clientIp = request.getRemoteAddr();
                User currentUser = event.getRequest() != null ? event.getRequest().getUser() : null;

                for (User user : userList) {
                    EventTracker tracker = new EventTracker();
                    tracker.setTypeOfEvent("REMOVE");
                    tracker.setEntity("User");
                    tracker.setEventTime(LocalDateTime.now());
                    tracker.setClientIp(clientIp);
                    tracker.setCcp(user.getCcp());
                    if (currentUser != null) {
                        tracker.setUserName(currentUser);
                        if (currentUser.getGroup() != null) {
                            tracker.setUserGroup(currentUser.getGroup());
                        } else {
                            LOGGER.warn("User group is null for user: " + currentUser.getName());
                        }
                    } else {
                        LOGGER.warn("Current user is null");
                    }
                    tracker.setSavedData(user.toString());
                    crudService.persistObject(tracker);
                }
            } else {
                LOGGER.warn("No user data found in ThreadLocal.");
            }
        } catch (Exception e) {
            LOGGER.error("Error tracking deletion for User: " + e.getMessage(), e);
        } finally {
            threadLocalUser.remove();
        }
    }
}