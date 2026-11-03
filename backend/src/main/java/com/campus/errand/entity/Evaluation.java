package com.campus.errand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价实体
 */
@Data
@TableName("evaluation")
public class Evaluation {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long evaluatorId;
    private Long evaluateeId;
    /** 评分 1-5 */
    private Integer rating;
    private String content;
    private LocalDateTime createTime;
}
