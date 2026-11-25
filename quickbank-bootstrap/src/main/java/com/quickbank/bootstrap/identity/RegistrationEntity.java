package com.quickbank.bootstrap.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "id_registrations")
public class RegistrationEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "username", length = 64, nullable = false)
    private String username;

    @Column(name = "email", length = 255, nullable = false)
    private String email;

    @Column(name = "first_name", length = 80)
    private String firstName;

    @Column(name = "last_name", length = 80)
    private String lastName;

    @Column(name = "keycloak_user_id", length = 36, nullable = false)
    private String keycloakUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RegistrationEntity() {
    }

    public RegistrationEntity(
            String id,
            String username,
            String email,
            String firstName,
            String lastName,
            String keycloakUserId,
            Instant createdAt
    ) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.keycloakUserId = keycloakUserId;
        this.createdAt = createdAt;
    }

    public String id() {
        return id;
    }

    public String username() {
        return username;
    }

    public String email() {
        return email;
    }

    public String keycloakUserId() {
        return keycloakUserId;
    }

    public void rename(String email, String firstName, String lastName) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }
}
