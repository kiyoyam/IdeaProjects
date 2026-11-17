package com.campus.errand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.errand.common.BusinessException;
import com.campus.errand.common.Constants;
import com.campus.errand.common.PageResult;
import com.campus.errand.dto.EvaluateDTO;
import com.campus.errand.entity.Evaluation;
import com.campus.errand.entity.Task;
import com.campus.errand.entity.User;
import com.campus.errand.mapper.EvaluationMapper;
import com.campus.errand.mapper.TaskMapper;
import com.campus.errand.service.EvaluationService;
import com.campus.errand.service.UserService;
import com.campus.errand.vo.EvaluationPendingVO;
import com.campus.errand.vo.EvaluationSummaryVO;
import com.campus.errand.vo.EvaluationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
        evaluation.setSpeedRating(dto.getSpeedRating());
        evaluation.setServiceRating(dto.getServiceRating());
        evaluation.setQualityRating(dto.getQualityRating());
        evaluation.setContent(dto.getContent());
        evaluation.setTags(joinTags(dto.getTags()));
        evaluation.setAnonymous(Boolean.TRUE.equals(dto.getAnonymous()) ? 1 : 0);
        evaluationMapper.insert(evaluation);
        // 更新被评人平均分（按总分计算）
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
        return toVOs(list, null);
    }

    @Override
    public PageResult<EvaluationPendingVO> pending(Long userId, Integer page, Integer size) {
        int current = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : size;
        // 我已评价过的任务
        Set<Long> evaluatedTaskIds = evaluationMapper.selectList(new LambdaQueryWrapper<Evaluation>()
                        .eq(Evaluation::getEvaluatorId, userId))
                .stream().map(Evaluation::getTaskId).collect(Collectors.toSet());
        // 我参与且已完成的任务
        List<Task> completed = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .eq(Task::getStatus, Constants.STATUS_COMPLETED)
                .and(w -> w.eq(Task::getPublisherId, userId).or().eq(Task::getRunnerId, userId))
                .orderByDesc(Task::getCompleteTime));
        List<Task> todo = completed.stream()
                .filter(t -> !evaluatedTaskIds.contains(t.getId()))
                .collect(Collectors.toList());

        int total = todo.size();
        int from = Math.min((current - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);
        List<Task> pageTasks = todo.subList(from, to);

        Set<Long> userIds = new HashSet<>();
        pageTasks.forEach(t -> {
            if (t.getPublisherId() != null) userIds.add(t.getPublisherId());
            if (t.getRunnerId() != null) userIds.add(t.getRunnerId());
        });
        Map<Long, User> userMap = userMap(userIds);

        List<EvaluationPendingVO> vos = new ArrayList<>();
        for (Task t : pageTasks) {
            boolean isPublisher = userId.equals(t.getPublisherId());
            Long counterpartId = isPublisher ? t.getRunnerId() : t.getPublisherId();
            EvaluationPendingVO vo = new EvaluationPendingVO();
            vo.setTaskId(t.getId());
            vo.setTitle(t.getTitle());
            vo.setCategory(t.getCategory());
            vo.setReward(t.getReward());
            vo.setRole(isPublisher ? "PUBLISHER" : "RUNNER");
            vo.setCompleteTime(t.getCompleteTime());
            User counterpart = counterpartId == null ? null : userMap.get(counterpartId);
            vo.setCounterpartName(counterpart == null ? null : displayName(counterpart));
            vos.add(vo);
        }
        return new PageResult<>((long) total, vos);
    }

    @Override
    public PageResult<EvaluationVO> received(Long userId, Integer page, Integer size) {
        Page<Evaluation> p = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        evaluationMapper.selectPage(p, new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getEvaluateeId, userId)
                .orderByDesc(Evaluation::getCreateTime));
        return new PageResult<>(p.getTotal(), toVOs(p.getRecords(), userId));
    }

    @Override
    public PageResult<EvaluationVO> sent(Long userId, Integer page, Integer size) {
        Page<Evaluation> p = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        evaluationMapper.selectPage(p, new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getEvaluatorId, userId)
                .orderByDesc(Evaluation::getCreateTime));
        return new PageResult<>(p.getTotal(), toVOs(p.getRecords(), userId));
    }

    @Override
    public EvaluationSummaryVO summary(Long userId) {
        List<Evaluation> list = evaluationMapper.selectList(new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getEvaluateeId, userId));
        EvaluationSummaryVO vo = new EvaluationSummaryVO();
        if (list.isEmpty()) {
            return vo;
        }
        vo.setTotalCount((long) list.size());
        vo.setAvgRating(avg(list.stream().map(Evaluation::getRating).collect(Collectors.toList())));
        vo.setSpeedAvg(avg(list.stream().map(Evaluation::getSpeedRating).collect(Collectors.toList())));
        vo.setServiceAvg(avg(list.stream().map(Evaluation::getServiceRating).collect(Collectors.toList())));
        vo.setQualityAvg(avg(list.stream().map(Evaluation::getQualityRating).collect(Collectors.toList())));
        long good = list.stream().filter(e -> e.getRating() != null && e.getRating() >= 4).count();
        vo.setGoodRate((int) Math.round(good * 100.0 / list.size()));
        // 标签统计
        Map<String, Long> tagCount = new HashMap<>();
        for (Evaluation e : list) {
            for (String tag : splitTags(e.getTags())) {
                tagCount.merge(tag, 1L, Long::sum);
            }
        }
        vo.setTags(tagCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(en -> new EvaluationSummaryVO.TagStat(en.getKey(), en.getValue()))
                .collect(Collectors.toList()));
        return vo;
    }

    // ==================== 私有方法 ====================

    private List<EvaluationVO> toVOs(List<Evaluation> list, Long viewerId) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> userIds = new HashSet<>();
        Set<Long> taskIds = new HashSet<>();
        list.forEach(e -> {
            userIds.add(e.getEvaluatorId());
            userIds.add(e.getEvaluateeId());
            taskIds.add(e.getTaskId());
        });
        Map<Long, User> userMap = userMap(userIds);
        Map<Long, Task> taskMap = taskMapper.selectBatchIds(taskIds).stream()
                .collect(Collectors.toMap(Task::getId, Function.identity()));
        List<EvaluationVO> vos = new ArrayList<>();
        for (Evaluation e : list) {
            EvaluationVO vo = buildVO(e, userMap, taskMap, viewerId);
            vos.add(vo);
        }
        return vos;
    }

    private EvaluationVO toVO(Evaluation e, Long evaluatorId, Long evaluateeId) {
        Map<Long, User> map = userService.listByIds(java.util.Arrays.asList(evaluatorId, evaluateeId)).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Task task = taskMapper.selectById(e.getTaskId());
        Map<Long, Task> taskMap = task == null ? Collections.emptyMap()
                : Collections.singletonMap(task.getId(), task);
        return buildVO(e, map, taskMap, evaluatorId);
    }

    private EvaluationVO buildVO(Evaluation e, Map<Long, User> userMap, Map<Long, Task> taskMap, Long viewerId) {
        EvaluationVO vo = new EvaluationVO();
        vo.setId(e.getId());
        vo.setTaskId(e.getTaskId());
        vo.setEvaluatorId(e.getEvaluatorId());
        vo.setEvaluateeId(e.getEvaluateeId());
        vo.setRating(e.getRating());
        vo.setSpeedRating(e.getSpeedRating());
        vo.setServiceRating(e.getServiceRating());
        vo.setQualityRating(e.getQualityRating());
        vo.setContent(e.getContent());
        vo.setTags(splitTags(e.getTags()));
        vo.setAnonymous(e.getAnonymous() != null && e.getAnonymous() == 1);
        vo.setCreateTime(e.getCreateTime());
        Task task = taskMap.get(e.getTaskId());
        if (task != null) {
            vo.setTaskTitle(task.getTitle());
        }
        boolean anonymous = Boolean.TRUE.equals(vo.getAnonymous());
        boolean self = viewerId != null && viewerId.equals(e.getEvaluatorId());
        User evaluator = userMap.get(e.getEvaluatorId());
        // 匿名评价对他人隐藏评价人，评价人自己仍可见
        if (anonymous && !self) {
            vo.setEvaluatorName("匿名用户");
        } else if (evaluator != null) {
            vo.setEvaluatorName(displayName(evaluator));
        }
        User evaluatee = userMap.get(e.getEvaluateeId());
        if (evaluatee != null) {
            vo.setEvaluateeName(displayName(evaluatee));
        }
        return vo;
    }

    private Map<Long, User> userMap(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return userService.listByIds(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private BigDecimal avg(List<Integer> values) {
        List<Integer> valid = values.stream().filter(v -> v != null && v > 0).collect(Collectors.toList());
        if (valid.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = valid.stream().map(BigDecimal::valueOf).reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(valid.size()), 1, RoundingMode.HALF_UP);
    }

    private String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        String joined = tags.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .limit(6)
                .collect(Collectors.joining(","));
        return joined.isEmpty() ? null : (joined.length() > 255 ? joined.substring(0, 255) : joined);
    }

    private List<String> splitTags(String tags) {
        if (!StringUtils.hasText(tags)) {
            return new ArrayList<>();
        }
        return java.util.Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private String displayName(User user) {
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }
}
