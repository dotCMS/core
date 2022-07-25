package com.dotcms.rest.api.v1.experiment;

import java.util.Map;

public class ClickAnalyticEventTypeHandler implements AnalyticEventTypeHandler{

    private final String JS_CODE  = "console.log('Listener click events in', ${query_selector});\n"
            + "        var elements = document.querySelectorAll('${query_selector}');\n"
            + "        \n"
            + "        for (var i = 0; i < elements.length; i++){\n"
            + "            console.log(\"element\", elements[i]);\n"
            + "            as[i].addEventListener('click', (event) => {\n"
            + "                var target = event.target;\n"
            + "                jitsu('track', 'click', {\n"
            + "                  target: {\n"
            + "                    name: event.target.name,\n"
            + "                    class: event.target.classList,\n"
            + "                    id: event.target.id,\n"
            + "                    tag: event.target.tagName,\n"
            + "                  },\n"
            + "                });\n"
            + "            });\n"
            + "        }\n"
            + "    }\n";

    @Override
    public String getJSCode(final Map<String, String> parameters) {
        return JS_CODE.replaceAll("\\$\\{query_selector\\}", parameters.get("query_selector"));
    }
}
