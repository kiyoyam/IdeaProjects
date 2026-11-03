package com.campus.errand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 结算记录实体
 */
@Data
@TableName("settlement")
public class Settlement {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long orderId;
    private Long publisherId;
    private Long runnerId;
    private BigDecimal amount;
    /** 类型: TASK任务结算 / REFUND退款 */
    private String type;
    private String status;
    private String remark;
    private LocalDateTime createTime;
}
