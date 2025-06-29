package com.example.cs25entity.domain.subscription.entity;

import com.example.cs25common.global.entity.BaseEntity;
import com.example.cs25entity.domain.quiz.entity.QuizCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "subscription")
public class Subscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quizCategory_id")
    private QuizCategory category;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(columnDefinition = "DATE")
    private LocalDate startDate;

    @Column(columnDefinition = "DATE")
    private LocalDate endDate;

    private boolean isActive;

    private int subscriptionType; // "월화수목금토일" => "1111111" => 127

    @Column(unique = true)
    private String serialId;

    @Builder
    public Subscription(QuizCategory category, String email, LocalDate startDate,
        LocalDate endDate, Set<DayOfWeek> subscriptionType) {
        this.category = category;
        this.email = email;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = true;
        this.subscriptionType = encodeDays(subscriptionType);
        this.serialId = UUID.randomUUID().toString();
    }

    // Set<DayOfWeek> → int
    public static int encodeDays(Set<DayOfWeek> days) {
        int result = 0;
        for (DayOfWeek day : days) {
            result |= day.getBitValue();
        }
        return result;
    }

    // int → Set<DayOfWeek>
    public static Set<DayOfWeek> decodeDays(int bits) {
        Set<DayOfWeek> result = EnumSet.noneOf(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            if (DayOfWeek.contains(bits, day)) {
                result.add(day);
            }
        }
        return result;
    }

    /**
     * 오늘이 구독한 날짜인지 확인하는 메서드
     * @return true/false 반환
     */
    public boolean isTodaySubscribed() {
        int todayIndex = LocalDate.now().getDayOfWeek().getValue() % 7;
        int todayBit = 1 << todayIndex;
        return (this.subscriptionType & todayBit) > 0;
    }

    /**
     * 사용자가 입력한 값으로 구독정보를 업데이트하는 메서드
     *
     * @param category 퀴즈 카테고리
     * @param days     구독 요일 정보
     * @param isActive 활성화 상태
     * @param period   기간 연장 정보
     */
    public void update(QuizCategory category, Set<DayOfWeek> days,
        boolean isActive, SubscriptionPeriod period) {
        this.category = category;
        this.subscriptionType = encodeDays(days);
        this.isActive = isActive;
        this.endDate = this.endDate.plusMonths(period.getMonths());
    }

    /**
     * 구독 비활성화하는 메서드
     */
    public void updateDisable() {
        this.isActive = false;
    }

    /**
     * 구독 활성화하는 메서드
     */
    public void updateEnable() {
        this.isActive = true;
    }
}
