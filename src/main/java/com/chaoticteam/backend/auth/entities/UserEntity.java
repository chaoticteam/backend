package com.chaoticteam.backend.auth.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "users")
public class UserEntity {

    public UserEntity() {
        this.roleEntity = null;
        this.profileEntity = null;
    }
    public UserEntity(String username, String email, String password,ProfileEntity profileEntity) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.roleEntity = null;
        this.profileEntity = profileEntity;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String email;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean verified;

    @JsonIgnore
    private String password;

    // foreign key to RoleEntity one to many
    @ManyToOne(optional = true)
    @JoinColumn(name = "role_id", nullable = true)
    private RoleEntity roleEntity;

    // foreign key to ProfileEntity one to one
    @OneToOne
    @JoinColumn(name = "profile_id")
    private ProfileEntity profileEntity;
}