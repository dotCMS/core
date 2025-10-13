package com.dotcms.cost;

import com.dotmarketing.business.APILocator;
import com.liferay.util.Xss;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public class RequestCostReport {


    private final RequestCostApi requestCostApi = APILocator.getRequestCostAPI();

    String writeAccounting(HttpServletRequest request) {
        List<Map<String, Object>> accounting = requestCostApi.getAccountList(request);
        StringWriter sw = new StringWriter();
        sw.append("<html>\n<title>Request Accounting</title>\n<body>");
        sw.append("<h2>\ndotCMS Request Accounting: " + Xss.escapeHTMLAttrib(request.getRequestURI()) + "\n</h2>\n");
        sw.append("<table border=1 style=\"width:90%;\">\n");
        int totalCost = 0;
        int i = 0;
        for (Map<String, Object> map : accounting) {
            int cost = (int) map.getOrDefault(requestCostApi.COST, 0);
            totalCost += cost;

            String className = ((Class) map.getOrDefault(RequestCostApi.CLASS, Object.class)).getSimpleName();
            String methodName = (String) map.getOrDefault(RequestCostApi.METHOD, "unknown");
            Object[] args = (Object[]) map.getOrDefault(RequestCostApi.ARGS, new Object[0]);

            sw.append("<tr>\n")
                    .append("<td>" + ++i + "</td>")
                    .append("<td>")
                    .append(className)
                    .append("</td>")
                    .append("<td>")
                    .append(methodName)
                    .append("</td>")
                    .append("<td>");
            for (Object obj : args) {
                sw.append(obj != null ? obj.toString() : "");
                sw.append("<br>\n");
            }
            sw.append("</td>")
                    .append("<td>")
                    .append(String.valueOf(cost))
                    .append("</td>")
                    .append("</tr>\n");
        }
        sw.append("<tr>"
                + "<td></td>"
                + "<td></td>"
                + "<td></td>"
                + "<td align='right'>Total:</td>"
                + "<td>" + totalCost + "</td>"
                + "</tr>\n");
        sw.append("</table>\n");
        sw.append("</body>\n</html>\n");

        return
                sw.toString();


    }


}
