package com.axelor.apps.seo.service.impl;

import com.axelor.apps.seo.db.BandwidthSettings;
import com.axelor.apps.seo.db.Booking;
import com.axelor.apps.seo.db.CustomsCheckpoint;
import com.axelor.apps.seo.db.repo.BandwidthSettingsRepository;
import com.axelor.apps.seo.db.repo.BookingRepository;
import com.axelor.apps.seo.db.repo.CustomsCheckpointRepository;
import com.axelor.apps.seo.rest.dto.TimeSlotAvailabilityDto;
import com.axelor.apps.seo.service.BandwidthSettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BandwidthSettingServiceImpl implements BandwidthSettingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final BandwidthSettingsRepository bandwidthSettingsRepository;
    private final CustomsCheckpointRepository customsCheckpointRepository;
    private final BookingRepository bookingRepository;
    @Inject
    public BandwidthSettingServiceImpl(BandwidthSettingsRepository bandwidthSettingsRepository, CustomsCheckpointRepository customsCheckpointRepository, BookingRepository bookingRepository) {
        this.bandwidthSettingsRepository = bandwidthSettingsRepository;
        this.customsCheckpointRepository = customsCheckpointRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public List<TimeSlotAvailabilityDto> generateSlotsForDate(LocalDate date, Long ccp) {

        BandwidthSettings bandwidthSettings = getActiveBandwidthSetting(date,ccp);
        if (bandwidthSettings == null) {
            return Collections.emptyList();
        }

        List<TimeSlotAvailabilityDto> slots = new ArrayList<>();

        int bookingSlots = bandwidthSettings.getBookingSlots() != null ? bandwidthSettings.getBookingSlots() : 0;

        // Получаем часы неактивности
        List<Integer> inactiveHours = new ArrayList<>();
        if (bandwidthSettings.getHoursOfInactivity() != null) {
            String[] hours = bandwidthSettings.getHoursOfInactivity().toString().split(",");
            for (String h : hours) {
                try {
                    inactiveHours.add(Integer.parseInt(h.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // Получаем список всех бронирований на эту дату и ПП
        List<Booking> bookings = getBookings(date,ccp);

        // Считаем количество занятых слотов по каждому часу
        Map<Integer, Long> bookedCountPerHour = bookings.stream()
                .filter(b -> b.getBookingStatus() != null && !b.getBookingStatus().equals("cancelled"))
                .collect(Collectors.groupingBy(
                        b -> Integer.parseInt(b.getBookingTime()), // bookingTime = "13" или "13:00-14:00"
                        Collectors.counting()
                ));


        for (int hour = 0; hour < 24; hour++) {
            String timeRange = String.format("%02d:00-%02d:00", hour, hour + 1);
            String status;
            int freeSlotCount;

            if (inactiveHours.contains(hour)) {
                status = "CLOSED";
                freeSlotCount = 0;
            } else {
                long booked = bookedCountPerHour.getOrDefault(hour, 0L);
                freeSlotCount = bookingSlots - (int) booked;
                if (freeSlotCount <= 0) {
                    freeSlotCount = 0;
                    status = "BOOKED";
                } else {
                    status = "FREE";
                }
            }

            TimeSlotAvailabilityDto dto = new TimeSlotAvailabilityDto(String.valueOf(hour),timeRange, freeSlotCount, status);
            slots.add(dto);
        }
        return slots;
    }

    private List<Booking> getBookings(LocalDate date, Long ccp) {
        return bookingRepository.all()
                .filter("self.ccp = :ccp AND self.bookingDate = :date")
                .bind("ccp", ccp)
                .bind("date", date)
                .fetch();
    }

    private BandwidthSettings getActiveBandwidthSetting(LocalDate date, Long ccpId) {
        CustomsCheckpoint ccp = customsCheckpointRepository.find(ccpId);

        return bandwidthSettingsRepository.all()
                .filter("self.ccp = :ccp " +
                        "AND self.settingsStartAction <= :date " +
                        "AND (self.settingsEndAction IS NULL OR self.settingsEndAction >= :date)")
                .bind("ccp", ccp)
                .bind("date", date.atStartOfDay())
                .fetchOne();
    }
}
