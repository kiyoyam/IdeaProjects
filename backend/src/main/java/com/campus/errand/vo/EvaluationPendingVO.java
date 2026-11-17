package com.campus.errand.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 待评价订单视图对象（评价中心「待评价」列表）
 */
@Data
public class EvaluationPendingVO {

    private Long taskId;
    private String title;
    private String category;
    private BigDecimal reward;
    /** 我在该任务中的身份：PUBLISHER 待评价跑腿员 / RUNNER 待评价发布者 */
    private String role;
    /** 对方昵称 */
    private String counterpartName;
    private LocalDateTime completeTime;
}
