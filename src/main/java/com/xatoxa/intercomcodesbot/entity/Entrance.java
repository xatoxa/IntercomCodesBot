package com.xatoxa.intercomcodesbot.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity(name="entrances")
public class Entrance extends HomeAbstract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String number;

    @ManyToOne
    private Home home;

    @OneToMany(mappedBy = "entrance", cascade = CascadeType.REMOVE, fetch = FetchType.EAGER, orphanRemoval = true)
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

    public String getTextCodes(){
        StringBuilder text = new StringBuilder();
        text.append("\n");
        if (codes.size() == 0)
            text.append("---");
        else {
            for (IntercomCode code :
                    this.codes) {
                text.append("\t\t");
                text.append(code.getText());
                text.append("\n");
            }
        }

        return text.toString();
    }

    public void dismissHome(){
        this.home.delEntrance(this);
        this.home = null;
    }

    @Override
    public String getAddress() {
        return this.number;
    }

    public String getInverseAddress(){
        return this.home.getAddress() + ", п. " + this.number;

    }
}
