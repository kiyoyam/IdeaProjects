package com.campus.errand.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价视图对象
 */
@Data
public class EvaluationVO {

    private Long id;
    private Long taskId;
    private Long evaluatorId;
    private String evaluatorName;
    private Long evaluateeId;
    private String evaluateeName;
    private Integer rating;
    private String content;
    private LocalDateTime createTime;
}
