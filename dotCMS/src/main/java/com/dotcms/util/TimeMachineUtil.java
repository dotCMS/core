package com.dotcms.util;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.rest.api.v1.page.PageResource;
import java.util.Date;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public final class TimeMachineUtil {

    private TimeMachineUtil(){}

    /**
     * If Time Machine is running return the timestamp of the Time Machine date
     * Running Time Machine is determined by the presence of the attribute PageResource.TM_DATE in the request or session
     * @return Optional<String>
     */
    public static Optional<String> getTimeMachineDate() {
        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        if (null == request) {
            return Optional.empty();
        }
        final HttpSession session = request.getSession(false);
        Object timeMachineObject = session != null ? session.getAttribute(PageResource.TM_DATE) : null;
        if(null != timeMachineObject){
            return Optional.of(timeMachineObject.toString());
        }
        timeMachineObject = request.getAttribute(PageResource.TM_DATE);
        return Optional.ofNullable(timeMachineObject != null ? timeMachineObject.toString() : null);
    }

    /**
     * If Time Machine is running return the timestamp of the Time Machine date as a Date
     * @return Optional<Date>
     */
    public static Optional<Date> getTimeMachineDateAsDate() {
        final Optional<String> timeMachine = getTimeMachineDate();
        if (timeMachine.isPresent()) {
            final String tmDate = timeMachine.get();
            return Optional.of(new Date(Long.parseLong(tmDate)));
        }
        return Optional.empty();
    }

    /**
     * Return true if Time Machine is running, otherwise return false
     * @return boolean
     */
    public static boolean isRunning(){
        final Optional<String> timeMachine = getTimeMachineDate();
        return timeMachine.isPresent();
    }

    /**
     * Return true if Time Machine is not running, otherwise return false
     * @return boolean
     */
    public static boolean isNotRunning(){
        return !isRunning();
    }
}
