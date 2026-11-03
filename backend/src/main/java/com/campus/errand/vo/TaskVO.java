package com.campus.errand.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 任务视图对象
 */
@Data
public class TaskVO {

    private Long id;
    private Long publisherId;
    private String publisherName;
    private BigDecimal publisherRating;
    private Long runnerId;
    private String runnerName;
    private String title;
    private String description;
    private String category;
    private BigDecimal reward;
    private String pickupAddress;
    private String deliveryAddress;
    private LocalDateTime deadline;
    private String status;
    private String cancelReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime completeTime;
    /** 当前用户身份: PUBLISHER(发布者) / RUNNER(接单人) / NONE */
    private String myRole;
    /** 当前用户是否已评价（完成状态时展示评价入口用） */
    private Boolean evaluated = false;
}
