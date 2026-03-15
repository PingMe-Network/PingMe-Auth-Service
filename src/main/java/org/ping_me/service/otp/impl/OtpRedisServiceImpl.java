package org.ping_me.service.otp.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.ping_me.service.otp.OtpRedisService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @author : user664dntp
 * @mailto : phatdang19052004@gmail.com
 * @created : 15/01/2026, Thursday
 **/
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OtpRedisServiceImpl implements OtpRedisService {

    StringRedisTemplate otpRedisTemplate;

    @Override
    public void set(String key, String value, long timeout, TimeUnit timeUnit) {
        otpRedisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    @Override
    public String get(String key) {
        return otpRedisTemplate.opsForValue().get(key);
    }

    @Override
    public void delete(String key) {
        otpRedisTemplate.delete(key);
    }
}
