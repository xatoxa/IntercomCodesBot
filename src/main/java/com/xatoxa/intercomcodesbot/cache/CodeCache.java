package com.xatoxa.intercomcodesbot.cache;

import com.xatoxa.intercomcodesbot.entity.Entrance;
import com.xatoxa.intercomcodesbot.entity.Home;
import com.xatoxa.intercomcodesbot.entity.IntercomCode;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@NoArgsConstructor
@Setter
public class CodeCache {
    private Home home;
    private Entrance entrance;
    private IntercomCode code;

    @Override
    public String toString(){

        return home.getStreet() +
                ", " +
                home.getNumber() +
                ", " +
                entrance.getNumber() +
                ", " +
                code.getText();
    }

    public Home getHome() {
        return Objects.requireNonNullElseGet(this.home, Home::new);
    }

    public Entrance getEntrance() {
        return Objects.requireNonNullElseGet(this.entrance, Entrance::new);
    }

    public IntercomCode getCode() {
        return Objects.requireNonNullElseGet(this.code, IntercomCode::new);
    }
}
