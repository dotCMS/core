package com.dotcms.rendering.velocity.util;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.vavr.Tuple3;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class VelocityUtilTest {

    @DataProvider
    public static Object[] dataProviderTestConvertToVelocityVariable() {
        return new Tuple3[] {
                // actual, expected, firstLetterUppercase
                new Tuple3<>("123", "one23", false),
                new Tuple3<>("123", "One23", true),
                new Tuple3<>("_123", "_123", false),
                new Tuple3<>("_123a", "_123a", false),
                new Tuple3<>("_123a", "_123a", true),
                new Tuple3<>("asd123asd", "asd123asd", false),
                new Tuple3<>("asd123asd", "Asd123asd", true),
                new Tuple3<>("#%#$", "____", true),
                new Tuple3<>("#%#$1", "____1", true),
                new Tuple3<>("#%#$abc", "____abc", true),
                new Tuple3<>("#%#$abc", "____abc", false),

        };
    }

    @Test
    @UseDataProvider("dataProviderTestConvertToVelocityVariable")
    public void testConvertToVelocityVariable(final Tuple3<String, String, Boolean> testCase) {
        Assert.assertEquals(testCase._2,
                VelocityUtil.convertToVelocityVariable(testCase._1, testCase._3));
    }

}
