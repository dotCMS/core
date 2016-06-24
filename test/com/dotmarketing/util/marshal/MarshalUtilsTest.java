package com.dotmarketing.util.marshal;


import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.ReflectionTestBean;
import com.dotmarketing.util.ReflectionUtils;
import org.junit.Test;

import static  org.junit.Assert.*;

import java.io.IOException;

/**
 * ReflectionUtils Test
 * @author jsanca
 */

public class MarshalUtilsTest {



    /**
     * Testing the new Instance
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void newInstanceTest()  {

        final Object o1 =
                ReflectionUtils.newInstance
                        ((String)null);

        assertNull(o1);

        final Object o2 =
                ReflectionUtils.newInstance
                        ("com.doesnotexists.NoExistingClass");

        assertNull(o2);

        final Object o3 =
                ReflectionUtils.newInstance
                        ("com.dotmarketing.util.ReflectionUtilsTest");

        assertNotNull(o3);

        assertTrue(o3 instanceof MarshalUtilsTest);

        final Object o4 =
                ReflectionUtils.newInstance((Class)null);

        assertNull(o4);

        final MarshalUtilsTest reflectionUtilsTest =
                ReflectionUtils.newInstance(MarshalUtilsTest.class);

        assertNotNull(reflectionUtilsTest);


        final ReflectionTestBean bean =
                ReflectionUtils.newInstance
                        (ReflectionTestBean.class, "Test Name");

        assertNotNull(bean);
        assertEquals("Test Name", bean.toString());

        final ReflectionTestBean bean2 =
                ReflectionUtils.newInstance
                        (ReflectionTestBean.class, "Test Name", "Wrong Parameter");

        assertNull(bean2);

        Class<?> [] types = ReflectionUtils.getTypes(new Integer(2), "Hello", Float.MAX_VALUE);

        assertNotNull(types);
        assertTrue(types.length == 3);
        assertEquals(Integer.class, types[0]);
        assertEquals(String.class, types[1]);
        assertEquals(Float.class, types[2]);

    }

}
