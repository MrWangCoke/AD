package dx.ahut.adbackend.ticket;

import dx.ahut.adbackend.auth.User;
import dx.ahut.adbackend.auth.UserRepository;
import dx.ahut.adbackend.ticket.TicketDtos.CreateTicketRequest;
import dx.ahut.adbackend.ticket.TicketDtos.TicketResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public TicketService(TicketRepository ticketRepository, UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TicketResponse createNewUserBindTicket(CreateTicketRequest request) {
        String studentId = normalize(request.studentId());
        String phone = normalize(request.phone());
        if (studentId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入学号");
        }
        if (phone.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入手机号");
        }

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录后再提交工单"));
        if (!phone.equals(user.getPhone())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请将该账号手机号改成电信校园卡号码");
        }

        Ticket ticket = new Ticket(
                user,
                studentId,
                Ticket.TYPE_NEW_USER_BIND,
                phone,
                normalizeDefault(request.newPassword(), "123456"),
                phone,
                "待处理"
        );
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listByUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        return ticketRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(TicketResponse::from)
                .toList();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeDefault(String value, String fallback) {
        String normalized = normalize(value);
        return normalized.isBlank() ? fallback : normalized;
    }
}
