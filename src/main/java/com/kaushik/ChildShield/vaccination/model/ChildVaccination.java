package com.kaushik.ChildShield.vaccination.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kaushik.ChildShield.child.model.Child;
import com.kaushik.ChildShield.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "child_vaccinations", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"child_id", "schedule_dose_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChildVaccination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Child child;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "schedule_dose_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ScheduleDose scheduleDose;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "administered_date")
    private LocalDate administeredDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "assignedVillages"})
    private User recordedBy;

    @Column(length = 500)
    private String notes;

    @JsonProperty("childName")
    public String getChildName() {
        return child != null ? child.getFullName() : null;
    }

    @JsonProperty("childDob")
    public LocalDate getChildDob() {
        return child != null ? child.getDateOfBirth() : null;
    }

    @JsonProperty("vaccineName")
    public String getVaccineName() {
        return scheduleDose != null ? scheduleDose.getDisplayName() : null;
    }

    @JsonProperty("doseCode")
    public String getDoseCode() {
        return scheduleDose != null ? scheduleDose.getDoseCode() : null;
    }

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum Status {
        PENDING,
        UPCOMING,
        DUE_TODAY,
        OVERDUE,
        COMPLETED,
        CANCELLED
    }
}
