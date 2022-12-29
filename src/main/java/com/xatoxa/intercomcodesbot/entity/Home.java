package com.xatoxa.intercomcodesbot.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Getter
@Setter
@Entity(name="homes")
public class Home {
    @Id
    private Long id;

    @Column
    private String lan;

    @Column
    private String lat;

    @Column
    private String street;

    @Column
    private String number;

    @OneToMany(mappedBy = "home", cascade = CascadeType.ALL)
    private List<Entry> entrances;
}
