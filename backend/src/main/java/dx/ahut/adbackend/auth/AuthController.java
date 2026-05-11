package dx.ahut.adbackend.auth;

import dx.ahut.adbackend.ratelimit.ClientIp;
import dx.ahut.adbackend.ratelimit.RateLimitService;
import dx.ahut.adbackend.auth.AuthDtos.LoginRequest;
import dx.ahut.adbackend.auth.AuthDtos.RegisterRequest;
import dx.ahut.adbackend.auth.AuthDtos.BindUserRequest;
import dx.ahut.adbackend.auth.AuthDtos.ResetPasswordRequest;
import dx.ahut.adbackend.auth.AuthDtos.UpdateProfileRequest;
import dx.ahut.adbackend.auth.AuthDtos.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RateLimitService rateLimitService;

    public AuthController(AuthService authService, RateLimitService rateLimitService) {
        this.authService = authService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/register")
    public UserResponse register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest
    ) {
        rateLimitService.check(
                "auth:register:" + ClientIp.from(servletRequest) + ":" + normalize(request.phone()),
                8,
                Duration.ofMinutes(5),
                "注册请求过于频繁，请稍后再试"
        );
        return authService.register(request);
    }

    @PostMapping("/login")
    public UserResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        rateLimitService.check(
                "auth:login:" + ClientIp.from(servletRequest) + ":" + normalize(request.phone()),
                20,
                Duration.ofMinutes(5),
                "登录请求过于频繁，请稍后再试"
        );
        return authService.login(request);
    }

    @PostMapping("/bind")
    public UserResponse bind(@Valid @RequestBody BindUserRequest request) {
        return authService.bind(request);
    }

    @PostMapping("/reset-password")
    public UserResponse resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest servletRequest
    ) {
        rateLimitService.check(
                "auth:reset-password:" + ClientIp.from(servletRequest) + ":" + normalize(request.phone()),
                5,
                Duration.ofMinutes(15),
                "重置密码请求过于频繁，请稍后再试"
        );
        return authService.resetPassword(request);
    }

    @PutMapping("/profile/{id}")
    public UserResponse updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return authService.updateProfile(id, request);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
