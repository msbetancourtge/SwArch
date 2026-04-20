package com.clickmunch.AuthService.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@Table( name = "users")
public class User {
    @Id
    private Long id;
    private String name;
    private String email;
    private String username;
    private String passwordHash;
    private Role role;
    private ApprovalStatus approvalStatus;
    private LocalDateTime createdAt;

    private String phone;
    private String bio;
    private String profileImageUrl;
    private String address;
    private String governmentId;

    private String inviteToken;
    private LocalDateTime inviteTokenExpiry;
    private Long invitedRestaurantId;

    private String resetToken;
    private LocalDateTime resetTokenExpiry;

}
