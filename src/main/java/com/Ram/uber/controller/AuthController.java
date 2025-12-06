package com.Ram.uber.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Ram.uber.dto.Login;
import com.Ram.uber.dto.Signup;
import com.Ram.uber.exception.BadRequestException;
import com.Ram.uber.model.User;
import com.Ram.uber.repository.UserRepository;
import com.Ram.uber.security.JwtService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody Signup request) {
        if (userRepository.findByUsername(request.getUsername()) != null) {
            throw new BadRequestException("User already exists");
        }
        if (!"ROLE_USER".equals(request.getRole()) && !"ROLE_DRIVER".equals(request.getRole())) {
            throw new BadRequestException("Role must be ROLE_USER or ROLE_DRIVER");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(request.getUsername(), encodedPassword, request.getRole());
        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody Login request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            UserDetails principal = (UserDetails) authentication.getPrincipal();
            User user = userRepository.findByUsername(principal.getUsername());
            String token = jwtService.generateToken(user.getUsername(), user.getRole());

            return ResponseEntity.ok(java.util.Map.of("token", token));
        } catch (BadCredentialsException ex) {
            throw new BadRequestException("Invalid username or password");
        }
    }
}
