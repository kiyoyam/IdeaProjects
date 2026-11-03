package com.campus.errand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 任务实体
 */
@Data
@TableName("task")
public class Task {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long publisherId;
    private Long runnerId;
    private String title;
    private String description;
    /** 分类: PICKUP取件 / DELIVERY送件 / BUY代买 / HELP代办 / OTHER其他 */
    private String category;
    private BigDecimal reward;
    private String pickupAddress;
    private String deliveryAddress;
    private LocalDateTime deadline;
    /** 状态: PENDING/ACCEPTED/IN_PROGRESS/FINISHED/COMPLETED/CANCELLED */
    private String status;
    private String cancelReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime completeTime;
}
