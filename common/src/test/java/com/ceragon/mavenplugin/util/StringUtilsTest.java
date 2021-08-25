package com.ceragon.mavenplugin.util;

import junit.framework.TestCase;

public class StringUtilsTest extends TestCase {

    public void testFormatVelocity() {
        String source = "hello ${name}";
        String value = StringUtils.formatKV(source, "name", "kevin");
        assertEquals("hello kevin", value);
    }
}