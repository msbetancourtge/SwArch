package com.clickmunch.AuthService.controller;

import com.clickmunch.AuthService.dto.ApiResponse;
import com.clickmunch.AuthService.dto.LoginRequest;
import com.clickmunch.AuthService.dto.LoginResponse;
import com.clickmunch.AuthService.dto.RegisterRequest;
import com.clickmunch.AuthService.dto.UserInfoResponse;
import com.clickmunch.AuthService.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest loginRequest) {
        ApiResponse<LoginResponse> response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@RequestBody RegisterRequest registerRequest) {
        ApiResponse<String> response = authService.register(registerRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{userId}")
    public UserInfoResponse GetUserInfo(@PathVariable Long userId) {
        return authService.getUserById(userId);
    }

    // Endpoint administrativo para crear usuarios con rol específico (solo para admins)
    @PostMapping("/admin/create-user")
    public ResponseEntity<ApiResponse<String>> adminCreateUser(@RequestBody RegisterRequest registerRequest) {
        ApiResponse<String> response = authService.adminCreateUser(registerRequest);
        return ResponseEntity.ok(response);
    }

}