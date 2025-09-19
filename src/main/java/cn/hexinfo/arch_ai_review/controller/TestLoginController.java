package cn.hexinfo.arch_ai_review.controller;

import cn.hexinfo.arch_ai_review.dto.ApiResponse;
import cn.hexinfo.arch_ai_review.entity.User;
import cn.hexinfo.arch_ai_review.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/test")
public class TestLoginController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @GetMapping("/check-password")
    public ApiResponse<Map<String, Object>> checkPassword(
            @RequestParam String username, 
            @RequestParam String password) {
        
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            return ApiResponse.error(404, "用户不存在: " + username);
        }
        
        User user = userOpt.get();
        boolean matches = passwordEncoder.matches(password, user.getPassword());
        
        Map<String, Object> result = new HashMap<>();
        result.put("username", username);
        result.put("passwordMatches", matches);
        result.put("encodedPassword", user.getPassword());
        result.put("userRole", user.getRole());
        
        return ApiResponse.success("密码检查结果", result);
    }
}
