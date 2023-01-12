package com.xatoxa.intercomcodesbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "users")
public class User {
    @Id
    private Long id;

    @Column
    private Long chatId;

    @Column
    private String firstName;

    @Column
    private String lastName;

    @Column
    private String username;

    @Column
    private boolean admin;

    @Column
    private boolean enabled;

    @Override
    public String toString() {
        String link =
                this.username == null ?
                        "[" + this.id + "](tg://user?id=" + this.id + ")"
                        : "@" + this.username;
        return this.id + " " + link + " " + (this.firstName == null ? "" : this.firstName) + " " +
                (this.lastName == null ? "" : this.lastName);
    }
}
