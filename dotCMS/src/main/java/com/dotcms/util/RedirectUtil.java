package com.dotcms.util;

import java.io.StringWriter;
import javax.servlet.http.HttpServletResponse;
import com.dotcms.auth.providers.saml.v1.DotSamlResource;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;


public class RedirectUtil {

    final static String redirectTemplate =
                        new StringWriter()
                        .append("<html>")
                        .append("<head>")
                        .append("<meta http-equiv=\"refresh\" content=\"0;URL='REDIRECT_ME'\"/>")
                        .append("<style>p {font-family: Arial;font-size: 16px;color: #666;margin: 50px;text-align:center;opacity: 1;animation: fadeIn ease 5s;animation-iteration-count: 0;-webkit-animation: fadeIn ease 5s;}@keyframes fadeIn {0% {opacity:0;}100% {opacity:1;}}@-moz-keyframes fadeIn {0% {opacity:0;}100% {opacity:1;}}@-webkit-keyframes fadeIn {0% {opacity:0;}100% {opacity:1;}}@-o-keyframes fadeIn {0% {opacity:0;}100% {opacity:1;}@-ms-keyframes fadeIn {0% {opacity:0;}100% {opacity:1;}}</style>")
                        .append("</head>")
                        .append("<body><p>If your browser does not refresh, click <a href=\"REDIRECT_ME\">Here</a>.</p></body>")
                        .append("</html>")
                        .toString();
                    
    
    
    
    public static void sendRedirectHTML(HttpServletResponse response, final String redirectUrl) {
        
        final String finalTemplate = UtilMethods.replace(redirectTemplate,"REDIRECT_ME", redirectUrl);
        
        response.setContentType("text/html");
        Try.run(() -> {
            response.getWriter().write(finalTemplate);
            response.getWriter().flush();
        }).onFailure(e->Logger.warn(RedirectUtil.class,"Unable to redirect to :" + redirectUrl+ " cause:"+e.getMessage()));
        

        
    }
    


} 


