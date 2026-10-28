package com.masterboxer.springbootmultitenancy.controller;

import com.masterboxer.springbootmultitenancy.record.LoginRequest;
import com.masterboxer.springbootmultitenancy.record.TokenResponse;
import com.masterboxer.springbootmultitenancy.security.AuthProperties;
import com.masterboxer.springbootmultitenancy.security.AuthProperties.UserEntry;
import com.masterboxer.springbootmultitenancy.security.JwtTokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * Issues a JWT whose "tenant" claim is decided by the server from the
 * credentials, so a client can never choose a tenant it does not belong to.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthProperties authProperties;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;
    // Compared against when the user does not exist, so response time does not reveal valid usernames
    private final String dummyHash;

    public AuthController(AuthProperties authProperties, PasswordEncoder passwordEncoder, JwtTokenService tokenService) {
        this.authProperties = authProperties;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        Optional<UserEntry> user = authProperties.users().stream()
                .filter(u -> u.tenant().equals(request.tenant()) && u.username().equals(request.username()))
                .findFirst();

        String hash = user.map(UserEntry::passwordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
        if (user.isEmpty() || !passwordMatches) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        UserEntry entry = user.get();
        String token = tokenService.issue(entry.username(), entry.tenant(), entry.roles());
        return ResponseEntity.ok(new TokenResponse(token, "Bearer", tokenService.ttlSeconds()));
    }
}
