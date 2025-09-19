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
@RequestMapping("/api/v1/admin")
public class PasswordResetController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @GetMapping("/reset-password")
    public ApiResponse<Map<String, Object>> resetPassword(
            @RequestParam String username, 
            @RequestParam String newPassword) {
        
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            return ApiResponse.error(404, "用户不存在: " + username);
        }
        
        User user = userOpt.get();
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        userRepository.save(user);
        
        Map<String, Object> result = new HashMap<>();
        result.put("username", username);
        result.put("passwordReset", true);
        result.put("newEncodedPassword", encodedPassword);
        
        return ApiResponse.success("密码已重置", result);
    }
}
