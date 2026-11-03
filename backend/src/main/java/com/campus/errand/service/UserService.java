package com.campus.errand.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.errand.common.PageResult;
import com.campus.errand.dto.LoginDTO;
import com.campus.errand.dto.RechargeDTO;
import com.campus.errand.dto.RegisterDTO;
import com.campus.errand.entity.User;
import com.campus.errand.vo.LoginVO;
import com.campus.errand.vo.UserVO;

/**
 * 用户服务
 */
public interface UserService extends IService<User> {

    LoginVO register(RegisterDTO dto);

    LoginVO login(LoginDTO dto);

    UserVO currentUser();

    UserVO recharge(Long userId, RechargeDTO dto);

    /** 查询用户，不存在则抛业务异常 */
    User getByIdRequired(Long id);

    UserVO toVO(User user);

    PageResult<UserVO> pageUsers(Integer page, Integer size, String keyword);
}
