package org.ping_me.service.auth;

import org.ping_me.dto.request.auth.DefaultLoginRequest;
import org.ping_me.dto.request.auth.MobileLoginRequest;
import org.ping_me.dto.request.auth.RegisterRequest;
import org.ping_me.dto.request.auth.SubmitSessionMetaRequest;
import org.ping_me.dto.response.auth.CurrentUserSessionResponse;
import org.ping_me.service.auth.model.AuthResultWrapper;
import org.springframework.http.ResponseCookie;

/**
 * Admin 8/4/2025
 **/
public interface AuthenticationService {
    CurrentUserSessionResponse register(
            RegisterRequest registerRequest);

    AuthResultWrapper defaultLogin(DefaultLoginRequest defaultLoginRequest);

    AuthResultWrapper mobileLogin(MobileLoginRequest mobileLoginRequest);

    ResponseCookie logout(String refreshToken);

    AuthResultWrapper refreshSession(String refreshToken, SubmitSessionMetaRequest submitSessionMetaRequest);

    AuthResultWrapper adminLogin(DefaultLoginRequest defaultLoginRequest);
}
