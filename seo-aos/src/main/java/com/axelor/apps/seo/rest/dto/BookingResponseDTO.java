package com.axelor.apps.seo.rest.dto;

import com.axelor.apps.seo.db.Booking;
import com.axelor.apps.seo.rest.mapper.CustomsCheckpointMapper;

import java.util.List;

public class BookingResponseDTO {
    public Long id;
    public String uIdBooking;
    public String queueType;
    public String bookingDate;
    public String bookingTime;
    public CustomsCheckpointDTO ccp;

    public static BookingResponseDTO fromEntity(Booking booking, List<MetaSelectDTO> slots) {
        BookingResponseDTO dto = new BookingResponseDTO();
        dto.id = booking.getId();
        dto.uIdBooking = booking.getuIdBooking();
        dto.queueType = booking.getQueueType();
        dto.bookingDate = booking.getBookingDate() != null
                ? booking.getBookingDate().toString()
                : null;
        if (booking.getBookingTime() != null && slots != null) {
            dto.bookingTime = slots.stream()
                    .filter(s -> s.getValue().equals(booking.getBookingTime()))
                    .map(MetaSelectDTO::getTitle)
                    .findFirst()
                    .orElse(booking.getBookingTime());
        }

        if (booking.getCcp() != null) {
            dto.ccp = CustomsCheckpointMapper.toDTO(booking.getCcp());
        }
        return dto;
    }
}

