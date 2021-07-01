package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.BaseResponse;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import org.apache.velocity.context.Context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This Actionlet allows to the user to move a contentlet to another place
 * The user may set a path into the actionlet params, if the hosts exists but not the path it can be created.
 * @author jsanca
 */
public class MoveContentActionlet extends WorkFlowActionlet {


    /**
     * This is the parameter for the Actionlet
     */
    public static final String PATH_KEY = "path";

    /**
     * This is the parameter if want to override the path from the contentlet map properties
     */
    public static final String CONTENTLET_PATH_KEY = Contentlet.PATH_TO_MOVE;

    @Override
    public List<WorkflowActionletParameter> getParameters() {
        List<WorkflowActionletParameter> params = new ArrayList<>();

        params.add(new WorkflowActionletParameter("path", "Optional path to move, for example: //demo.dotcms.com/application", "", false));

        return params;
    }

    @Override
    public String getName() {
        return "Move";
    }

    @Override
    public String getHowTo() {
        return "If the path is not set on this actionlet, dotCMS will allow a user to select a destination";
    }

    @Override
    public void executeAction(final WorkflowProcessor processor,
                              final Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {

        final Contentlet contentlet = processor.getContentlet();
        final User user             = processor.getUser();
        final String pathParam      = params.get(PATH_KEY).getValue();
        final String path           = findFolderIdByPath(pathParam, contentlet, processor);

        Logger.debug(this, "Moving the contentlet to: " + path);

        processor.setContentlet(Try.of(()->APILocator.getContentletAPI().move(contentlet, user, path, false))
                .getOrElseThrow(e -> new WorkflowActionFailureException(e.getMessage(), (Exception) e)));
    }


    private String findFolderIdByPath (final String actionletPathParameter,
                                       final Contentlet contentlet,
                                       final WorkflowProcessor processor)  {

        return  UtilMethods.isSet(actionletPathParameter)?
                this.evalVelocity(processor, actionletPathParameter): //helps to eval things such as ${contentlet.hostName}/trash
                contentlet.getStringProperty(CONTENTLET_PATH_KEY);
    }

    protected String evalVelocity(final WorkflowProcessor processor, final String velocityMessage) {

        final User currentUser = processor.getUser();
        final HttpServletRequest request =
                null == HttpServletRequestThreadLocal.INSTANCE.getRequest()?
                        this.mockRequest(currentUser): HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final HttpServletResponse response =
                null == HttpServletResponseThreadLocal.INSTANCE.getResponse()?
                        this.mockResponse(): HttpServletResponseThreadLocal.INSTANCE.getResponse();

        final Context velocityContext = VelocityUtil.getInstance().getContext(request, response);
        velocityContext.put("workflow",   processor);
        velocityContext.put("user",       currentUser);
        velocityContext.put("contentlet", processor.getContentlet());
        velocityContext.put("content",    processor.getContentlet());

        try {
            return VelocityUtil.eval(velocityMessage, velocityContext);
        } catch (Exception e1) {
            Logger.warn(this.getClass(), "unable to parse message, falling back" + e1);
        }

        return velocityMessage;
    }

    protected HttpServletRequest  mockRequest (final User  currentUser) {

        final Host host = Try.of(()->APILocator.getHostAPI()
                .findDefaultHost(currentUser, false)).getOrElse(APILocator.systemHost());
        return new MockAttributeRequest(
                new MockSessionRequest(
                        new FakeHttpRequest(host.getHostname(), StringPool.FORWARD_SLASH).request()
                ).request()
        ).request();
    }

    protected HttpServletResponse mockResponse () {

        return new BaseResponse().response();
    }
}
