package com.dotcms.util;

import com.dotcms.api.web.HttpServletRequestThreadLocal;

import javax.servlet.http.HttpSession;
import java.util.Optional;

public final class TimeMachineUtil {

    private TimeMachineUtil(){}

    /**
     * If Time Machine is running return the timestamp of the Time Machine date
     * @return
     */
    public static Optional<String> getTimeMachineDate() {
        if (null == HttpServletRequestThreadLocal.INSTANCE.getRequest()) {
            return Optional.empty();
        }
        final HttpSession session = HttpServletRequestThreadLocal.INSTANCE.getRequest().getSession(false);
        final Object timeMachineObject = session != null ? session.getAttribute("tm_date") : null;
        return Optional.ofNullable(timeMachineObject != null ? timeMachineObject.toString() : null);
    }

    /**
     * Return true if Time Machine is running, otherwise return false
     * @return
     */
    public static boolean isRunning(){
        final Optional<String> timeMachine = getTimeMachineDate();
        return timeMachine.isPresent();
    }
}
