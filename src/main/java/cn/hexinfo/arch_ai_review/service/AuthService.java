package cn.hexinfo.arch_ai_review.service;

import cn.hexinfo.arch_ai_review.dto.LoginRequestDto;
import cn.hexinfo.arch_ai_review.dto.LoginResponseDto;
import cn.hexinfo.arch_ai_review.entity.User;

public interface AuthService {
    
    /**
     * 用户登录
     * 
     * @param loginRequest 登录请求
     * @return 登录响应
     */
    LoginResponseDto login(LoginRequestDto loginRequest);
    
    /**
     * 刷新令牌
     * 
     * @param refreshToken 刷新令牌
     * @return 登录响应
     */
    LoginResponseDto refreshToken(String refreshToken);
    
    /**
     * 获取当前登录用户
     * 
     * @return 用户实体
     */
    User getCurrentUser();
    
    /**
     * 根据用户名获取用户
     * 
     * @param username 用户名
     * @return 用户实体
     */
    User getUserByUsername(String username);
}
