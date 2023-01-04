package com.xatoxa.intercomcodesbot.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
@Entity(name = "users_history")
public class UserHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @NonNull
    private Long userId;

    @Column
    @NonNull
    private String action;

    @Column
    @NonNull
    private LocalDateTime dateTime;

    @Override
    public String toString() {
        return "[" + this.userId + "](tg://user?id=" + this.userId + ") | " + this.action;
    }
}
