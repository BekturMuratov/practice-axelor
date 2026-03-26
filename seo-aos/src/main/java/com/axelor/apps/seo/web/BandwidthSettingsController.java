package com.axelor.apps.seo.web;

import com.axelor.i18n.I18n;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class BandwidthSettingsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public void setLiveQueuePercentage(ActionRequest request, ActionResponse response) {
        Integer maxVehiclesPerHours = (Integer) request.getContext().get("maxVehiclesPerHours");
        BigDecimal percentBooking = (BigDecimal) request.getContext().get("percentBooking");
        Integer reserve = (Integer) request.getContext().get("reserve");

        if (maxVehiclesPerHours == null || percentBooking == null) {
            return;
        }
        if (reserve == null) {
            reserve = 0;
        }

        if (percentBooking.compareTo(BigDecimal.ZERO) < 0
                || percentBooking.compareTo(BigDecimal.valueOf(100)) > 0) {
            response.setError(I18n.get("Percent booking must be between 0 and 100"));
            return;
        }

        if (reserve > maxVehiclesPerHours) {
            response.setError(I18n.get("Reserve cannot be greater than maximum vehicles per hour"));
            return;
        }

        int effectiveCapacity = maxVehiclesPerHours - reserve;
        BigDecimal effectiveCapacityBD = BigDecimal.valueOf(effectiveCapacity);
        BigDecimal bookingSlotsBD = effectiveCapacityBD
                .multiply(percentBooking)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
        int bookingSlots = bookingSlotsBD.intValue();
        int liveSlots = effectiveCapacity - bookingSlots;
        BigDecimal percentLiveQueue = BigDecimal.valueOf(100).subtract(percentBooking);


        response.setValue("percentLiveQueue", percentLiveQueue);
        response.setValue("bookingSlots", bookingSlots);
        response.setValue("liveSlots", liveSlots);

    }

}
