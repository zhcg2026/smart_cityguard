package com.cityguard.common.config;

import com.cityguard.common.jackson.FlexibleLocalDateTimeDeserializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * 请求体 LocalDateTime 兼容前端日期控件格式（yyyy-MM-dd HH:mm:ss）
 */
@Configuration
public class JacksonDateTimeConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer flexibleLocalDateTimeCustomizer() {
        return builder -> builder.deserializerByType(
                LocalDateTime.class, new FlexibleLocalDateTimeDeserializer());
    }
}
