package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.response.BaseResponse;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.DNSUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.Mailer;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Same of the {@link EmailActionlet} but runs asynchronously.
 * @author jsanca
 */
public class AsyncEmailActionlet extends EmailActionlet {

    private static final long serialVersionUID = 1L;

    protected final SystemMessageEventUtil systemMessageEventUtil =
            SystemMessageEventUtil.getInstance();


    @Override
    public String getName() {
        return "Async Send an Email";
    }

    @Override
    public String getHowTo() {
        return "This actionlet will send an email that can be based on the submitted content. The value of every field here is parsed velocity.  So, to send a custom email to the email address stored in a field called userEmail, put $content.userEmail in the 'to email' field and the system will replace it with the variables from the content";
    }

    @Override
    protected void sendEmail(final Mailer mail, final WorkflowProcessor processor) {
        DotConcurrentFactory.getInstance().getSubmitter().submit(()->{
            try {
                mail.sendMessage();
            } catch (Exception e) {

                final List<String> userList = new ArrayList<>();
                userList.add(processor.getUser().getUserId());
                this.systemMessageEventUtil.pushMessage(new SystemMessageBuilder().setMessage("Error sending the email: " + e.getMessage())
                        .setLife(5000)
                        .setSeverity(MessageSeverity.ERROR).create(), userList);
            }
        });
    }

}
