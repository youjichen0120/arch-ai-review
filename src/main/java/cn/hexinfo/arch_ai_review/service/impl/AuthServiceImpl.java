package cn.hexinfo.arch_ai_review.service.impl;

import cn.hexinfo.arch_ai_review.dto.LoginRequestDto;
import cn.hexinfo.arch_ai_review.dto.LoginResponseDto;
import cn.hexinfo.arch_ai_review.entity.User;
import cn.hexinfo.arch_ai_review.exception.AuthenticationException;
import cn.hexinfo.arch_ai_review.exception.ResourceNotFoundException;
import cn.hexinfo.arch_ai_review.repository.UserRepository;
import cn.hexinfo.arch_ai_review.security.JwtTokenProvider;
import cn.hexinfo.arch_ai_review.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Override
    public LoginResponseDto login(LoginRequestDto loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String token = tokenProvider.generateToken(authentication);
            String refreshToken = tokenProvider.generateRefreshToken(loginRequest.getUsername());
            
            User user = getUserByUsername(loginRequest.getUsername());
            
            // 计算令牌过期时间
            Date expiryDate = tokenProvider.getExpirationDateFromToken(token);
            LocalDateTime expiresAt = expiryDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            
            // 构建权限列表
            List<String> permissions = user.getAuthorities().stream()
                    .map(auth -> auth.getAuthority())
                    .collect(Collectors.toList());
            
            // 构建响应
            LoginResponseDto.UserDto userDto = LoginResponseDto.UserDto.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .permissions(permissions)
                    .build();
            
            return LoginResponseDto.builder()
                    .token(token)
                    .refreshToken(refreshToken)
                    .expiresAt(expiresAt)
                    .user(userDto)
                    .build();
            
        } catch (Exception e) {
            log.error("登录失败: {}", e.getMessage());
            throw new AuthenticationException("用户名或密码错误");
        }
    }

    @Override
    public LoginResponseDto refreshToken(String refreshToken) {
        try {
            if (refreshToken == null || refreshToken.isEmpty()) {
                throw new AuthenticationException("刷新令牌为空");
            }
            
            if (!tokenProvider.validateToken(refreshToken)) {
                throw new AuthenticationException("刷新令牌已过期或无效");
            }
            
            if (!tokenProvider.isRefreshToken(refreshToken)) {
                throw new AuthenticationException("无效的令牌类型，不是刷新令牌");
            }
            
            String username = tokenProvider.getUsernameFromToken(refreshToken);
            if (username == null || username.isEmpty()) {
                throw new AuthenticationException("刷新令牌中无法获取用户名");
            }
            
            User user = getUserByUsername(username);
            if (user == null) {
                throw new AuthenticationException("找不到对应的用户");
            }
            
            String newToken = tokenProvider.generateToken(username);
            String newRefreshToken = tokenProvider.generateRefreshToken(username);
            
            // 计算令牌过期时间
            Date expiryDate = tokenProvider.getExpirationDateFromToken(newToken);
            LocalDateTime expiresAt = expiryDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            
            // 构建权限列表
            List<String> permissions = user.getAuthorities().stream()
                    .map(auth -> auth.getAuthority())
                    .collect(Collectors.toList());
            
            // 构建响应
            LoginResponseDto.UserDto userDto = LoginResponseDto.UserDto.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .permissions(permissions)
                    .build();
            
            return LoginResponseDto.builder()
                    .token(newToken)
                    .refreshToken(newRefreshToken)
                    .expiresAt(expiresAt)
                    .user(userDto)
                    .build();
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.error("刷新令牌时发生错误", e);
            throw new AuthenticationException("刷新令牌失败: " + e.getMessage());
        }
    }

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            return getUserByUsername(username);
        }
        
        return null;
    }

    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + username));
    }
}
