package dx.ahut.adbackend.auth;

import dx.ahut.adbackend.auth.AuthDtos.LoginRequest;
import dx.ahut.adbackend.auth.AuthDtos.RegisterRequest;
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

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String defaultName(String phone) {
        int start = Math.max(0, phone.length() - 4);
        return "用户" + phone.substring(start);
    }
}
