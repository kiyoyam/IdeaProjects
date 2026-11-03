package com.campus.errand.dto;

import lombok.Data;

/**
 * 任务分页查询参数
 */
@Data
public class TaskQueryDTO {

    private Integer page = 1;
    private Integer size = 10;
    /** 关键词（标题/描述模糊匹配） */
    private String keyword;
    private String category;
    private String status;
}
