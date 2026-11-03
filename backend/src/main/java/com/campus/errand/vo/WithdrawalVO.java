package com.campus.errand.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提现记录视图对象
 */
@Data
public class WithdrawalVO {

    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private BigDecimal amount;
    /** PENDING/APPROVED/REJECTED */
    private String status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime handleTime;
}
