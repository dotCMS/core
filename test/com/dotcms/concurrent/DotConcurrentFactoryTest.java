package com.dotcms.concurrent;

import com.dotcms.cms.login.LoginService;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.authentication.AuthenticationForm;
import com.dotcms.rest.api.v1.authentication.AuthenticationHelper;
import com.dotcms.rest.api.v1.authentication.AuthenticationResource;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.json.JSONException;
import com.liferay.portal.*;
import com.liferay.portal.auth.AuthException;
import com.liferay.portal.ejb.UserLocalManager;
import com.liferay.portal.model.User;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.stream.IntStream;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DotConcurrentFactoryTest {


    public DotConcurrentFactoryTest() {

	}

    @Test
    public void testDefaultOne() throws JSONException{

        final DotConcurrentFactory dotConcurrentFactory =
                DotConcurrentFactory.getInstance();

        final DotSubmitter submitter =
                dotConcurrentFactory.getSubmitter();

        System.out.println(submitter);

        IntStream.range(0, 40).forEach(
                n -> {

                    if (n % 10 == 0) {

                        System.out.println(submitter);
                    }
                    submitter.execute(new PrintTask("Thread" + n));
                }
        );

        //check active thread, if zero then shut down the thread pool
        for (;;) {
            int count = submitter.getActiveCount();
            System.out.println("Active Threads : " + count);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (count == 0) {
                //submitter.shutdown();
                break;
            }
        }

        System.out.print("Staring a new one submitter");

        final DotSubmitter submitter2 =
                dotConcurrentFactory.getSubmitter();

        System.out.println(submitter2);

        assertTrue(submitter == submitter2);

        IntStream.range(0, 20).forEach(
                n -> {
                    submitter2.execute(new PrintTask("Thread" + n));
                }
        );

        //check active thread, if zero then shut down the thread pool
        for (;;) {
            int count = submitter2.getActiveCount();
            System.out.println("Active Threads : " + count);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (count == 0) {
                submitter2.shutdown();
                break;
            }
        }

    }


    class PrintTask implements Runnable {

        String name;

        public PrintTask(String name){
            this.name = name;
        }

        @Override
        public void run() {

            System.out.println(name + " is running");

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println(name + " is running");
        }

    }
}
