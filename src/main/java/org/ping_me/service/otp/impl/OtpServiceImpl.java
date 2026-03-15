package org.ping_me.service.otp.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.ping_me.config.auth.JwtBuilder;
import org.ping_me.dto.request.mail.AuthOtpRequest;
import org.ping_me.dto.request.mail.OtpVerificationRequest;
import org.ping_me.dto.request.mail.SendOtpRequest;
import org.ping_me.dto.response.mail.GetOtpResponse;
import org.ping_me.dto.response.mail.OtpVerificationResponse;
import org.ping_me.model.User;
import org.ping_me.model.constant.AccountStatus;
import org.ping_me.model.constant.AuthOtpType;
import org.ping_me.repository.jpa.UserRepository;
import org.ping_me.service.otp.OtpService;
import org.ping_me.service.otp.OtpRedisService;
import org.ping_me.service.user.CurrentUserProvider;
import org.ping_me.utils.OtpGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class OtpServiceImpl implements OtpService {

    // Service
    OtpRedisService otpRedisService;
    JwtBuilder jwtService;

    SendAsyncService sendAsyncService;

    // Repository
    UserRepository userRepository;

    // Provider
    CurrentUserProvider currentUserProvider;

    static String OTP_PREFIX = "OTP:";
    static String ADMIN_VERIFIED_PREFIX = "VERIFIED_ADMIN:";

    @NonFinal
    @Value("${spring.mail.timeout}")
    long timeout;

    @NonFinal
    @Value("${spring.mail.default-otp}")
    String defaultOtp;

    @Override
    public GetOtpResponse sendOtp(AuthOtpRequest request) {
        String email = request.getEmail();
        String otp = OtpGenerator.generateOtp(6);

        User user = userRepository.findByEmail(email);
        if (user == null) throw new EntityNotFoundException("User not found with email: " + email);

        if (request.getAuthOtpType() != AuthOtpType.ACCOUNT_ACTIVATION &&
                user.getAccountStatus() == AccountStatus.NON_ACTIVATED)
            throw new IllegalArgumentException("Please active your email to do this action: " + email);

        otpRedisService.set(OTP_PREFIX + email, otp, timeout, TimeUnit.MINUTES);

        sendAsyncService.sendOtpAsync(
                SendOtpRequest.builder()
                        .toMail(email)
                        .otp(otp)
                        .authOtpType(request.getAuthOtpType())
                        .build()
        );

        return GetOtpResponse.builder()
                .otp(otp)
                .mailRecipient(email)
                .isSent(true)
                .build();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public GetOtpResponse sendAdminOtp() {
        User currentUser = currentUserProvider.get();

        return sendOtp(AuthOtpRequest.builder()
                .email(currentUser.getEmail())
                .authOtpType(AuthOtpType.ADMIN_VERIFICATION)
                .build());
    }

    @Override
    public OtpVerificationResponse verifyOtp(OtpVerificationRequest request) {
        // default otp for testing purpose
        if (request.getOtp().equalsIgnoreCase(defaultOtp))
            return OtpVerificationResponse.builder()
                    .isValid(true)
                    .resetPasswordToken(executePostVerificationLogic(request.getMailRecipient(), request.getAuthOtpType())
                            .orElse(null))
                    .build();

        String email = request.getMailRecipient();
        String storedOtp = otpRedisService.get(OTP_PREFIX + email);

        if (storedOtp == null)
            throw new IllegalArgumentException("OTP has expired or does not exist.");

        if (!storedOtp.equalsIgnoreCase(request.getOtp()))
            return OtpVerificationResponse.builder().isValid(false).build();

        // Xóa OTP ngay sau khi xác thực đúng (Tránh brute force)
        otpRedisService.delete(OTP_PREFIX + email);

        // Thực hiện các logic nghiệp vụ sau xác thực (Lưu trạng thái admin, tạo token...)
        String resetToken = executePostVerificationLogic(email, request.getAuthOtpType())
                .orElse(null);

        return OtpVerificationResponse.builder()
                .isValid(true)
                .resetPasswordToken(resetToken)
                .build();
    }

    @Override
    public boolean checkAdminIsVerified() {
        User currentUser = currentUserProvider.get();
        String storedVerifiedProof = otpRedisService.get(ADMIN_VERIFIED_PREFIX + currentUser.getEmail());
        return storedVerifiedProof != null;
    }

    // ========================================================================
    // UTILS
    // ========================================================================
    private Optional<String> executePostVerificationLogic(String email, AuthOtpType type) {
        return switch (type) {
            case ADMIN_VERIFICATION -> {
                otpRedisService.set(ADMIN_VERIFIED_PREFIX + email, "VERIFIED", 24, TimeUnit.HOURS);
                yield Optional.empty();
            }

            case USER_FORGET_PASSWORD -> {
                User user = userRepository.findByEmail(email);
                if (user == null) throw new EntityNotFoundException("User not found: " + email);

                yield Optional.ofNullable(jwtService.buildJwt(user, 600L));
            }

            default -> Optional.empty();
        };
    }
}
