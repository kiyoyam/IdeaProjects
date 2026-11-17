package com.campus.errand.service;

import com.campus.errand.common.PageResult;
import com.campus.errand.dto.EvaluateDTO;
import com.campus.errand.vo.EvaluationPendingVO;
import com.campus.errand.vo.EvaluationSummaryVO;
import com.campus.errand.vo.EvaluationVO;

import java.util.List;

/**
 * 评价服务
 */
public interface EvaluationService {

    EvaluationVO evaluate(Long userId, EvaluateDTO dto);

    List<EvaluationVO> listByTask(Long taskId);

    /** 待评价订单：我参与且已完成、但我还没评价的任务 */
    PageResult<EvaluationPendingVO> pending(Long userId, Integer page, Integer size);

    /** 我收到的评价 */
    PageResult<EvaluationVO> received(Long userId, Integer page, Integer size);

    /** 我发出的评价 */
    PageResult<EvaluationVO> sent(Long userId, Integer page, Integer size);

    /** 某用户的评价汇总 */
    EvaluationSummaryVO summary(Long userId);
}
