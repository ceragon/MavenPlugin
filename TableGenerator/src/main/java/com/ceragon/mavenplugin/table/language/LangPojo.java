package com.ceragon.mavenplugin.table.language;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LangPojo {
    private String name;
    private int code;
    private String desc;
}
