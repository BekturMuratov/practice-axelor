package com.axelor.apps.svh.web;

import com.axelor.apps.svh.db.Registration;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

import java.time.LocalDateTime;

public class RegistrationController {

    public void setCurrentDate(ActionRequest request, ActionResponse response) {

        Registration registration =
                request.getContext().asType(Registration.class);

        if (registration.getCreatedOn() == null) {

            LocalDateTime now = LocalDateTime.now();
            registration.setCreatedOn(now);

            response.setValue("createdOn", now);
        }
    }

    public void normalizeOnChange(ActionRequest request, ActionResponse response) {

        Registration registration =
                request.getContext().asType(Registration.class);

        response.setValue("plate_no", normalize(registration.getPlate_no()));
        response.setValue("plate_no_trailer", normalize(registration.getPlate_no_trailer()));
        response.setValue("serial_number", normalize(registration.getSerial_number()));
        response.setValue("vin_code", normalize(registration.getVin_code()));
    }

    public void normalizeAndValidateOnSave(ActionRequest request, ActionResponse response) {

        Registration registration =
                request.getContext().asType(Registration.class);

        registration.setPlate_no(normalize(registration.getPlate_no()));
        registration.setPlate_no_trailer(normalize(registration.getPlate_no_trailer()));
        registration.setSerial_number(normalize(registration.getSerial_number()));

        String vin = normalize(registration.getVin_code());

        if (vin != null && vin.length() != 17) {
            response.setError("VIN должен содержать ровно 17 символов");
            return;
        }

        registration.setVin_code(vin);

        response.setValues(registration);
    }


    private String normalize(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value
                .trim()
                .replaceAll("\\s+", "")
                .toUpperCase();
    }
}