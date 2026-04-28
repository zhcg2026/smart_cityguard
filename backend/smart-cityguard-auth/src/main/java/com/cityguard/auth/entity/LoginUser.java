package com.cityguard.auth.entity;

import lombok.Data;
import java.util.List;

@Data
public class LoginUser {
    private Long id;
    private String username;
    private String realName;
    private String avatar;
    private String phone;
    private String email;
    private Long departmentId;
    private String departmentName;
    private Long gridId;
    private String gridName;
    private List<String> roles;
    private List<String> permissions;
}