package cn.hexinfo.arch_ai_review.controller;

import cn.hexinfo.arch_ai_review.dto.ApiResponse;
import cn.hexinfo.arch_ai_review.dto.LoginRequestDto;
import cn.hexinfo.arch_ai_review.dto.LoginResponseDto;
import cn.hexinfo.arch_ai_review.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @PostMapping("/login")
    public ApiResponse<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto loginRequest) {
        LoginResponseDto loginResponse = authService.login(loginRequest);
        return ApiResponse.success("登录成功", loginResponse);
    }
    
    @PostMapping("/refresh")
    public ApiResponse<LoginResponseDto> refreshToken(@RequestParam String refreshToken) {
        LoginResponseDto loginResponse = authService.refreshToken(refreshToken);
        return ApiResponse.success("刷新令牌成功", loginResponse);
    }
}
