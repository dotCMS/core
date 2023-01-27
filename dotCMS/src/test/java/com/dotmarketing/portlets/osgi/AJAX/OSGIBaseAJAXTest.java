package com.dotmarketing.portlets.osgi.AJAX;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class OSGIBaseAJAXTest {

    Set<Class<? extends OSGIBaseAJAX>> classes = Set.of(OSGIAJAX.class);

    @Test
    public void TestAllowedCommands() throws NoSuchMethodException, ServletException, IOException, DotDataException {

        final String property = System.getProperty(WebKeys.OSGI_ENABLED);
        try {
            System.setProperty(WebKeys.OSGI_ENABLED, Boolean.TRUE.toString());
            final User user = mock(User.class);
            when(user.isBackendUser()).thenReturn(true);
            when(user.isAdmin()).thenReturn(true);

            for (Class<? extends OSGIBaseAJAX> clazz : classes) {
                final OSGIBaseAJAX action = mock(clazz);
                when(action.getUser()).thenReturn(user);
                when(action.doesUserHaveAccessToPortlet(user)).thenReturn(true);
                when(action.getAllowedCommands()).thenCallRealMethod();
                when(action.getMethod(anyString(), any())).thenCallRealMethod();
                doCallRealMethod().when(action).service(any(HttpServletRequest.class), any(HttpServletResponse.class));

                final List<String> commands = new ArrayList<>(action.getAllowedCommands());
                for (String command : commands) {

                    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
                    HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

                    when(request.getParameter(anyString())).thenAnswer(invocation -> {
                        final String arg = invocation.getArgument(0);
                        if ("cmd".equals(arg)) {
                            return command;
                        }
                        return null;
                    });
                    System.out.println(" Command ::  " + command + "  on  class " + action);
                    action.service(request, response);
                    verify(action, atLeastOnce()).getMethod(anyString(), any());
                }
            }
        } finally {
            if(null != property){
                System.setProperty(WebKeys.OSGI_ENABLED, property);
            }
        }
    }


    /**
     * Non-happy path: Call the action using a forbidden command. Nothing should get called
     * @throws NoSuchMethodException
     * @throws ServletException
     * @throws IOException
     */
    @Test
    public void TestNotAllowedCommands() throws NoSuchMethodException, ServletException, IOException, DotDataException {
        final String property = System.getProperty(WebKeys.OSGI_ENABLED);
        System.setProperty(WebKeys.OSGI_ENABLED, Boolean.TRUE.toString());
        try {
            final User user = mock(User.class);
            for (Class<? extends OSGIBaseAJAX> clazz : classes) {
                final OSGIBaseAJAX action = mock(clazz);
                when(action.getUser()).thenReturn(user);
                when(action.doesUserHaveAccessToPortlet(user)).thenReturn(true);
                when(action.getAllowedCommands()).thenCallRealMethod();
                when(action.getMethod(anyString(), any())).thenCallRealMethod();
                doCallRealMethod().when(action).service(any(HttpServletRequest.class), any(HttpServletResponse.class));

                final String command = "forbiddenMethod";

                final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
                final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

                when(request.getParameter(anyString())).thenAnswer(invocation -> {
                    final String arg = invocation.getArgument(0);
                    if ("cmd".equals(arg)) {
                        return command;
                    }
                    return null;
                });
                System.out.println(" Command ::  " + command + "  on  class " + action);
                action.service(request, response);
                verify(action, never()).getMethod(anyString(), any());
            }
        } finally {
            if(null != property){
                System.setProperty(WebKeys.OSGI_ENABLED, property);
            }
        }
    }


    /**
     *When Calling this controller with null command verify that it defaults to the method called action
     * @throws NoSuchMethodException
     * @throws ServletException
     * @throws IOException
     */
    @Test
    public void TestOnNullCallTheDefaultCommand() throws NoSuchMethodException, ServletException, IOException, DotDataException {
        final String property = System.getProperty(WebKeys.OSGI_ENABLED);
        System.setProperty(WebKeys.OSGI_ENABLED, Boolean.TRUE.toString());
        try {
            final User user = mock(User.class);
            for (Class<? extends OSGIBaseAJAX> clazz : classes) {
                final OSGIBaseAJAX action = mock(clazz);
                when(action.getUser()).thenReturn(user);
                when(action.doesUserHaveAccessToPortlet(user)).thenReturn(true);
                when(action.getAllowedCommands()).thenCallRealMethod();
                when(action.getMethod(anyString(), any())).thenCallRealMethod();
                doCallRealMethod().when(action).service(any(HttpServletRequest.class), any(HttpServletResponse.class));

                final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
                final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

                when(request.getParameter(anyString())).thenAnswer(invocation -> {
                    return null;
                });
                action.service(request, response);
                verify(action, atLeastOnce()).getMethod(eq("action"), any());

            }
        } finally {
            if(null != property){
              System.setProperty(WebKeys.OSGI_ENABLED, property);
            }
        }
    }


}