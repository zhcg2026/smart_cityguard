package com.cityguard.common.enums;

import lombok.Getter;

@Getter
public enum CaseStatusEnum {
    PENDING_VERIFY(0, "待核查"),
    PENDING_REGISTER(1, "待立案"),
    PENDING_DISPATCH(2, "待派遣"),
    PENDING_HANDLE(3, "待处置"),
    PENDING_CHECK(4, "待核实"),
    CLOSED(5, "已结案"),
    REJECTED(6, "不受理"),
    HANDLING(7, "处置中"),
    RETURNED(8, "回退");

    private final Integer code;
    private final String name;

    CaseStatusEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static CaseStatusEnum fromCode(Integer code) {
        for (CaseStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}