package com.campus.errand.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 取消任务参数
 */
@Data
public class CancelDTO {

    @Size(max = 255, message = "取消原因长度不能超过255")
    private String reason;
}
