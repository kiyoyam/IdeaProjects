package com.campus.errand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.errand.common.BusinessException;
import com.campus.errand.common.Constants;
import com.campus.errand.dto.EvaluateDTO;
import com.campus.errand.entity.Evaluation;
import com.campus.errand.entity.Task;
import com.campus.errand.entity.User;
import com.campus.errand.mapper.EvaluationMapper;
import com.campus.errand.mapper.TaskMapper;
import com.campus.errand.service.EvaluationService;
import com.campus.errand.service.UserService;
import com.campus.errand.vo.EvaluationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 评价服务实现
 */
@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {

    private final EvaluationMapper evaluationMapper;
    private final TaskMapper taskMapper;
    private final UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EvaluationVO evaluate(Long userId, EvaluateDTO dto) {
        Task task = taskMapper.selectById(dto.getTaskId());
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
        if (!Constants.STATUS_COMPLETED.equals(task.getStatus())) {
            throw new BusinessException("任务完成后才能评价");
        }
        boolean isPublisher = task.getPublisherId().equals(userId);
        boolean isRunner = userId.equals(task.getRunnerId());
        if (!isPublisher && !isRunner) {
            throw new BusinessException("仅任务双方可评价");
        }
        Long evaluateeId = isPublisher ? task.getRunnerId() : task.getPublisherId();
        if (evaluateeId == null) {
            throw new BusinessException("对方信息缺失，无法评价");
        }
        Long count = evaluationMapper.selectCount(new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getTaskId, task.getId())
                .eq(Evaluation::getEvaluatorId, userId));
        if (count != null && count > 0) {
            throw new BusinessException("您已评价过该任务");
        }
        Evaluation evaluation = new Evaluation();
        evaluation.setTaskId(task.getId());
        evaluation.setEvaluatorId(userId);
        evaluation.setEvaluateeId(evaluateeId);
        evaluation.setRating(dto.getRating());
        evaluation.setContent(dto.getContent());
        evaluationMapper.insert(evaluation);
        // 更新被评人平均分
        User evaluatee = userService.getByIdRequired(evaluateeId);
        BigDecimal oldAvg = evaluatee.getRating() == null ? BigDecimal.ZERO : evaluatee.getRating();
        int oldCount = evaluatee.getRatingCount() == null ? 0 : evaluatee.getRatingCount();
        BigDecimal newAvg = oldAvg.multiply(BigDecimal.valueOf(oldCount))
                .add(BigDecimal.valueOf(dto.getRating()))
                .divide(BigDecimal.valueOf(oldCount + 1), 2, RoundingMode.HALF_UP);
        userService.update(new LambdaUpdateWrapper<User>()
                .eq(User::getId, evaluateeId)
                .set(User::getRating, newAvg)
                .set(User::getRatingCount, oldCount + 1));
        return toVO(evaluation, userId, evaluateeId);
    }

    @Override
    public List<EvaluationVO> listByTask(Long taskId) {
        List<Evaluation> list = evaluationMapper.selectList(new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getTaskId, taskId)
                .orderByDesc(Evaluation::getCreateTime));
        Set<Long> ids = new HashSet<>();
        list.forEach(e -> {
            ids.add(e.getEvaluatorId());
            ids.add(e.getEvaluateeId());
        });
        Map<Long, User> userMap = ids.isEmpty() ? Collections.emptyMap()
                : userService.listByIds(ids).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return list.stream().map(e -> toVO(e, userMap)).collect(Collectors.toList());
    }

    private EvaluationVO toVO(Evaluation e, Map<Long, User> userMap) {
        EvaluationVO vo = new EvaluationVO();
        vo.setId(e.getId());
        vo.setTaskId(e.getTaskId());
        vo.setEvaluatorId(e.getEvaluatorId());
        vo.setEvaluateeId(e.getEvaluateeId());
        vo.setRating(e.getRating());
        vo.setContent(e.getContent());
        vo.setCreateTime(e.getCreateTime());
        User evaluator = userMap.get(e.getEvaluatorId());
        if (evaluator != null) {
            vo.setEvaluatorName(displayName(evaluator));
        }
        User evaluatee = userMap.get(e.getEvaluateeId());
        if (evaluatee != null) {
            vo.setEvaluateeName(displayName(evaluatee));
        }
        return vo;
    }

    private EvaluationVO toVO(Evaluation e, Long evaluatorId, Long evaluateeId) {
        Map<Long, User> map = userService.listByIds(java.util.Arrays.asList(evaluatorId, evaluateeId)).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return toVO(e, map);
    }

    private String displayName(User user) {
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }
}
