package com.campus.errand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价实体
 * <p>
 * 总评分 rating 会参与用户信用分计算；三个维度分与标签用于展示与统计。
 */
@Data
@TableName("evaluation")
public class Evaluation {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long evaluatorId;
    private Long evaluateeId;
    /** 总评分 1-5 */
    private Integer rating;
    /** 配送速度 1-5 */
    private Integer speedRating;
    /** 服务态度 1-5 */
    private Integer serviceRating;
    /** 完成质量 1-5 */
    private Integer qualityRating;
    private String content;
    /** 快捷标签，逗号分隔 */
    private String tags;
    /** 是否匿名 1是 0否 */
    private Integer anonymous;
    private LocalDateTime createTime;
}
