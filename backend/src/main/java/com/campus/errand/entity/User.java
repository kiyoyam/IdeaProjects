package com.campus.errand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("`user`")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String phone;
    private String avatar;
    /** 角色: STUDENT / RUNNER / ADMIN */
    private String role;
    /** 可用余额 */
    private BigDecimal balance;
    /** 冻结余额 */
    private BigDecimal frozenBalance;
    /** 平均评分 */
    private BigDecimal rating;
    /** 被评次数 */
    private Integer ratingCount;
    /** 状态: 1正常 0禁用 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
