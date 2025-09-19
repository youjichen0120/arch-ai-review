package cn.hexinfo.arch_ai_review.controller;

import cn.hexinfo.arch_ai_review.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {
    
    @GetMapping
    public ApiResponse<Object> checkHealth() {
        return ApiResponse.success("服务正常运行", 
                new Object() {
                    public final String status = "UP";
                    public final String version = "1.0.0";
                });
    }
}
