package com.xatoxa.intercomcodesbot.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@Entity(name="codes")
public class IntercomCode extends HomeEntranceAbstract{
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
