package com.xatoxa.intercomcodesbot.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Data
@Getter
@Setter
@Entity(name="entrances")
public class Entrance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String number;

    @ManyToOne
    private Home home;

    @OneToMany
    private List<IntercomCode> codes;

    public void addCode(IntercomCode code){
        if (this.codes == null){
            this.codes = new ArrayList<>();
        }
        this.codes.add(code);
    }

    public void delCode(IntercomCode code){
        this.codes.remove(code);
    }
}
