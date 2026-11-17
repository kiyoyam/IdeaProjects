package com.campus.errand.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 个人评价汇总（评价中心顶部数据）
 */
@Data
public class EvaluationSummaryVO {

    /** 收到的评价总数 */
    private Long totalCount = 0L;
    /** 平均总分 */
    private BigDecimal avgRating = BigDecimal.ZERO;
    private BigDecimal speedAvg = BigDecimal.ZERO;
    private BigDecimal serviceAvg = BigDecimal.ZERO;
    private BigDecimal qualityAvg = BigDecimal.ZERO;
    /** 好评率（4-5 分占比，0-100） */
    private Integer goodRate = 0;
    /** 标签统计，按出现次数倒序 */
    private List<TagStat> tags = new ArrayList<>();

    @Data
    public static class TagStat {
        private String name;
        private Long count;

        public TagStat() {
        }

        public TagStat(String name, Long count) {
            this.name = name;
            this.count = count;
        }
    }
}
