package com.smartretail.auth.service;

import com.smartretail.auth.dto.request.LoginRequest;
import com.smartretail.auth.dto.request.RegisterRequest;
import com.smartretail.auth.dto.response.AuthResponse;
import com.smartretail.auth.dto.response.UserSummary;
import com.smartretail.auth.entity.RefreshToken;
import com.smartretail.auth.repository.RefreshTokenRepository;
import com.smartretail.auth.support.Identifiers;
import com.smartretail.auth.support.LoginAttempts;
import com.smartretail.common.exception.ApiException;
import com.smartretail.common.exception.ErrorCode;
import com.smartretail.user.entity.Profile;
import com.smartretail.user.entity.User;
import com.smartretail.user.enums.Role;
import com.smartretail.user.repository.ProfileRepository;
import com.smartretail.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid email/mobile or password";

    private final UserRepository users;
    private final ProfileRepository profiles;
    private final RefreshTokenRepository refreshTokens;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttempts loginAttempts;
    private final Clock clock;
    // Compared against when the account does not exist, so response time does not reveal it.
    private final String dummyHash;

    public AuthService(UserRepository users, ProfileRepository profiles, RefreshTokenRepository refreshTokens,
            TokenService tokenService, PasswordEncoder passwordEncoder, LoginAttempts loginAttempts) {
        this.users = users;
        this.profiles = profiles;
        this.refreshTokens = refreshTokens;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.loginAttempts = loginAttempts;
        this.clock = Clock.systemUTC();
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = Identifiers.normalizeEmail(request.email());
        String phone = Identifiers.normalizePhone(request.phone());
        if (email == null && phone == null) {
            throw new ApiException(ErrorCode.EMAIL_OR_PHONE_REQUIRED, "Email or mobile number is required");
        }
        if (email != null && users.existsByEmail(email)) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_REGISTERED, "An account with this email already exists");
        }
        if (phone != null && users.existsByPhone(phone)) {
            throw new ApiException(ErrorCode.PHONE_ALREADY_REGISTERED, "An account with this mobile number already exists");
        }

        User user;
        try {
            user = users.saveAndFlush(
                    new User(email, phone, passwordEncoder.encode(request.password()), Role.CUSTOMER));
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.ACCOUNT_ALREADY_EXISTS, "An account with this email or mobile number already exists");
        }
        Profile profile = profiles.save(new Profile(user.getId(), request.fullName().trim(), email, phone));
        log.info("Account registered: userId={}", user.getId());
        return issueTokens(user, profile, clock.instant());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.identifier().trim();
        loginAttempts.checkAllowed(identifier);
        Optional<User> found = Identifiers.isEmail(identifier)
                ? users.findByEmail(Identifiers.normalizeEmail(identifier))
                : findByPhoneQuietly(identifier);

        String hash = found.map(User::getPasswordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
        Optional<User> accepted = found.filter(u -> passwordMatches && u.isEnabled());
        if (accepted.isEmpty()) {
            loginAttempts.recordFailure(identifier);
            log.warn("Login failed: userId={}", found.map(User::getId).orElse(null));
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, INVALID_CREDENTIALS);
        }
        User user = accepted.get();
        loginAttempts.reset(identifier);
        log.info("Login succeeded: userId={}", user.getId());
        return issueTokens(user, profiles.findById(user.getId()).orElse(null), clock.instant());
    }

    /**
     * Exchanges a refresh token for a new pair (rotation). Presenting a token that was already
     * used revokes every session of that user, since it means the token was copied.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse refresh(String rawToken) {
        Instant now = clock.instant();
        RefreshToken token = refreshTokens.findByTokenHash(TokenService.hash(rawToken))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REFRESH_TOKEN, "Invalid refresh token"));
        User user = token.getUser();
        if (token.isRevoked()) {
            refreshTokens.revokeAllForUser(user.getId(), now);
            log.warn("Used refresh token presented, all sessions revoked: userId={}", user.getId());
            throw new ApiException(ErrorCode.REFRESH_TOKEN_REUSED, "Refresh token was already used; please sign in again");
        }
        if (token.isExpired(now) || !user.isEnabled()) {
            throw new ApiException(ErrorCode.SESSION_EXPIRED, "Session expired; please sign in again");
        }
        token.revoke(now);
        return issueTokens(user, profiles.findById(user.getId()).orElse(null), now);
    }

    @Transactional
    public void logout(String rawToken) {
        refreshTokens.findByTokenHash(TokenService.hash(rawToken)).ifPresent(t -> t.revoke(clock.instant()));
    }

    @Transactional(readOnly = true)
    public UserSummary currentUser(UUID userId) {
        User user = users.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND, "User not found"));
        return summarize(user, profiles.findById(userId).orElse(null));
    }

    private Optional<User> findByPhoneQuietly(String identifier) {
        try {
            return users.findByPhone(Identifiers.normalizePhone(identifier));
        } catch (ApiException invalidPhone) {
            return Optional.empty();
        }
    }

    private AuthResponse issueTokens(User user, Profile profile, Instant now) {
        String refreshToken = tokenService.generateRefreshToken();
        refreshTokens.save(new RefreshToken(user, TokenService.hash(refreshToken), tokenService.refreshTokenExpiry(now)));
        return new AuthResponse(
                tokenService.createAccessToken(user, now),
                refreshToken,
                "Bearer",
                tokenService.accessTokenTtlSeconds(),
                summarize(user, profile));
    }

    private static UserSummary summarize(User user, Profile profile) {
        return new UserSummary(
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                profile == null ? null : profile.getFullName(),
                user.getRoles().stream().map(Enum::name).sorted().toList(),
                profile != null && profile.isShopProfileComplete());
    }
}
