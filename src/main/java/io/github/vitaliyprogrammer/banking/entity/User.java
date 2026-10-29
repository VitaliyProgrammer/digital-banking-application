package io.github.vitaliyprogrammer.banking.entity;

import io.github.vitaliyprogrammer.banking.entity.status.Role;
import io.github.vitaliyprogrammer.banking.entity.status.UserStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    /** UUID v7 starts with a timestamp, so new ids are appended to the index instead of scattered across it. */
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    private String email;

    private String passwordHash;

    private String firstName;

    private String lastName;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    private Instant createdAt;

    /** Self-registration always creates an active client; staff roles are never self-assigned. */
    public static User newClient(String email, String passwordHash, String firstName, String lastName, Instant now) {
        User user = new User();
        user.email = email;
        user.passwordHash = passwordHash;
        user.firstName = firstName;
        user.lastName = lastName;
        user.role = Role.CLIENT;
        user.status = UserStatus.ACTIVE;
        user.createdAt = now;
        return user;
    }

    /** Only the startup bootstrap creates admins, from ADMIN_EMAIL / ADMIN_PASSWORD. */
    public static User newAdmin(String email, String passwordHash, Instant now) {
        User user = newClient(email, passwordHash, "Bank", "Admin", now);
        user.role = Role.ADMIN;
        return user;
    }

    public boolean isBlocked() {
        return status == UserStatus.BLOCKED;
    }
}
