package com.chaoticteam.backend.profile.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chaoticteam.backend.auth.entities.ProfileEntity;
import com.chaoticteam.backend.auth.entities.TelephoneEntity;
import com.chaoticteam.backend.auth.entities.UserEntity;
import com.chaoticteam.backend.utils.NotFoundException;

/** Builds the same vCard 2.1 text as go-server's /vcard/{username}. */
@Service
public class VCardService {

    private static final String EOL = "\r\n";

    private final ProfileService profileService;

    public VCardService(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Transactional(readOnly = true)
    public String create(String username) {
        UserEntity user = profileService.findUser(username);
        ProfileEntity profile = user.getProfileEntity();
        List<TelephoneEntity> telephones = profile == null ? null : profile.getTelephoneEntity();
        if (telephones == null || telephones.isEmpty()) {
            throw new NotFoundException("the user not has telephone");
        }
        TelephoneEntity phone = telephones.get(0);

        StringBuilder card = new StringBuilder();
        line(card, "BEGIN:VCARD");
        line(card, "VERSION:2.1");
        line(card, "N:" + profile.getFirstName());
        line(card, "FN:" + profile.getFirstName() + " " + profile.getLastName());
        line(card, "NICKNAME:" + user.getUsername());
        line(card, "TEL;CELL:+" + phone.getCountryCode() + phone.getPhoneNumber());
        line(card, "EMAIL:" + user.getEmail());
        optional(card, "PHOTO;VALUE=URI:", profile.getPhoto());
        optional(card, "X-SOCIALPROFILE;TYPE=facebook:", profile.getFacebook());
        optional(card, "X-SOCIALPROFILE;TYPE=twitter:", profile.getTwitter());
        optional(card, "X-SOCIALPROFILE;TYPE=linkedin:", profile.getLinkedin());
        optional(card, "X-SOCIALPROFILE;TYPE=github:", profile.getGithub());
        optional(card, "X-SOCIALPROFILE;TYPE=instagram:", profile.getInstagram());
        optional(card, "X-SOCIALPROFILE;TYPE=youtube:", profile.getYoutube());
        optional(card, "URL:", profile.getWebsite());
        line(card, "END:VCARD");
        return card.toString();
    }

    private static void optional(StringBuilder card, String prefix, String value) {
        if (value != null && !value.isBlank()) {
            line(card, prefix + value);
        }
    }

    private static void line(StringBuilder card, String text) {
        card.append(text).append(EOL);
    }
}
