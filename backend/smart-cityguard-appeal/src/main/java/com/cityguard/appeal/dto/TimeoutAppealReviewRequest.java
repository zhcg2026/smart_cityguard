package com.cityguard.appeal.dto;

import lombok.Data;

@Data
public class TimeoutAppealReviewRequest {

    private Long appealId;
    /** true=通过 false=打回 */
    private Boolean approved;
    private String opinion;
}
