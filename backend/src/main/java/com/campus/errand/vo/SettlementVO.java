package com.campus.errand.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 结算记录视图对象
 */
@Data
public class SettlementVO {

    private Long id;
    private Long taskId;
    private String title;
    private Long publisherId;
    private String publisherName;
    private Long runnerId;
    private String runnerName;
    private BigDecimal amount;
    /** TASK任务结算 / REFUND退款 */
    private String type;
    private String status;
    private String remark;
    private LocalDateTime createTime;
}
