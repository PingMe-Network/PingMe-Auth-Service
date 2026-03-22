package org.ping_me.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Arrays;

/**
 * Admin 8/3/2025
 **/
@Configuration
public class SecurityConfiguration {
    private static final String[] WHITELIST = {
            // Authentication
            "/auth-service/auth/login",
            "/auth-service/auth/mobile/login",
            "/auth-service/auth/logout",
            "/auth-service/auth/register",
            "/auth-service/auth/refresh",
            "/auth-service/auth/admin/login",
            "/auth-service/auth/mobile/refresh",

            // Forget password
            "/auth-service/auth/forget-password",
            "/mail-management/api/v1/mails/send-otp",
            "/mail-management/api/v1/mails/otp-verification",

            // OTP
            "/auth-service/otp/send",
            "/auth-service/otp/verify",

            // API DOCS
            "/swagger-ui/**",
            "/v3/api-docs/**",

            // WebSocket
            // Bỏ qua kiểm tra tại lớp BearerTokenFilter
            // Kiểm tra tại lớp HandShakeInterceptor
            "/core-service/ws/**",

            // Health check - Kiểm tra nhịp tim
            "/actuator/health",
            "/actuator/health/**",

    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity httpSecurity,
            CustomAuthenticationEntryPoint customAuthenticationEntryPoint
    ) throws Exception {
        httpSecurity
                .cors(AbstractHttpConfigurer::disable)

                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(WHITELIST).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                );

        return httpSecurity.build();
    }
}
