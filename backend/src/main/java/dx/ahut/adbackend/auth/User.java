package dx.ahut.adbackend.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String phone;

    @Column(nullable = false, length = 120)
    private String passwordHash;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 32)
    private String studentId;

    @Column(length = 512)
    private String avatarUrl;

    protected User() {
    }

    public User(String phone, String passwordHash, String name, String studentId, String avatarUrl) {
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.name = name;
        this.studentId = studentId;
        this.avatarUrl = avatarUrl;
    }

    public Long getId() {
        return id;
    }

    public String getPhone() {
        return phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getName() {
        return name;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void updateProfile(String phone, String name, String studentId, String avatarUrl) {
        this.phone = phone;
        this.name = name;
        this.studentId = studentId;
        this.avatarUrl = avatarUrl;
    }

    public void updatePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
