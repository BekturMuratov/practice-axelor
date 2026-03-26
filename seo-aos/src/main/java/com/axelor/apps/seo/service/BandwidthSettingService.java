package com.axelor.apps.seo.service;

import com.axelor.apps.seo.rest.dto.TimeSlotAvailabilityDto;

import java.time.LocalDate;
import java.util.List;

public interface BandwidthSettingService {

    List<TimeSlotAvailabilityDto> generateSlotsForDate (LocalDate date, Long ccpId);
}
