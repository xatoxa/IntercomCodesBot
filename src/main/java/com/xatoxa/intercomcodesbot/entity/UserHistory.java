package com.xatoxa.intercomcodesbot.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity(name = "users_history")
public class UserHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long userId;

    @Column
    private String username;

    @Column
    private String action;

    @Column
    private LocalDateTime dateTime;

    public UserHistory(Long userId, String username, String action, LocalDateTime dateTime) {
        this.userId = userId;
        this.username = username;
        this.action = action;
        this.dateTime = dateTime;
    }

    @Override
    public String toString() {
        String link = username == null ? "[" + this.userId + "](tg://user?id=" + this.userId + ")" : "@" + this.username;

        return link + " | " + this.action;
    }
}
