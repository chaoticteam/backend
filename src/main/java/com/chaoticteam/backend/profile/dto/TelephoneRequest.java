package com.chaoticteam.backend.profile.dto;

public record TelephoneRequest(String phoneNumber, String countryCode, boolean whatsapp, boolean telegram) {
}
