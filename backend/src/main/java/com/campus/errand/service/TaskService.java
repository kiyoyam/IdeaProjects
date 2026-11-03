package com.campus.errand.service;

import com.campus.errand.common.PageResult;
import com.campus.errand.dto.TaskPublishDTO;
import com.campus.errand.dto.TaskQueryDTO;
import com.campus.errand.vo.OrderVO;
import com.campus.errand.vo.TaskVO;

/**
 * 任务服务
 */
public interface TaskService {

    TaskVO publish(TaskPublishDTO dto, Long userId);

    PageResult<TaskVO> pageQuery(TaskQueryDTO dto, Long userId);

    TaskVO detail(Long taskId, Long userId);

    /** 跑腿员接单 */
    void accept(Long taskId, Long userId);

    /** 跑腿员开始配送 */
    void start(Long taskId, Long userId);

    /** 跑腿员标记送达，等待发布者确认 */
    void finish(Long taskId, Long userId);

    /** 发布者确认完成，触发结算 */
    void confirm(Long taskId, Long userId);

    /** 取消任务（发布者/跑腿员），触发退款 */
    void cancel(Long taskId, Long userId, String reason);

    PageResult<TaskVO> myPublished(Long userId, Integer page, Integer size);

    PageResult<OrderVO> myAccepted(Long userId, Integer page, Integer size);
}
