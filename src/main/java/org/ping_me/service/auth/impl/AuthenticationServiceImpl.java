package org.ping_me.service.auth.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.ping_me.client.TurnstileClient;
import org.ping_me.client.dto.TurnstileResponse;
import org.ping_me.config.auth.JwtBuilder;
import org.ping_me.dto.request.auth.DefaultLoginRequest;
import org.ping_me.dto.request.auth.MobileLoginRequest;
import org.ping_me.dto.request.auth.RegisterRequest;
import org.ping_me.dto.request.auth.SubmitSessionMetaRequest;
import org.ping_me.dto.request.mail.AuthOtpRequest;
import org.ping_me.dto.response.auth.CheckEmailResponse;
import org.ping_me.dto.response.auth.CurrentUserSessionResponse;
import org.ping_me.dto.event.UserAuditEvent;
import org.ping_me.dto.response.mail.GetOtpResponse;
import org.ping_me.model.Role;
import org.ping_me.model.User;
import org.ping_me.model.constant.AccountStatus;
import org.ping_me.model.constant.AuthOtpType;
import org.ping_me.model.constant.AuthProvider;
import org.ping_me.model.enums.AuditAction;
import org.ping_me.repository.jpa.RoleRepository;
import org.ping_me.repository.jpa.UserRepository;
import org.ping_me.service.auth.AuthenticationService;
import org.ping_me.service.auth.RefreshTokenRedisService;
import org.ping_me.service.auth.model.AuthResultWrapper;
import org.ping_me.service.otp.OtpService;
import org.ping_me.service.user.CurrentUserProvider;
import org.ping_me.utils.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseCookie;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * Admin 8/3/2025
 **/
@Service
@RequiredArgsConstructor
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {

    AuthenticationManager authenticationManager;
    PasswordEncoder passwordEncoder;

    JwtBuilder jwtService;
    RefreshTokenRedisService refreshTokenRedisService;

    UserMapper userMapper;

    UserRepository userRepository;

    RoleRepository roleRepository;

    OtpService otpService;

    CurrentUserProvider currentUserProvider;

    TurnstileClient turnstileClient;

    @Value("${cloudflare.turnstile.secret-key}")
    @NonFinal
    String secretKey;

    static String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

    @Value("${app.jwt.access-token-expiration}")
    @NonFinal
    Long accessTokenExpiration;

    @Value("${app.jwt.refresh-token-expiration}")
    @NonFinal
    Long refreshTokenExpiration;

    @Value("${cookie.sameSite}")
    @NonFinal
    String sameSite;

    @Value("${cookie.secure}")
    @NonFinal
    boolean secure;

    @NonFinal
    @Value("${spring.kafka.topic.user-create-dev}")
    String userCreateTopic;

    @Qualifier("kafkaObjectTemplate")
    KafkaTemplate<String, Object> kafkaObjectTemplate;

    @Override
    public CurrentUserSessionResponse register(
            RegisterRequest registerRequest,
            boolean needTurnstile
    ) {
        if (needTurnstile) validateTurnstile(registerRequest.getTurnstileToken());

        Role role = roleRepository.findById(2L).orElse(null);
        var user = User
                .builder()
                .email(registerRequest.getEmail())
                .name(registerRequest.getName())
                .gender(registerRequest.getGender())
                .address(registerRequest.getAddress())
                .dob(registerRequest.getDob())
                .role(role)
                .build();

        if (userRepository.existsByEmail(registerRequest.getEmail()))
            throw new DataIntegrityViolationException("Email đã tồn tại");

        user.setAuthProvider(AuthProvider.LOCAL);
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setAccountStatus(AccountStatus.PENDING_VERIFICATION);
        var savedUser = userRepository.save(user);

        publishUserAudit(savedUser, AuditAction.CREATE);

        otpService.sendOtp(new AuthOtpRequest(user.getEmail(), AuthOtpType.ACCOUNT_ACTIVATION, registerRequest.getTurnstileToken()));

        return userMapper.mapToCurrentUserSessionResponse(savedUser);
    }

    @Override
    public AuthResultWrapper defaultLogin(DefaultLoginRequest defaultLoginRequest) {
        validateTurnstile(defaultLoginRequest.getTurnstileToken());

        var authenticationToken = new UsernamePasswordAuthenticationToken(
                defaultLoginRequest.getEmail(),
                defaultLoginRequest.getPassword()
        );
        var authentication = authenticationManager.authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User currentUser = currentUserProvider.get();
        if (currentUser.getAccountStatus() == AccountStatus.PENDING_VERIFICATION) {
            otpService.sendOtp(new AuthOtpRequest(currentUser.getEmail(), AuthOtpType.ACCOUNT_ACTIVATION, defaultLoginRequest.getTurnstileToken()));
            throw new DisabledException("REQUIRE_ACTIVATION");
        }

        return buildAuthResultWrapper(currentUser, defaultLoginRequest.getSubmitSessionMetaRequest());
    }

    @Override
    public AuthResultWrapper mobileLogin(MobileLoginRequest mobileLoginRequest) {
        var authenticationToken = new UsernamePasswordAuthenticationToken(
                mobileLoginRequest.getEmail(),
                mobileLoginRequest.getPassword()
        );

        var authentication = authenticationManager.authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User currentUser = currentUserProvider.get();
        if (currentUser.getAccountStatus() == AccountStatus.PENDING_VERIFICATION) {
            otpService.sendOtp(new AuthOtpRequest(currentUser.getEmail(), AuthOtpType.ACCOUNT_ACTIVATION, null));
            System.out.println("User AccountStatus: "+ currentUser.getAccountStatus());
            throw new DisabledException("REQUIRE_ACTIVATION");
        }

        return buildAuthResultWrapper(currentUserProvider.get(), mobileLoginRequest.getSubmitSessionMetaRequest());
    }

    @Override
    public ResponseCookie logout(String refreshToken) {
        if (refreshToken != null) {
            String email = jwtService.decodeJwt(refreshToken).getSubject();
            var refreshTokenUser = userRepository
                    .getUserByEmail(email)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));

            refreshTokenRedisService.deleteRefreshToken(refreshToken, refreshTokenUser.getId().toString());
        }

        return ResponseCookie
                .from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .path("/")
                .sameSite(sameSite)
                .secure(secure)
                .maxAge(0)
                .build();
    }

    @Override
    public AuthResultWrapper refreshSession(
            String refreshToken, SubmitSessionMetaRequest submitSessionMetaRequest
    ) {
        String email = jwtService.decodeJwt(refreshToken).getSubject();
        var refreshTokenUser = userRepository
                .getUserByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));

        if (!refreshTokenRedisService.validateToken(refreshToken, refreshTokenUser.getId().toString()))
            throw new AccessDeniedException("Không có quyền truy cập");

        refreshTokenRedisService.deleteRefreshToken(refreshToken, refreshTokenUser.getId().toString());

        return buildAuthResultWrapper(refreshTokenUser, submitSessionMetaRequest);
    }

    @Override
    public AuthResultWrapper adminLogin(DefaultLoginRequest defaultLoginRequest) {
        String email = defaultLoginRequest.getEmail();
        User user = userRepository.findByEmail(email);

        if (user == null) throw new NullPointerException("Không tìm thấy người dùng với email: " + email);

        if (!passwordEncoder.matches(defaultLoginRequest.getPassword(), user.getPassword()))
            throw new IllegalArgumentException("Mật khẩu không đúng");

        if (user.getRole() == null || !user.getRole().getName().equals("ADMIN"))
            throw new AccessDeniedException("Người dùng không có quyền truy cập");

        return buildAuthResultWrapper(user, defaultLoginRequest.getSubmitSessionMetaRequest(), 600L);
    }

    @Override
    public CheckEmailResponse checkEmail(String email) {
        return new CheckEmailResponse(userRepository.existsByEmail(email));
    }

    // =====================================
    // Utilities methods
    // =====================================
    public void validateTurnstile(String token) {
        TurnstileResponse response = turnstileClient
                .verifyToken(secretKey, token);

        if (!response.success()) {
            String errors = String.join(",", response.errorCodes());
            throw new AccessDeniedException(errors);
        }
    }

    private AuthResultWrapper buildAuthResultWrapper(
            User user,
            SubmitSessionMetaRequest submitSessionMetaRequest
    ) {
        return buildAuthResultWrapper(user, submitSessionMetaRequest, accessTokenExpiration);
    }

    private AuthResultWrapper buildAuthResultWrapper(
            User user,
            SubmitSessionMetaRequest submitSessionMetaRequest,
            Long accessTokenTtl
    ) {
        // ================================================
        // CREATE TOKEN
        // ================================================
        var accessToken = jwtService.buildJwt(user, accessTokenTtl);
        var refreshToken = jwtService.buildJwt(user, refreshTokenExpiration);

        // ================================================
        // HANDLE WHITELIST REFRESH TOKEN VIA REDIS
        // ================================================
        refreshTokenRedisService.saveRefreshToken(
                refreshToken,
                user.getId().toString(),
                submitSessionMetaRequest,
                Duration.ofSeconds(refreshTokenExpiration)
        );


        var refreshTokenCookie = ResponseCookie
                .from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .path("/")
                .sameSite(sameSite)
                .secure(secure)
                .maxAge(refreshTokenExpiration)
                .build();

        return new AuthResultWrapper(
                userMapper.mapToCurrentUserSessionResponse(user),
                accessToken,
                refreshTokenCookie
        );
    }

    // warning action kệ đi - để sau này tiện update mấy cái liên quan tới delete hoặc update user
    private void publishUserAudit(User targetUser, AuditAction action) {
        if (targetUser == null || targetUser.getId() == null) return;

        try {
            String actionEmail = targetUser.getEmail();
            String actionName = targetUser.getName();

            try {
                User currentUser = currentUserProvider.get();
                if (currentUser != null) {
                    actionEmail = currentUser.getEmail();
                    actionName = currentUser.getName();
                }
            } catch (Exception e) {
                log.debug("Không có session người dùng hiện tại, sử dụng thông tin targetUser.");
            }

            UserAuditEvent event = new UserAuditEvent(
                    targetUser.getId(),
                    actionEmail,
                    actionName,
                    action,
                    System.currentTimeMillis()
            );

            kafkaObjectTemplate.send(userCreateTopic, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Kafka: Đã gửi event {} cho user {}", action, targetUser.getEmail());
                        } else {
                            log.error("Kafka: Gửi event thất bại: {}", ex.getMessage());
                        }
                    });

        } catch (Exception ex) {
            log.error("Lỗi nghiêm trọng khi chuẩn bị gửi Kafka event: {}", ex.getMessage());
        }
    }
}
