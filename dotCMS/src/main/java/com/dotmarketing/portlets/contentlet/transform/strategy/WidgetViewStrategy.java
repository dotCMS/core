package com.dotmarketing.portlets.contentlet.transform.strategy;

import com.dotcms.api.APIProvider;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.contenttype.model.type.WidgetContentType;
import com.dotcms.mock.request.*;
import com.dotcms.mock.response.MockHttpCaptureResponse;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.repackage.org.directwebremoting.util.FakeHttpServletRequest;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Set;

/**
 * Strategy to handle Contentlets of type Widget. It simply adds a new field to the map called
 * `widgetCodeJSON` which in case of having a $dotJSON.put will parse the field as JSON
 */
public class WidgetViewStrategy extends WebAssetStrategy<Contentlet> {

    /**
     * Main constructor
     * @param toolBox
     */
    WidgetViewStrategy(final APIProvider toolBox) {
        super(toolBox);
    }

    /**
     * Transform main method - entry point
     * @param widget
     * @param map
     * @param options
     * @param user
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Override
    protected Map<String, Object> transform(final Contentlet widget, final Map<String, Object> map,
            final Set<TransformOptions> options, final User user)
            throws DotSecurityException, DotDataException {


        // short circuit if this transformer has already been run
        if(map.containsKey(WidgetContentType.WIDGET_CODE_JSON_FIELD_VAR)){
            return map;
        }

        final String fieldValue = (String) map.get(WidgetContentType.WIDGET_CODE_FIELD_VAR);

        // do not allow the real request/response to be modified here.
        HttpServletRequest request =  HttpServletRequestThreadLocal.INSTANCE.getRequest();
        HttpServletResponse response = HttpServletResponseThreadLocal.INSTANCE.getResponse();

        HttpServletRequest fakeRequest = request == null
                ? new FakeHttpRequest("localhost", "/").request()
                : new MockHeaderRequest(new MockAttributeRequest(new MockSessionRequest(new MockParameterRequest(request))));;

        HttpServletResponse fakeResponse = new MockHttpResponse();


        final Object parsedValue = RenderFieldStrategy.parseAsJSON(fakeRequest, fakeResponse,
                fieldValue, widget, WidgetContentType.WIDGET_CODE_FIELD_VAR);


        map.put(WidgetContentType.WIDGET_CODE_JSON_FIELD_VAR, parsedValue);



        return map;
    }


}
