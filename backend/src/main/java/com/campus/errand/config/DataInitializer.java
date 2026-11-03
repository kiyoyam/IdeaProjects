package com.campus.errand.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.errand.entity.Task;
import com.campus.errand.entity.User;
import com.campus.errand.mapper.TaskMapper;
import com.campus.errand.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 数据初始化：首次启动时写入管理员/测试账号与示例任务
 * 测试账号：admin / stu001 / runner001，密码均为 123456
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final TaskMapper taskMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initUsers();
        initTasks();
    }

    private void initUsers() {
        Long count = userMapper.selectCount(null);
        if (count != null && count > 0) {
            return;
        }
        insertUser("admin", "管理员", "ADMIN", "0");
        insertUser("stu001", "小明同学", "STUDENT", "100");
        insertUser("runner001", "跑腿小张", "RUNNER", "100");
        log.info("初始化测试账号完成：admin / stu001 / runner001（密码均为 123456）");
    }

    private void initTasks() {
        Long count = taskMapper.selectCount(null);
        if (count != null && count > 0) {
            return;
        }
        User stu = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "stu001"));
        if (stu == null) {
            return;
        }
        insertTask(stu.getId(), "帮取快递（菜鸟驿站）", "帮我到菜鸟驿站取两个快递，送到5栋宿舍楼下，包裹不大。",
                "PICKUP", "5.00", "菜鸟驿站", "5栋宿舍楼下");
        insertTask(stu.getId(), "代买午饭（食堂二楼）", "帮忙在食堂二楼打一份红烧肉饭，送到图书馆一楼大厅，谢谢！",
                "BUY", "8.00", "食堂二楼", "图书馆一楼大厅");
        insertTask(stu.getId(), "送文件到行政楼", "把这份文件从实验楼101送到行政楼302办公室，需要签收。",
                "DELIVERY", "12.00", "实验楼101", "行政楼302");
        log.info("初始化示例任务完成");
    }

    private void insertUser(String username, String nickname, String role, String balance) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("123456"));
        user.setNickname(nickname);
        user.setRole(role);
        user.setBalance(new BigDecimal(balance));
        user.setFrozenBalance(BigDecimal.ZERO);
        user.setRating(new BigDecimal("5.00"));
        user.setRatingCount(0);
        user.setStatus(1);
        userMapper.insert(user);
    }

    private void insertTask(Long publisherId, String title, String description, String category,
                            String reward, String pickup, String delivery) {
        Task task = new Task();
        task.setPublisherId(publisherId);
        task.setTitle(title);
        task.setDescription(description);
        task.setCategory(category);
        task.setReward(new BigDecimal(reward));
        task.setPickupAddress(pickup);
        task.setDeliveryAddress(delivery);
        task.setStatus("PENDING");
        taskMapper.insert(task);
    }
}
