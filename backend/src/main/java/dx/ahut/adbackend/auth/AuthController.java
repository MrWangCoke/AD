package dx.ahut.adbackend.auth;

import dx.ahut.adbackend.auth.AuthDtos.LoginRequest;
import dx.ahut.adbackend.auth.AuthDtos.RegisterRequest;
import dx.ahut.adbackend.auth.AuthDtos.BindUserRequest;
import dx.ahut.adbackend.auth.AuthDtos.ResetPasswordRequest;
import dx.ahut.adbackend.auth.AuthDtos.UpdateProfileRequest;
import dx.ahut.adbackend.auth.AuthDtos.UserResponse;
import jakarta.validation.Valid;
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

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/bind")
    public UserResponse bind(@Valid @RequestBody BindUserRequest request) {
        return authService.bind(request);
    }

    @PostMapping("/reset-password")
    public UserResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    @PutMapping("/profile/{id}")
    public UserResponse updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return authService.updateProfile(id, request);
    }
}
