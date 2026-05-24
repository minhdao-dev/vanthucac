package com.vanthucac.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum UserStatus {
        ACTIVE, BANNED
    }

    public static User create(String email, String passwordHash, String fullName) {
        var user = new User();
        user.email = email;
        user.passwordHash = passwordHash;
        user.fullName = fullName;
        return user;
    }

    public void addRole(Role role) {
        roles.add(role);
    }

    public void updatePhone(String phone) {
        this.phone = phone;
    }

    public void updateProfile(String fullName, String phone, String avatarUrl) {
        if (fullName != null && !fullName.isBlank()) this.fullName = fullName;
        if (phone != null && !phone.isBlank()) this.phone = phone;
        if (avatarUrl != null && !avatarUrl.isBlank()) this.avatarUrl = avatarUrl;
    }
}
