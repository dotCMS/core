package com.dotcms.datacreator.creators;

import com.dotcms.datacreator.DataCreatorCommand;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;

import java.util.HashMap;
import java.util.Map;

public class Create1PageContentletsDataCreatorCommand implements DataCreatorCommand {

    @Override
    public void execute(final Map<String, Object> contextMap) {

        final Map<String, Object> identifierMap = new HashMap<>();
        final User user = APILocator.systemUser();

        final Host site;
        try {
            site = APILocator.getHostAPI().findDefaultHost(user, false);
            Template template = APILocator.getTemplateAPI().systemTemplate();
            identifierMap.put("pageid", new HTMLPageDataGen(site, template).nextPersisted().getIdentifier());

            contextMap.put(this.getName(), identifierMap);
        } catch (Exception e) {

            throw new RuntimeException(e);
        }

    }
}
