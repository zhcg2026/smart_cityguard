package com.cityguard.timer.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResolvedTimeLimit {

    private String timeLimitType;
    private int timeLimitValue;
    private boolean urgent;
}
