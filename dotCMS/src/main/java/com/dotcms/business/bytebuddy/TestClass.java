package com.dotcms.business.bytebuddy;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.util.LogTime;
import com.dotmarketing.util.Logger;


public class TestClass {

    @LogTime(loggingLevel="DEBUG")
    public String timeTest() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "finished time task";
    }

    @WrapInTransaction
    public String transTest() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
      return "Finished transTest2";
    }

    @WrapInTransaction
    public void transTest2(String parameter) {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Finished transTest2");
    }
}
