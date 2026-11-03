package com.campus.errand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提现申请实体
 */
@Data
@TableName("withdrawal")
public class Withdrawal {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private BigDecimal amount;
    /** 状态: PENDING/APPROVED/REJECTED */
    private String status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime handleTime;
}
