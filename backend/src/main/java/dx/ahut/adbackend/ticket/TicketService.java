package dx.ahut.adbackend.ticket;

import dx.ahut.adbackend.auth.User;
import dx.ahut.adbackend.auth.UserRepository;
import dx.ahut.adbackend.ticket.TicketDtos.CreateTicketRequest;
import dx.ahut.adbackend.ticket.TicketDtos.TicketResponse;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TicketService {

    private static final String MAINLAND_PHONE_PATTERN = "^1[3-9]\\d{9}$";
    private static final List<Integer> OPEN_TICKET_STATUSES = List.of(
            Ticket.STATUS_PENDING,
            Ticket.STATUS_QUEUED,
            Ticket.STATUS_PROCESSING
    );
    private static final int DEFAULT_PENDING_LIMIT = 100;
    private static final int MAX_PENDING_LIMIT = 500;
    private static final int TICKET_NO_RETRY_LIMIT = 2;

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketNumberGenerator ticketNumberGenerator;
    private final ConcurrentMap<String, Object> newUserBindLocks = new ConcurrentHashMap<>();

    public TicketService(
            TicketRepository ticketRepository,
            UserRepository userRepository,
            TicketNumberGenerator ticketNumberGenerator
    ) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.ticketNumberGenerator = ticketNumberGenerator;
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
        validatePhone(phone);

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录后再提交工单"));
        if (!phone.equals(user.getPhone())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请将该账号手机号改成电信校园卡号码");
        }

        String bindKey = user.getId() + ":" + studentId + ":" + phone;
        Object lock = newUserBindLocks.computeIfAbsent(bindKey, ignored -> new Object());
        synchronized (lock) {
            return ticketRepository.findFirstByUserIdAndStudentIdAndPhoneAndTicketTypeAndStatusInOrderByCreatedAtDesc(
                            user.getId(),
                            studentId,
                            phone,
                            Ticket.TYPE_NEW_USER_BIND,
                            OPEN_TICKET_STATUSES
                    )
                    .map(TicketResponse::from)
                    .orElseGet(() -> createOpenNewUserBindTicket(user, studentId, phone, request));
        }
    }

    private TicketResponse createOpenNewUserBindTicket(
            User user,
            String studentId,
            String phone,
            CreateTicketRequest request
    ) {
        Ticket ticket = new Ticket(
                user,
                studentId,
                Ticket.TYPE_NEW_USER_BIND,
                phone,
                normalizeDefault(request.newPassword(), "123456"),
                phone,
                "待处理"
        );
        try {
            return TicketResponse.from(saveWithTicketNoRetry(ticket));
        } catch (DataIntegrityViolationException error) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "工单提交冲突，请稍后重试");
        }
    }

    @Transactional
    public TicketResponse createBroadbandPasswordTicket(CreateTicketRequest request) {
        String studentId = normalize(request.studentId());
        String phone = normalize(request.phone());
        String broadbandAccount = normalize(request.broadbandAccount());
        String newPassword = normalize(request.newPassword());

        if (studentId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先完善学号后再提交工单");
        }
        if (phone.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先完善手机号后再提交工单");
        }
        validatePhone(phone);
        if (broadbandAccount.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未识别到宽带账号");
        }
        if (newPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未识别到宽带密码");
        }

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录后再提交工单"));

        Ticket ticket = new Ticket(
                user,
                studentId,
                Ticket.TYPE_BROADBAND_PASSWORD,
                broadbandAccount,
                newPassword,
                phone,
                "待处理"
        );
        try {
            return TicketResponse.from(saveWithTicketNoRetry(ticket));
        } catch (DataIntegrityViolationException error) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "工单提交冲突，请稍后重试");
        }
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

    @Transactional(readOnly = true)
    public List<TicketResponse> listPendingTickets(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_PENDING_LIMIT));
        if (limit <= 0) {
            safeLimit = DEFAULT_PENDING_LIMIT;
        }
        PageRequest pageRequest = PageRequest.of(0, safeLimit, Sort.by("createdAt").ascending().and(Sort.by("id").ascending()));
        return ticketRepository.findByStatusOrderByCreatedAtAsc(Ticket.STATUS_PENDING, pageRequest)
                .stream()
                .map(TicketResponse::from)
                .toList();
    }

    private Ticket saveWithTicketNoRetry(Ticket ticket) {
        for (int attempt = 0; attempt < TICKET_NO_RETRY_LIMIT; attempt++) {
            String ticketNo = ticketNumberGenerator.nextTicketNo();
            if (ticketRepository.existsByTicketNo(ticketNo)) {
                continue;
            }
            ticket.assignTicketNo(ticketNo);
            ticket.ensurePendingStatus();
            return ticketRepository.saveAndFlush(ticket);
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "工单号生成冲突，请重试");
    }

    private static void validatePhone(String phone) {
        if (!phone.matches(MAINLAND_PHONE_PATTERN)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入正确的11位手机号");
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeDefault(String value, String fallback) {
        String normalized = normalize(value);
        return normalized.isBlank() ? fallback : normalized;
    }
}
