package com.campus.errand.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 发布任务参数
 */
@Data
public class TaskPublishDTO {

    @NotBlank(message = "任务标题不能为空")
    @Size(max = 100, message = "标题长度不能超过100")
    private String title;

    @Size(max = 1000, message = "描述长度不能超过1000")
    private String description;

    /** PICKUP取件/DELIVERY送件/BUY代买/HELP代办/OTHER其他 */
    private String category = "OTHER";

    @NotNull(message = "报酬金额不能为空")
    @DecimalMin(value = "0.01", message = "报酬金额必须大于0")
    private BigDecimal reward;

    @NotBlank(message = "取件地址不能为空")
    @Size(max = 200, message = "取件地址长度不能超过200")
    private String pickupAddress;

    @NotBlank(message = "送达地址不能为空")
    @Size(max = 200, message = "送达地址长度不能超过200")
    private String deliveryAddress;

    private LocalDateTime deadline;
}
