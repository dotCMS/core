package com.dotcms.util;

import com.dotcms.api.web.HttpServletRequestThreadLocal;

import javax.servlet.http.HttpSession;
import java.util.Optional;

public class TimeMachineUtil {

    private TimeMachineUtil(){}

    public static Optional<String> getTimeMachineDate() {
        final HttpSession session = HttpServletRequestThreadLocal.INSTANCE.getRequest().getSession();
        final Object timeMachineObject = session != null ? session.getAttribute("tm_date") : null;
        return Optional.ofNullable(timeMachineObject != null ? timeMachineObject.toString() : null);
    }

    public static boolean isRunning(){
        final Optional<String> timeMachine = getTimeMachineDate();
        return timeMachine.isPresent();
    }
}
