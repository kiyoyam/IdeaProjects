package com.campus.errand.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 接单记录视图对象
 */
@Data
public class OrderVO {

    private Long id;
    private Long taskId;
    private String title;
    private String category;
    private BigDecimal reward;
    private String status;
    private Long publisherId;
    private String publisherName;
    private String pickupAddress;
    private String deliveryAddress;
    private LocalDateTime deadline;
    private LocalDateTime createTime;
    private LocalDateTime completeTime;
}
