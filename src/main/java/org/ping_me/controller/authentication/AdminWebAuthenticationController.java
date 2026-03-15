package org.ping_me.controller.authentication;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.ping_me.dto.base.ApiResponse;
import org.ping_me.dto.request.authentication.DefaultLoginRequest;
import org.ping_me.dto.response.authentication.AdminLoginResponse;
import org.ping_me.service.authentication.AuthenticationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : user664dntp
 * @mailto : phatdang19052004@gmail.com
 * @created : 5/02/2026, Thursday
 **/

@RestController
@RequestMapping("/auth-service/auth/admin")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class AdminWebAuthenticationController {
    AuthenticationService authenticationService;

    @PostMapping("/login")
    ResponseEntity<ApiResponse<AdminLoginResponse>> login(@Valid @RequestBody DefaultLoginRequest request) {
        var authResultWrapper = authenticationService.adminLogin(request);
        var payload = AdminLoginResponse.builder()
                .accessToken(authResultWrapper.getAccessToken())
                .email(authResultWrapper.getUserSession().getEmail())
                .isAdminAccount(true)
                .userSession(authResultWrapper.getUserSession())
                .build();

        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, authResultWrapper.getRefreshTokenCookie().toString())
                .body(new ApiResponse<>(payload));
    }
}
