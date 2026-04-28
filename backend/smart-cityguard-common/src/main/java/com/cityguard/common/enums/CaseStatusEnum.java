package com.cityguard.common.enums;

import lombok.Getter;

@Getter
public enum CaseStatusEnum {
    REPORTED("reported", "上报"),
    PENDING_ACCEPT("pending_accept", "待立案"),
    PENDING_VERIFY("pending_verify", "待核实"),
    ACCEPTED("accepted", "立案"),
    PENDING_DISPATCH("pending_dispatch", "待派遣"),
    NOT_ACCEPTED("not_accepted", "不立案"),
    DISPATCHED("dispatched", "已派遣"),
    PENDING_HANDLE("pending_handle", "待处置"),
    RETURNED("returned", "回退"),
    HANDLING("handling", "处置中"),
    HANDLE_FINISH("handle_finish", "处置完成"),
    PENDING_CHECK("pending_check", "待核查"),
    CHECKING("checking", "核查中"),
    CHECK_PASS("check_pass", "核查通过"),
    CHECK_NOT_PASS("check_not_pass", "核查不通过"),
    PENDING_CLOSE("pending_close", "待结案"),
    CLOSED("closed", "结案"),
    FORCED_CLOSE("forced_close", "强制结案"),
    CANCELLED("cancelled", "作废");

    private final String code;
    private final String name;

    CaseStatusEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static CaseStatusEnum fromCode(String code) {
        for (CaseStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}