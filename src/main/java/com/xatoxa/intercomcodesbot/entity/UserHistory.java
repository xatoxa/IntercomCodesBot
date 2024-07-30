package com.xatoxa.intercomcodesbot.entity;

import com.xatoxa.intercomcodesbot.ActionType;
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
    private String actionType;

    @Column
    private LocalDateTime dateTime;

    public UserHistory(Long userId, String username, String action, ActionType actionType, LocalDateTime dateTime) {
        this.userId = userId;
        this.username = username;
        this.action = action;
        this.actionType = actionType.name();
        this.dateTime = dateTime;
    }

    @Override
    public String toString() {
        String link = username == null ?
                "<a href=\"tg://user?id=" + this.userId + "\"> " + this.userId + "</a>"
                : "@" + this.username;

        return link + " | " + this.action;
    }
}
