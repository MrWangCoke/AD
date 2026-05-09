package dx.ahut.adbackend.ticket;

import dx.ahut.adbackend.auth.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
public class Ticket {

    public static final int TYPE_NEW_USER_BIND = 1;
    public static final int TYPE_ACCOUNT_NOT_FOUND = 2;
    public static final int TYPE_BROADBAND_PASSWORD = 3;
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_QUEUED = 1;
    public static final int STATUS_PROCESSING = 2;
    public static final int STATUS_COMPLETED = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_no", nullable = false, unique = true, length = 30)
    private String ticketNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "student_id", nullable = false, length = 50)
    private String studentId;

    @Column(name = "ticket_type", nullable = false)
    private Integer ticketType;

    @Column(name = "status")
    private Integer status = STATUS_PENDING;

    @Column(name = "broadband_account", length = 50)
    private String broadbandAccount;

    @Column(name = "new_password", length = 255)
    private String newPassword;

    @Column(length = 20)
    private String phone;

    @Column(name = "result_message")
    private String resultMessage;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected Ticket() {
    }

    public Ticket(User user, String studentId, Integer ticketType, String broadbandAccount, String newPassword, String phone, String resultMessage) {
        this.user = user;
        this.studentId = studentId;
        this.ticketType = ticketType;
        this.broadbandAccount = broadbandAccount;
        this.newPassword = newPassword;
        this.phone = phone;
        this.resultMessage = resultMessage;
    }

    @PrePersist
    void assignTicketNo() {
        if (ticketNo == null || ticketNo.isBlank()) {
            ticketNo = "TK-" + System.currentTimeMillis();
        }
        if (status == null) {
            status = STATUS_PENDING;
        }
    }

    public Long getId() {
        return id;
    }

    public String getTicketNo() {
        return ticketNo;
    }

    public User getUser() {
        return user;
    }

    public String getStudentId() {
        return studentId;
    }

    public Integer getTicketType() {
        return ticketType;
    }

    public Integer getStatus() {
        return status;
    }

    public String getBroadbandAccount() {
        return broadbandAccount;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public String getPhone() {
        return phone;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
