package com.xatoxa.intercomcodesbot.cache;

import com.xatoxa.intercomcodesbot.entity.Entry;
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
    private Entry entry;
    private IntercomCode code;

}
