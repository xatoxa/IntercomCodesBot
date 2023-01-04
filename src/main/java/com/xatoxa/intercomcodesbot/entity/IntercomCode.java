package com.xatoxa.intercomcodesbot.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity(name="codes")
public class IntercomCode extends HomeAbstract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String text;

    @ManyToOne
    private Entrance entrance;

    @Override
    public String getAddress() {
        return this.text;
    }

    public void dismissEntrance(){
        this.entrance.delCode(this);
        this.entrance = null;
    }
}
