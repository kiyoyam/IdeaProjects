package com.campus.errand.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 管理员驳回提现参数
 */
@Data
public class RejectDTO {

    @Size(max = 255, message = "驳回原因长度不能超过255")
    private String remark;
}
