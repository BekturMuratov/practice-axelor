package com.axelor.apps.seo.rest.dto;

import com.axelor.apps.seo.db.Booking;
import com.axelor.apps.seo.rest.dto.viewsDto.Views;
import com.axelor.apps.seo.rest.mapper.CustomsCheckpointMapper;
import com.fasterxml.jackson.annotation.JsonView;

import java.util.List;

public class BookingDTO {
    @JsonView({Views.Create.class, Views.Update.class})
    public Long id;
    @JsonView({Views.Create.class, Views.Update.class})
    public String uIdBooking;
    @JsonView({Views.Create.class, Views.Update.class})
    public String queueType;
    @JsonView({Views.Create.class, Views.Update.class})
    public String bookingDate;
    @JsonView({Views.Create.class, Views.Update.class})
    public String bookingTime;
    @JsonView({Views.Create.class, Views.Update.class})
    public String fullNameDriver;
    @JsonView({Views.Create.class, Views.Update.class})
    public String phoneNumber;
    @JsonView({Views.Create.class, Views.Update.class})
    public String plateNo;
    @JsonView({Views.Create.class, Views.Update.class})
    public String typeOfCargo;
    @JsonView({Views.Create.class, Views.Update.class})
    public String declarationType;
    @JsonView({Views.Create.class, Views.Update.class})
    public String declarationNumber;
    @JsonView({Views.Create.class})
    public String isPaid;
    @JsonView({Views.Create.class})
    public String paymentNumber;
    @JsonView({Views.Create.class})
    public String paymentDate;
    @JsonView({Views.Create.class, Views.Update.class})
    public String codeWord;
    @JsonView({Views.Create.class, Views.Update.class})
    public String bookingStatus;
    @JsonView({Views.Create.class, Views.Update.class})
    public String requisite;
    @JsonView({Views.Create.class, Views.Update.class})
    public CustomsCheckpointDTO ccp;
    public String registrationStatus;

    public static BookingDTO fromEntity(Booking booking , List<MetaSelectDTO> slots) {
        BookingDTO dto = new BookingDTO();
        dto.id = booking.getId();
        dto.uIdBooking = booking.getuIdBooking() != null ? booking.getuIdBooking() : "";
        dto.queueType = booking.getQueueType();
        dto.bookingDate = booking.getBookingDate() != null ? booking.getBookingDate().toString() : null;
        if (booking.getBookingTime() != null && slots != null) {
            dto.bookingTime = slots.stream()
                    .filter(s -> s.getValue().equals(booking.getBookingTime()))
                    .map(MetaSelectDTO::getTitle)
                    .findFirst()
                    .orElse(booking.getBookingTime());
        }
        dto.fullNameDriver = booking.getFullNameDriver();
        dto.phoneNumber = booking.getPhoneNumber();
        dto.plateNo = booking.getPlateNo();
        dto.typeOfCargo = booking.getTypeOfCargo();
        dto.declarationType = booking.getDeclarationType();
        dto.declarationNumber = booking.getDeclarationNumber();
        dto.isPaid = booking.getIsPaid();
        dto.paymentNumber = booking.getPaymentNumber();
        dto.paymentDate = booking.getPaymentDate() != null ? booking.getPaymentDate().toString() : null;
        dto.codeWord = booking.getCodeWord();
        dto.bookingStatus = booking.getBookingStatus();
        dto.requisite = booking.getRequisite().getRequisiteNumber();

        if (booking.getCcp() != null) {
            dto.ccp = CustomsCheckpointMapper.toDTO(booking.getCcp());
        }
        dto.registrationStatus = booking.getRegistration() != null ? booking.getRegistration().getRegistrationStatus() : null;
        return dto;
    }
}