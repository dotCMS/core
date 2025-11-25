package com.dotmarketing.portlets.workflows.ajax;

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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class WfBaseActionTest {

    Set<Class<? extends WfBaseAction>> classes = Set.of(
            WfActionClassAjax.class,
            WfRoleStoreAjax.class,
            WfActionAjax.class,
            WfSchemeAjax.class,
            WfTaskAjax.class
    );

    /**
     * Happy path:  Test That the allowed methods might get called when a white-listed command gets passed
     * @throws NoSuchMethodException
     * @throws ServletException
     * @throws IOException
     */
    @Test
    public void TestAllowedCommands() throws NoSuchMethodException, ServletException, IOException {

        final User user = mock(User.class);
        for (Class<? extends WfBaseAction> clazz: classes) {
            final WfBaseAction action = mock(clazz);
            when(action.getUser()).thenReturn(user);
            when( action.getAllowedCommands()).thenCallRealMethod();
            when( action.getMethod(anyString(),any(),any())).thenCallRealMethod();
            doCallRealMethod().when(action).service(any(HttpServletRequest.class),any(HttpServletResponse.class));

            final List<String> commands = new ArrayList<>(action.getAllowedCommands());
            for (String command : commands) {

                HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
                HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

                when(request.getParameter(anyString())).thenAnswer(invocation -> {
                    final String arg = invocation.getArgument(0);
                    if("cmd".equals(arg)){
                        return command;
                    }
                    return null;
                });
                System.out.println(" Command ::  "+ command  + "  on  class " + action  );
                action.service(request, response);
                verify(action, atLeastOnce()).getMethod(anyString(),any(),any());
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
    public void TestNotAllowedCommands() throws NoSuchMethodException, ServletException, IOException {

        final User user = mock(User.class);
        for (Class<? extends WfBaseAction> clazz: classes) {
            final WfBaseAction action = mock(clazz);
            when(action.getUser()).thenReturn(user);
            when( action.getAllowedCommands()).thenCallRealMethod();
            when( action.getMethod(anyString(),eq(new Class[] { HttpServletRequest.class, HttpServletResponse.class }))).thenCallRealMethod();
            doCallRealMethod().when(action).service(any(HttpServletRequest.class),any(HttpServletResponse.class));

                final String command = "forbiddenMethod";

                final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
                final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

                when(request.getParameter(anyString())).thenAnswer(invocation -> {
                    final String arg = invocation.getArgument(0);
                    if("cmd".equals(arg)){
                        return command;
                    }
                    return null;
                });
                System.out.println(" Command ::  "+ command  + "  on  class " + action  );
                action.service(request, response);
                verify(action, never()).getMethod(anyString(),any());

        }
    }


    /**
     *When Calling this controller with null command verify that it defaults to the method called action
     * @throws NoSuchMethodException
     * @throws ServletException
     * @throws IOException
     */
    @Test
    public void TestOnNullCallTheDefaultCommand() throws NoSuchMethodException, ServletException, IOException {

        final User user = mock(User.class);
        for (Class<? extends WfBaseAction> clazz: classes) {
            final WfBaseAction action = mock(clazz);
            when(action.getUser()).thenReturn(user);
            when( action.getAllowedCommands()).thenCallRealMethod();
            when( action.getMethod(anyString(),any(),any())).thenCallRealMethod();
            doCallRealMethod().when(action).service(any(HttpServletRequest.class),any(HttpServletResponse.class));

            final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
            final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

            when(request.getParameter(anyString())).thenAnswer(invocation -> {
                return null;
            });
            action.service(request, response);
            verify(action, atLeastOnce()).getMethod(eq("action"),any(),any());

        }
    }

}