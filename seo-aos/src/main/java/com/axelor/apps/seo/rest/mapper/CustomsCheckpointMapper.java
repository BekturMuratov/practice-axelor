package com.axelor.apps.seo.rest.mapper;

import com.axelor.apps.seo.db.CustomsCheckpoint;
import com.axelor.apps.seo.rest.dto.CustomsCheckpointDTO;

public class CustomsCheckpointMapper {

    public static CustomsCheckpointDTO toDTO(CustomsCheckpoint ccp) {
        if (ccp == null) return null;

        CustomsCheckpointDTO dto = new CustomsCheckpointDTO();
        dto.setId(ccp.getId());
        dto.setCcpName(ccp.getCcpName());
        dto.setDirection(ccp.getDirection());
        dto.setOnlinePayments(ccp.getOnlinePayments());
        dto.setPriceForBooking(ccp.getPriceForBooking());
        return dto;
    }
}
