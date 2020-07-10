package com.dotcms.util;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.enterprise.rules.RulesAPIImpl;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.util.Date;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TimeMachineUtilTest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to Test: {@link TimeMachineUtil#getTimeMachineDate()}
     * When: When Time Machine is not running
     * Should: Return a {@link Optional#empty()}
     */
    @Test
    public void shouldReturnOptionalEmptyWhenTimeMachineIsNotRunning(){
        final HttpSession session = mock(HttpSession.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession()).thenReturn(session);

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);


        final Optional<String> timeMachineDate = TimeMachineUtil.getTimeMachineDate();
        assertFalse(timeMachineDate.isPresent());
    }

    /**
     * Method to Test: {@link TimeMachineUtil#getTimeMachineDate()}
     * When: When Time Machine is running
     * Should: return the date in a {@link Optional}
     */
    @Test
    public void shouldReturnOptionalWithValueWhenTimeMachineIsRunning(){
        final HttpSession session = mock(HttpSession.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession()).thenReturn(session);

        final String time = Long.toString(new Date().getTime());

        when(session.getAttribute("tm_date")).thenReturn(time);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        final Optional<String> timeMachineDate = TimeMachineUtil.getTimeMachineDate();
        assertTrue(timeMachineDate.isPresent());
        assertEquals(time, timeMachineDate.get());
    }

    /**
     * Method to Test: {@link TimeMachineUtil#isRunning()}
     * When: When Time Machine is not running
     * Should: Return a false
     */
    @Test
    public void whenTimeMachineIsNotRunningShouldReturnFalse(){
        final HttpSession session = mock(HttpSession.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession()).thenReturn(session);

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        assertFalse(TimeMachineUtil.isRunning());
    }

    /**
     * Method to Test: {@link TimeMachineUtil#getTimeMachineDate()}
     * When: When Time Machine is running
     * Should: return true
     */
    @Test
    public void whenTimeMachineIsNotRunningShouldReturnTrue(){
        final HttpSession session = mock(HttpSession.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession()).thenReturn(session);

        final String time = Long.toString(new Date().getTime());

        when(session.getAttribute("tm_date")).thenReturn(time);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        assertTrue(TimeMachineUtil.isRunning());
    }
}
