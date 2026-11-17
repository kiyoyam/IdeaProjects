package com.campus.errand.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 评价参数（评分模板）
 */
@Data
public class EvaluateDTO {

    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @NotNull(message = "总评分不能为空")
    @Min(value = 1, message = "评分最小为1")
    @Max(value = 5, message = "评分最大为5")
    private Integer rating;

    /** 配送速度 1-5，可不填 */
    @Min(value = 1, message = "评分最小为1")
    @Max(value = 5, message = "评分最大为5")
    private Integer speedRating;

    /** 服务态度 1-5，可不填 */
    @Min(value = 1, message = "评分最小为1")
    @Max(value = 5, message = "评分最大为5")
    private Integer serviceRating;

    /** 完成质量 1-5，可不填 */
    @Min(value = 1, message = "评分最小为1")
    @Max(value = 5, message = "评分最大为5")
    private Integer qualityRating;

    @Size(max = 500, message = "评价内容长度不能超过500")
    private String content;

    /** 快捷标签，最多 6 个 */
    @Size(max = 6, message = "最多选择6个标签")
    private List<String> tags;

    /** 是否匿名评价 */
    private Boolean anonymous;
}
