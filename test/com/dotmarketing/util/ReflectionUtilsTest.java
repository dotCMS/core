package com.dotmarketing.util;


import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

import java.io.IOException;

/**
 * ReflectionUtils Test
 * @author jsanca
 */
public class ReflectionUtilsTest {



    /**
     * Testing the new Instance
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     * @throws InterruptedException
     */
    //@Test
    public void newInstanceTest()  {

        final ReflectionTestBean bean =
                ReflectionUtils.newInstance
                        (ReflectionTestBean.class, "Test Name");

        System.out.println(bean);
    }

    public static void main(String [] args)
    {
        new ReflectionUtilsTest().newInstanceTest();
    }
}
