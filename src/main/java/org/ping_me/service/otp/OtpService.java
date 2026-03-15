package org.ping_me.service.otp;

import org.ping_me.dto.request.mail.AuthOtpRequest;
import org.ping_me.dto.request.mail.OtpVerificationRequest;
import org.ping_me.dto.response.mail.GetOtpResponse;
import org.ping_me.dto.response.mail.OtpVerificationResponse;

/**
 * @author : user664dntp
 * @mailto : phatdang19052004@gmail.com
 * @created : 18/01/2026, Sunday
 **/
public interface OtpService {
    GetOtpResponse sendOtp(AuthOtpRequest request);

    OtpVerificationResponse verifyOtp(OtpVerificationRequest request);

    boolean checkAdminIsVerified();
}
