package dx.ahut.adbackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dx.ahut.adbackend.auth.AuthDtos.RegisterRequest;
import dx.ahut.adbackend.auth.AuthDtos.UserResponse;
import dx.ahut.adbackend.auth.AuthService;
import dx.ahut.adbackend.auth.User;
import dx.ahut.adbackend.auth.UserRepository;
import dx.ahut.adbackend.ticket.Ticket;
import dx.ahut.adbackend.ticket.TicketDtos.CreateTicketRequest;
import dx.ahut.adbackend.ticket.TicketDtos.TicketResponse;
import dx.ahut.adbackend.ticket.TicketRepository;
import dx.ahut.adbackend.ticket.TicketService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendResilienceTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void clearDatabase() {
        ticketRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void duplicateRegisterReturnsConflictJson() throws Exception {
        String payload = """
                {"phone":"13900000001","password":"123456","confirmPassword":"123456"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("该号码已注册"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void concurrentRegisterAllowsOnlyOneAccountForSamePhone() throws Exception {
        String phone = "13900000002";
        ExecutorService executor = Executors.newFixedThreadPool(8);
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            tasks.add(() -> {
                try {
                    authService.register(new RegisterRequest(phone, "123456", "123456"));
                    return true;
                } catch (ResponseStatusException error) {
                    return false;
                }
            });
        }

        List<Future<Boolean>> futures = executor.invokeAll(tasks);
        executor.shutdown();

        long successCount = 0;
        for (Future<Boolean> future : futures) {
            if (future.get()) {
                successCount++;
            }
        }

        assertThat(successCount).isEqualTo(1);
        assertThat(userRepository.findByPhone(phone)).isPresent();
    }

    @Test
    void newUserBindTicketIsIdempotentForOpenTicket() {
        UserResponse user = authService.register(new RegisterRequest("13900000003", "123456", "123456"));
        CreateTicketRequest request = new CreateTicketRequest(
                Ticket.TYPE_NEW_USER_BIND,
                user.id(),
                "S20260001",
                user.phone(),
                null,
                null
        );

        TicketResponse first = ticketService.createNewUserBindTicket(request);
        TicketResponse second = ticketService.createNewUserBindTicket(request);

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(ticketRepository.findAll()).hasSize(1);
    }

    @Test
    void pendingTicketsDefaultToOneHundredAndCapLimitAtFiveHundred() throws Exception {
        User user = userRepository.saveAndFlush(new User(
                "13900000004",
                "hash",
                "测试用户",
                "S20260002",
                null
        ));
        for (int i = 0; i < 120; i++) {
            Ticket ticket = new Ticket(
                    user,
                    "S" + i,
                    Ticket.TYPE_BROADBAND_PASSWORD,
                    "NET" + i,
                    "123456",
                    user.getPhone(),
                    "待处理"
            );
            ticket.assignTicketNo("TK-TEST-" + i);
            ticket.ensurePendingStatus();
            ticketRepository.save(ticket);
        }
        ticketRepository.flush();

        mockMvc.perform(get("/api/tickets/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(100));

        mockMvc.perform(get("/api/tickets/pending?limit=999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(120));
    }

    @Test
    void registerRateLimitReturnsTooManyRequestsJson() throws Exception {
        String payload = """
                {"phone":"13900000005","password":"123456","confirmPassword":"123456"}
                """;

        for (int i = 0; i < 8; i++) {
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload));
        }

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.message").value("注册请求过于频繁，请稍后再试"));
    }

    @Test
    void validationErrorsUseUnifiedJsonShape() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("请求参数不正确"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }
}
