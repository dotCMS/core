package com.dotcms.visitor.filter.logger;

import com.dotcms.visitor.filter.characteristics.AbstractCharacter;
import com.dotmarketing.util.UtilMethods;
import java.lang.reflect.Constructor;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class VisitorLoggerTest {


    @Test
    public void testAddConstructor() throws NoSuchMethodException {
        List<Constructor<AbstractCharacter>> result = VisitorLogger.addConstructor(CustomCharacterTest.class);

        Assert.assertTrue(UtilMethods.isSet(result));
        Assert.assertTrue(result.stream()
                .filter(constructor -> constructor.getDeclaringClass().getName()
                        .equals(CustomCharacterTest.class.getName())).findAny().isPresent());
    }

    @Test
    public void testRemoveConstructor() throws NoSuchMethodException {

        VisitorLogger.addConstructor(CustomCharacterTest.class);
        List<Constructor<AbstractCharacter>> result = VisitorLogger.removeConstructor(CustomCharacterTest.class);

        Assert.assertTrue(UtilMethods.isSet(result));
        Assert.assertFalse(result.stream()
                .filter(constructor -> constructor.getDeclaringClass().getName()
                        .equals(CustomCharacterTest.class.getName())).findAny().isPresent());
    }

    public static class CustomCharacterTest extends AbstractCharacter {

        public CustomCharacterTest(AbstractCharacter incomingCharacter) {
            super(incomingCharacter);

            getMap().put("custom_key_test", "Custom character added");
        }
    }

}
