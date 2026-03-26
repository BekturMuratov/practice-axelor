package com.axelor.apps.seo.service;

import com.axelor.apps.seo.db.Booking;
import com.axelor.apps.seo.db.Registration;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

public interface RegistrationService {
    void validation(ActionRequest request, ActionResponse response, String parameterName);
    void linkBookingToRegistration(Registration registration, Booking booking);
    void recalculateQueueSorting(boolean isCall);
    void updateRegistrationSortingAndLiveQueue();
    boolean isValidTDAndDTFormat(String number);
    boolean deleteBookingInRegistration(Long regId);
    void deleteRecord(Long id);
}
