package com.chaoticteam.backend.auth.dto;

import java.util.List;

import com.chaoticteam.backend.auth.entities.ProfileEntity;

public record ProfileResponse(
    Long id,
    Long userId,
    String firstName,
    String lastName,
    String photo,
    String bio,
    String linkedin,
    String github,
    String gitlab,
    String discord,
    String twitter,
    String facebook,
    String instagram,
    String youtube,
    String website,
    String specialties,
    String skills,
    String languages,
    String hobbies,
    boolean portfolio,
    List<TelephoneResponse> telephone
) {

    public static ProfileResponse from(ProfileEntity entity, Long userId) {
        if (entity == null) {
            return null;
        }
        List<TelephoneResponse> telephones = entity.getTelephoneEntity() == null
            ? List.of()
            : entity.getTelephoneEntity().stream().map(TelephoneResponse::from).toList();
        return new ProfileResponse(
            entity.getId(),
            userId,
            entity.getFirstName(),
            entity.getLastName(),
            entity.getPhoto(),
            entity.getBio(),
            entity.getLinkedin(),
            entity.getGithub(),
            entity.getGitlab(),
            entity.getDiscord(),
            entity.getTwitter(),
            entity.getFacebook(),
            entity.getInstagram(),
            entity.getYoutube(),
            entity.getWebsite(),
            entity.getSpecialties(),
            entity.getSkills(),
            entity.getLanguages(),
            entity.getHobbies(),
            entity.isPortfolio(),
            telephones
        );
    }
}
