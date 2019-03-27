package com.dotmarketing.util;

import com.liferay.portal.model.User;
import com.liferay.portal.util.WebAppPool;
import org.apache.struts.Globals;
import org.apache.struts.util.MessageResources;
import org.apache.struts.util.MessageResourcesFactory;
import org.apache.struts.util.PropertyMessageResources;
import org.apache.struts.util.PropertyMessageResourcesFactory;

/**
 * THis base class is useful just to load base things for other unit test
 *
 * @author jsanca
 */
public class BaseMessageResources {

    protected void initMessages() {

        final PropertyMessageResourcesFactory multiMessageResourcesFactory =
                new PropertyMessageResourcesFactory();

        final MessageResourcesFactory factory =
                MessageResourcesFactory.createFactory();

        final MessageResources messageResources =
                new PropertyMessageResources(factory,
                        "messages.Language");

        messageResources.setReturnNull(true);

        WebAppPool.put(User.DEFAULT, Globals.MESSAGES_KEY, messageResources);
    }

} // BaseMessageResources.
