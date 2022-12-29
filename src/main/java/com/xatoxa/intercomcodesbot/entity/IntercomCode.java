package com.xatoxa.intercomcodesbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@Entity(name="codes")
public class IntercomCode {
    @Id
    private Long id;

    @Column
    private String text;

    @ManyToOne
    private Entry entry;
}
