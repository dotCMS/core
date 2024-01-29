package com.dotmarketing.util;

import org.junit.Assert;
import org.junit.Test;

public class TestConfig {

    public enum TestEnum {

        ONE(1),
        TWO(2),
        THREE(3),
        FOUR(4),
        FIVE(5);

        private int code;
        private TestEnum(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    @Test
    public void test_getCustomArrayProperty_no_exists() {

        final TestEnum[] enums = Config.getCustomArrayProperty("noexists", TestEnum::valueOf, TestEnum.class,
                ()-> new TestEnum [] {TestEnum.ONE, TestEnum.FOUR});

        Assert.assertNotNull(enums);
        Assert.assertEquals("should returns 2 elements as  default", 2, enums.length);
        Assert.assertArrayEquals("default array is wrong",  new TestEnum [] {TestEnum.ONE, TestEnum.FOUR}, enums);
    }

    @Test
    public void test_getCustomArrayProperty_exists() {

        final String key = "testenums";
        Config.setProperty(key, "TWO,THREE,FOUR");

        final TestEnum[] enums = Config.getCustomArrayProperty(key, TestEnum::valueOf, TestEnum.class,
                ()-> new TestEnum [] {TestEnum.ONE, TestEnum.FOUR});

        Assert.assertNotNull(enums);
        Assert.assertEquals("should returns 3 elements", 3, enums.length);
        Assert.assertArrayEquals("array is wrong",  new TestEnum [] {TestEnum.TWO, TestEnum.THREE, TestEnum.FOUR}, enums);
    }
}
