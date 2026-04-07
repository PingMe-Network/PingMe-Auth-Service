package org.ping_me.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin 1/10/2026
 *
 **/
@AllArgsConstructor
@NoArgsConstructor
@Data
public class MobileAuthResponse {

    private CurrentUserSessionResponse userSession;
    private String accessToken;
    private String refreshToken;

}
