package com.backendauth.authentication.controller;

import com.backendauth.authentication.dto.AuthResponse;
import com.backendauth.authentication.dto.LoginRequest;
import com.backendauth.authentication.dto.RegisterRequest;
import com.backendauth.authentication.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest registerRequest){
        AuthResponse authResponse = authService.register(registerRequest);
        return new ResponseEntity<>(authResponse , HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest loginRequest){
        AuthResponse authResponse  = authService.login(loginRequest);
        return new ResponseEntity<>(authResponse , HttpStatus.OK);
    }
}
