package  com.dotcms.api.web;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;
import com.dotmarketing.util.Logger;


public class RequestThreadLocalListener implements ServletRequestListener {

    
    
    public RequestThreadLocalListener() {
        Logger.info(this.getClass(), "Starting RequestThreadLocalListener");
    }

	public void requestDestroyed(ServletRequestEvent requestEvent) {
	    HttpServletRequestThreadLocal.INSTANCE.setRequest(null);

	}


	public void requestInitialized(final ServletRequestEvent requestEvent) {
	    if(requestEvent.getServletRequest() instanceof HttpServletRequest) {
	        HttpServletRequestThreadLocal.INSTANCE.setRequest((HttpServletRequest) requestEvent.getServletRequest());
	    }
		
	}

}
