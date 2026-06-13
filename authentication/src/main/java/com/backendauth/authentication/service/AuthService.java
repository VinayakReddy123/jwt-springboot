package com.backendauth.authentication.service;

import com.backendauth.authentication.dto.AuthResponse;
import com.backendauth.authentication.dto.LoginRequest;
import com.backendauth.authentication.dto.RegisterRequest;
import com.backendauth.authentication.model.User;
import com.backendauth.authentication.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request){
        String email = request.getEmail();
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        boolean isEmailExists = userRepository.existsByEmail(email);
        if(isEmailExists){
            throw new RuntimeException("Email already exists"+email);
        }

        User user = User.builder()
                .email(email)
                .password(hashedPassword)
                .build();
        userRepository.save(user);
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public AuthResponse login(LoginRequest request){

        String email = request.getEmail();
        String rawPassword = request.getPassword();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email,rawPassword));
        User user = userRepository.findByEmail(email).orElseThrow( ()-> new RuntimeException("Email Doesnt exist"+email));
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
