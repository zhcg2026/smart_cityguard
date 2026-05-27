package com.cityguard.config.dto;

import lombok.Data;

@Data
public class CategorySmallSaveRequest {

    private Long id;

    private Long bigId;

    private String smallCode;

    private String smallName;

    private String responsibilitySubject;

    private String superviseSubject;

    private String legalBasis;

    private String collectRequirement;

    private String description;

    private Integer sortOrder;

    private Integer status;
}
