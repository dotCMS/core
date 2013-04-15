package com.dotmarketing.osgi.custom.spring;

import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.velocity.VelocityServlet;
import org.apache.velocity.VelocityContext;
import org.springframework.web.servlet.View;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * Copy of the dotCMS View {@link com.dotcms.spring.web.DotView} in order
 * to use our custom spring version and not the one use it by dotCMS.
 *
 * @author Jonathan Gamba
 *         Date: 4/15/13
 * @see com.dotcms.spring.web.DotView
 */
public class CustomView implements View {

    String pagePath;

    public CustomView ( String pagePath ) {
        super();
        this.pagePath = pagePath;
    }


    public String getContentType () {
        return null;
    }

    public void render ( Map<String, ?> map, HttpServletRequest request, HttpServletResponse response ) throws Exception {

        // get the VelocityContext
        VelocityContext ctx = VelocityUtil.getWebContext( request, response );

        if ( !pagePath.startsWith( "redirect:" ) ) {
            // add the Spring map to the context
            for ( String x : map.keySet() ) {
                ctx.put( x, map.get( x ) );
            }

            // add the context to the request.attr where it will be picked up and used by the VelocityServlet
            request.setAttribute( VelocityServlet.VELOCITY_CONTEXT, ctx );

            request.getRequestDispatcher( pagePath ).forward( request, response );
        } else {
            pagePath = pagePath.replaceFirst( "redirect:", "" );
            response.sendRedirect( pagePath );
        }
    }

}