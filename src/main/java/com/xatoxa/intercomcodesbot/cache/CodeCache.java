package com.xatoxa.intercomcodesbot.cache;

import com.xatoxa.intercomcodesbot.entity.Entrance;
import com.xatoxa.intercomcodesbot.entity.Home;
import com.xatoxa.intercomcodesbot.entity.IntercomCode;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class CodeCache {
    private Home home;
    private Entrance entrance;
    private IntercomCode code;

    @Override
    public String toString(){
        StringBuilder address = new StringBuilder();
        address
                .append(home.getStreet())
                .append(", ")
                .append(home.getNumber())
                .append(", ")
                .append(entrance.getNumber())
                .append(", ")
                .append(code.getText());

        return address.toString();
    }
}
