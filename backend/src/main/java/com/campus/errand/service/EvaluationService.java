package com.campus.errand.service;

import com.campus.errand.dto.EvaluateDTO;
import com.campus.errand.vo.EvaluationVO;

import java.util.List;

/**
 * 评价服务
 */
public interface EvaluationService {

    EvaluationVO evaluate(Long userId, EvaluateDTO dto);

    List<EvaluationVO> listByTask(Long taskId);
}
