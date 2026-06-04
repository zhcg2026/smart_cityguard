package com.cityguard.common.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 兼容前端 Element Plus 日期值（yyyy-MM-dd HH:mm:ss）与 ISO-8601（yyyy-MM-dd'T'HH:mm:ss）
 */
public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final DateTimeFormatter[] FORMATTERS = {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
    };

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getValueAsString();
        if (text == null || text.isBlank()) {
            return null;
        }
        String s = text.trim();
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                if (formatter == FORMATTERS[FORMATTERS.length - 1]) {
                    return LocalDate.parse(s, formatter).atStartOfDay();
                }
                return LocalDateTime.parse(s, formatter);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        throw ctxt.weirdStringException(s, LocalDateTime.class,
                "无法解析日期时间，支持 yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd'T'HH:mm:ss");
    }
}
