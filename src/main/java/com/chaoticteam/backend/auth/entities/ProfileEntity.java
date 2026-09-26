package com.chaoticteam.backend.auth.entities;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "profile")
@Getter
@Setter
public class ProfileEntity {

    public ProfileEntity(){
    }

    public ProfileEntity(String firstName, String lastName){
        this.setFirstName(firstName);
        this.setLastName(lastName);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @JsonAlias("first_name")
    private String firstName;
    @JsonAlias("last_name")
    private String lastName;

    private String photo;
    private String bio;

    // social media
    private String linkedin;
    private String github;
    private String gitlab;
    private String discord;
    private String twitter;
    private String facebook;
    private String instagram;
    private String youtube;
    private String website;

    // others
    private String specialties;
    private String skills;
    private String languages;
    private String hobbies;
    private boolean portfolio;

    // relations
    @JsonIgnore
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "profileEntity")
    private List<TelephoneEntity> telephoneEntity;
}