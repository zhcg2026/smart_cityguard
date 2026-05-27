package com.cityguard.caseinfo.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CaseDashboardTodosDto {

    private List<CaseDashboardTodoItemDto> items = new ArrayList<>();
}
