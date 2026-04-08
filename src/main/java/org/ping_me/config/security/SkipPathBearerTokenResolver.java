package org.ping_me.config.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Admin 8/9/2025
 **/
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SkipPathBearerTokenResolver implements BearerTokenResolver {

    BearerTokenResolver delegate = new DefaultBearerTokenResolver();
    static final List<String> SKIP_PATHS = List.of(
            "/auth-service/auth/login",
            "/auth-service/auth/mobile/login",
            "/auth-service/auth/admin/login",
            "/auth-service/auth/logout",
            "/auth-service/auth/register",
            "/auth-service/auth/check-email",
            "/auth-service/auth/refresh",
            "/auth-service/auth/mobile/refresh",
            "/auth-service/auth/reset-password",
            "/auth-service/auth/forget-password",
            "/auth-service/otp/send",
            "/auth-service/otp/verify"
    );

    @Override
    public String resolve(HttpServletRequest request) {
        String path = request.getRequestURI();

        for (String skip : SKIP_PATHS) {
            if (path.contains(skip)) {
                return null;
            }
        }

        return delegate.resolve(request);
    }

}
