package com.campus.errand.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价视图对象
 */
@Data
public class EvaluationVO {

    private Long id;
    private Long taskId;
    private String taskTitle;
    private Long evaluatorId;
    private String evaluatorName;
    private Long evaluateeId;
    private String evaluateeName;
    /** 总评分 1-5 */
    private Integer rating;
    private Integer speedRating;
    private Integer serviceRating;
    private Integer qualityRating;
    private String content;
    private List<String> tags;
    private Boolean anonymous;
    private LocalDateTime createTime;
}
