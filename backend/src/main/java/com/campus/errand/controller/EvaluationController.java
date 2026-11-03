package com.campus.errand.controller;

import com.campus.errand.common.Result;
import com.campus.errand.dto.EvaluateDTO;
import com.campus.errand.security.UserContext;
import com.campus.errand.service.EvaluationService;
import com.campus.errand.vo.EvaluationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 评价接口
 */
@Tag(name = "评价模块")
@RestController
@RequestMapping("/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @Operation(summary = "对已完成任务进行评价（双方互评，每人每任务一次）")
    @PostMapping
    public Result<EvaluationVO> evaluate(@Validated @RequestBody EvaluateDTO dto) {
        return Result.ok(evaluationService.evaluate(UserContext.userId(), dto));
    }

    @Operation(summary = "查询某任务的评价列表")
    @GetMapping("/task/{taskId}")
    public Result<List<EvaluationVO>> listByTask(@PathVariable Long taskId) {
        return Result.ok(evaluationService.listByTask(taskId));
    }
}
