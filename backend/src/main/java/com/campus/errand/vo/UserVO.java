package com.campus.errand.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户视图对象
 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String phone;
    private String avatar;
    private String role;
    private BigDecimal balance;
    private BigDecimal frozenBalance;
    private BigDecimal rating;
    private Integer ratingCount;
    private LocalDateTime createTime;
}
