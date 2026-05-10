package dx.ahut.adbackend.auth;

import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank String phone,
            @NotBlank String password,
            @NotBlank String confirmPassword
    ) {
    }

    public record LoginRequest(
            @NotBlank String phone,
            @NotBlank String password
    ) {
    }

    public record BindUserRequest(
            @NotBlank String studentId,
            @NotBlank String phone
    ) {
    }

    public record ResetPasswordRequest(
            @NotBlank String studentId,
            @NotBlank String phone,
            @NotBlank String newPassword,
            @NotBlank String confirmPassword
    ) {
    }

    public record UpdateProfileRequest(
            @NotBlank String phone,
            @NotBlank String name,
            @NotBlank String studentId,
            String avatarUrl
    ) {
    }

    public record UserResponse(
            Long id,
            String phone,
            String name,
            String studentId,
            String avatarUrl
    ) {

        public static UserResponse from(User user) {
            return new UserResponse(
                    user.getId(),
                    user.getPhone(),
                    user.getName(),
                    user.getStudentId(),
                    user.getAvatarUrl()
            );
        }
    }
}
