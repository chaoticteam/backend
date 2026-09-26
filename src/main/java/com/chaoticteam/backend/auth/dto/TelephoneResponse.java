package com.chaoticteam.backend.auth.dto;

import com.chaoticteam.backend.auth.entities.TelephoneEntity;

public record TelephoneResponse(Long id, String phoneNumber, String countryCode, boolean whatsapp, boolean telegram) {

    public static TelephoneResponse from(TelephoneEntity entity) {
        return new TelephoneResponse(
            entity.getId(),
            entity.getPhoneNumber(),
            entity.getCountryCode(),
            entity.isWhatsapp(),
            entity.isTelegram()
        );
    }
}
