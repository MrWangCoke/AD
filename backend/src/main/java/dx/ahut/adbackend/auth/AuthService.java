package dx.ahut.adbackend.auth;

import dx.ahut.adbackend.auth.AuthDtos.BindUserRequest;
import dx.ahut.adbackend.auth.AuthDtos.LoginRequest;
import dx.ahut.adbackend.auth.AuthDtos.RegisterRequest;
import dx.ahut.adbackend.auth.AuthDtos.ResetPasswordRequest;
import dx.ahut.adbackend.auth.AuthDtos.UpdateProfileRequest;
import dx.ahut.adbackend.auth.AuthDtos.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final String MAINLAND_PHONE_PATTERN = "^1[3-9]\\d{9}$";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String phone = normalize(request.phone());
        String password = normalize(request.password());
        String confirmPassword = normalize(request.confirmPassword());

        if (!phone.matches(MAINLAND_PHONE_PATTERN)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入正确的11位手机号");
        }
        if (!password.equals(confirmPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "两次密码不一致");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该号码已注册");
        }

        User user = new User(
                phone,
                passwordEncoder.encode(password),
                defaultName(phone),
                "未填写",
                null
        );
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse login(LoginRequest request) {
        String phone = normalize(request.phone());
        String password = normalize(request.password());
        if (!phone.matches(MAINLAND_PHONE_PATTERN)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入正确的11位手机号");
        }
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "号码或密码错误"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "号码或密码错误");
        }
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse bind(BindUserRequest request) {
        String phone = normalize(request.phone());
        String studentId = normalize(request.studentId());
        validatePhone(phone);
        if (studentId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入学号");
        }

        User user = userRepository.findByPhoneAndStudentId(phone, studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到匹配用户，请先注册或检查手机号和学号"));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse resetPassword(ResetPasswordRequest request) {
        String phone = normalize(request.phone());
        String studentId = normalize(request.studentId());
        String newPassword = normalize(request.newPassword());
        String confirmPassword = normalize(request.confirmPassword());

        validatePhone(phone);
        if (studentId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入学号");
        }
        if (newPassword.isBlank() || confirmPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入新密码和确认密码");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "两次密码不一致");
        }

        User user = userRepository.findByPhoneAndStudentId(phone, studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "学号和手机号不匹配"));
        user.updatePasswordHash(passwordEncoder.encode(newPassword));
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateProfile(Long id, UpdateProfileRequest request) {
        String phone = normalize(request.phone());
        String name = normalize(request.name());
        String studentId = normalize(request.studentId());
        String avatarUrl = normalizeNullable(request.avatarUrl());

        validatePhone(phone);
        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入姓名");
        }
        if (studentId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入学号");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        userRepository.findByPhone(phone)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "该手机号已被其他用户使用");
                });

        user.updateProfile(phone, name, studentId, avatarUrl);
        return UserResponse.from(userRepository.save(user));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeNullable(String value) {
        String normalized = normalize(value);
        return normalized.isEmpty() ? null : normalized;
    }

    private static void validatePhone(String phone) {
        if (!phone.matches(MAINLAND_PHONE_PATTERN)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入正确的11位手机号");
        }
    }

    private static String defaultName(String phone) {
        int start = Math.max(0, phone.length() - 4);
        return "用户" + phone.substring(start);
    }
}
