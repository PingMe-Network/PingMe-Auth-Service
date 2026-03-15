package org.ping_me.config.redis;

import org.ping_me.model.common.DeviceMeta;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class RedisTemplateConfig {

    // =========================================================
    // RedisTemplate cho caching phiên đăng nhập
    // =========================================================
    @Bean(name = "redisDeviceMetaTemplate")
    public RedisTemplate<String, DeviceMeta> redisDeviceMetaTemplate(
            RedisConnectionFactory cf,
            ObjectMapper om
    ) {
        RedisTemplate<String, DeviceMeta> tpl = new RedisTemplate<>();
        tpl.setConnectionFactory(cf);

        var keySer = new StringRedisSerializer();
        var valSer = new JacksonJsonRedisSerializer<>(om, DeviceMeta.class);

        tpl.setKeySerializer(keySer);
        tpl.setHashKeySerializer(keySer);
        tpl.setValueSerializer(valSer);
        tpl.setHashValueSerializer(valSer);

        tpl.afterPropertiesSet();
        return tpl;
    }
}
