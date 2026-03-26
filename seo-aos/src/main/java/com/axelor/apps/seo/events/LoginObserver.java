package com.axelor.apps.seo.events;

import com.axelor.apps.seo.db.LoginEventTracker;
import com.axelor.apps.seo.db.repo.LoginEventTrackerRepository;
import com.axelor.apps.seo.service.CrudService;
import com.axelor.apps.seo.utils.StatusConstants;
import com.axelor.auth.db.User;
import com.axelor.event.Observes;
import com.axelor.events.PostLogin;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class LoginObserver {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private HttpServletRequest request;
    @Inject
    private CrudService crudService;
    @Inject
    private LoginEventTrackerRepository trackerRepository;


    void onLoginSuccess(@Observes @Named(PostLogin.SUCCESS) PostLogin event) {
        final String userCode = event.getUser().getCode();
        final String userAgent = request.getHeader("User-Agent");
        final User user = event.getUser();
        final String clientIp = request.getRemoteAddr();
        String status = StatusConstants.LOGIN_EVENT_AUTHORIZATION_STATUS_SUCCESS;

        if (userCode.equals("mbankUser")) {
            return;
        }

        if(checkingPreviousEntries(userCode)){
            return;
        }
        createLoginEvent(userCode, userAgent, user, clientIp, status);
    }

    void onLoginFailure(@Observes @Named(PostLogin.FAILURE) PostLogin event) {
        String userCode = getUserIdFromEvent(event);
        String userAgent = request.getHeader("User-Agent");
        final String clientIp = request.getRemoteAddr();
        String status = StatusConstants.LOGIN_EVENT_AUTHORIZATION_STATUS_FAILURE;

        try {
            LoginEventTracker loginEventTracker = new LoginEventTracker();
            loginEventTracker.setUserCode(userCode);
            loginEventTracker.setUserAgent(userAgent);
            loginEventTracker.setTimestamp(LocalDateTime.now());
            loginEventTracker.setClientIp(clientIp);
            loginEventTracker.setStatus(status);

            crudService.persistObject(loginEventTracker);
        } catch (Exception e) {
            LOGGER.error("Error when saving failed login event", e);
        }

    }

    private String getUserIdFromEvent(PostLogin event) {
        try {
            Optional<?> principal = (Optional<?>) event.getPrincipal();
            if (principal.isPresent()) {
                Object value = principal.get();
                String text = value.toString();
                int idIndex = text.indexOf("id:");
                if (idIndex != -1) {
                    int start = idIndex + 4;
                    int end = text.indexOf(" ", start);
                    if (end == -1) {
                        end = text.length();
                    }
                    return text.substring(start, end);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error getting user login");
        }
        return I18n.get("Unknown user");

    }

    void createLoginEvent(String userCode, String userAgent, User user, String clientIp, String status) {
        try {
            LoginEventTracker loginEventTracker = new LoginEventTracker();
            loginEventTracker.setUserCode(userCode);
            loginEventTracker.setUserAgent(userAgent);
            loginEventTracker.setTimestamp(LocalDateTime.now());
            loginEventTracker.setClientIp(clientIp);
            loginEventTracker.setStatus(status);

            if (user != null) {
                loginEventTracker.setUserName(user);
                loginEventTracker.setUserGroup(user.getGroup());
            }
            crudService.persistObject(loginEventTracker);
        } catch (Exception e) {
            LOGGER.error("Error when saving authorization event");
        }
    }

    boolean checkingPreviousEntries(String userCode){
        try {
            LocalDateTime newTime = getTime();
            List<LoginEventTracker> loginEventTrackerList = trackerRepository.all()
                    .filter("self.userCode = :userCode AND self.createdOn >= :newTime")
                    .bind("userCode", userCode)
                    .bind("newTime", newTime)
                    .fetch();

            // Если список пустой, вернуть false, иначе true
            return !loginEventTrackerList.isEmpty();
        }catch (Exception e){
            LOGGER.error("Error checking previous login entries: {}", e.getMessage());
            return true;
        }

    }

    LocalDateTime getTime(){
        return LocalDateTime.now().minusMinutes(2);
    }

}
