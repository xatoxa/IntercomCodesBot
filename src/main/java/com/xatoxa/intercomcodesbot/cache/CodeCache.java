package com.xatoxa.intercomcodesbot.cache;

import com.xatoxa.intercomcodesbot.entity.Entrance;
import com.xatoxa.intercomcodesbot.entity.Home;
import com.xatoxa.intercomcodesbot.entity.IntercomCode;
import lombok.Data;

@Data
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
}
