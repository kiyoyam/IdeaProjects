package com.campus.errand.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 评价参数
 */
@Data
public class EvaluateDTO {

    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最小为1")
    @Max(value = 5, message = "评分最大为5")
    private Integer rating;

    @Size(max = 500, message = "评价内容长度不能超过500")
    private String content;
}
