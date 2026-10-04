package com.photobuddy.service.impl;

import com.photobuddy.dto.auth.AuthResponse;
import com.photobuddy.dto.auth.AuthUserResponse;
import com.photobuddy.dto.auth.LoginRequest;
import com.photobuddy.dto.auth.RegisterRequest;
import com.photobuddy.entity.RefreshToken;
import com.photobuddy.entity.User;
import com.photobuddy.entity.UserRole;
import com.photobuddy.repository.RefreshTokenRepository;
import com.photobuddy.repository.UserRepository;
import com.photobuddy.security.AuthenticatedUser;
import com.photobuddy.security.JwtTokenService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokens;

    @Value("${app.jwt.refresh-token-days:14}")
    private long refreshTokenDays;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens,
                       PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                       JwtTokenService jwtTokens) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokens = jwtTokens;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        String username = request.username().trim().toLowerCase();
        if (!request.password().equals(request.confirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password confirmation does not match");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at most 72 UTF-8 bytes");
        }
        if (users.existsByEmail(email) || users.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email or username is already in use");
        }
        User user = new User();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setGender(request.gender());
        user.setBio(blankToNull(request.bio()));
        user.setPhotographer(request.isPhotographer());
        user.setProfilePicture(blankToNull(request.profilePicture()));
        user = users.saveAndFlush(user);
        return issueSession(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        User user = users.findById(principal.id()).orElseThrow(
                () -> new UsernameNotFoundException("User not found"));
        return issueSession(user);
    }

    @Transactional
    public AuthResponse refresh(String rawToken) {
        RefreshToken current = refreshTokens.findByTokenHashForUpdate(hash(rawToken)).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired"));
        if (current.isRevoked() || !current.getExpiresAt().isAfter(Instant.now()) || !current.getUser().isEnabled()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired");
        }
        current.revoke();
        return issueSession(current.getUser());
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        refreshTokens.findByTokenHashForUpdate(hash(rawToken)).ifPresent(RefreshToken::revoke);
    }

    @Transactional(readOnly = true)
    public AuthUserResponse me(AuthenticatedUser principal) {
        User user = users.findById(principal.id()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return toResponse(user);
    }

    private AuthResponse issueSession(User user) {
        byte[] random = new byte[48];
        RANDOM.nextBytes(random);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        refreshTokens.save(new RefreshToken(user, hash(refreshToken),
                Instant.now().plus(refreshTokenDays, ChronoUnit.DAYS)));
        return new AuthResponse("Bearer", jwtTokens.createAccessToken(user),
                jwtTokens.getAccessTokenLifetimeSeconds(), refreshToken, toResponse(user));
    }

    private AuthUserResponse toResponse(User user) {
        return new AuthUserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getUsername(),
                user.getEmail(), List.of("ROLE_" + user.getRole().name()));
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
