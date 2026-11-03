package com.campus.errand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 接单记录实体
 */
@Data
@TableName("errand_order")
public class ErrandOrder {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long publisherId;
    private Long runnerId;
    private BigDecimal reward;
    /** 状态: ACCEPTED/IN_PROGRESS/FINISHED/COMPLETED/CANCELLED */
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime completeTime;
}
