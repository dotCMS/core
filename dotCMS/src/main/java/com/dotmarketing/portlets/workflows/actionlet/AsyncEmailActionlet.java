package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Mailer;

import java.util.ArrayList;
import java.util.List;

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
