package com.cityguard.timer.model;

import com.cityguard.timer.entity.WorkTimeConfig;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;
import java.util.Map;

@Getter
@Builder
public class TimerContext {

    private final LocalTime amStart;
    private final LocalTime amEnd;
    private final LocalTime pmStart;
    private final LocalTime pmEnd;
    /** key=LocalDate.toString(), value=holiday|workday */
    private final Map<String, String> holidayByDate;

    public static TimerContext from(WorkTimeConfig config, Map<String, String> holidayByDate) {
        return TimerContext.builder()
                .amStart(parseTime(config != null ? config.getAmStartTime() : null, "08:00"))
                .amEnd(parseTime(config != null ? config.getAmEndTime() : null, "12:00"))
                .pmStart(parseTime(config != null ? config.getPmStartTime() : null, "14:00"))
                .pmEnd(parseTime(config != null ? config.getPmEndTime() : null, "18:00"))
                .holidayByDate(holidayByDate != null ? holidayByDate : Map.of())
                .build();
    }

    private static LocalTime parseTime(String raw, String fallback) {
        String s = raw != null && !raw.isBlank() ? raw.trim() : fallback;
        return LocalTime.parse(s.length() == 5 ? s : fallback);
    }
}
