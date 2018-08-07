package com.dotmarketing.portlets.contentlet.model;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.ResourceLink.ResourceLinkBuilder;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.liferay.portal.model.User;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.junit.Test;

public class ResourceLinkTest {

    @Test
    public void testRestrictedLink() throws Exception{

        final User adminUser = mock(User.class);
        when(adminUser.getUserId()).thenReturn("dotcms.org.1");
        when(adminUser.getEmailAddress()).thenReturn("admin@dotcms.com");
        when(adminUser.getFirstName()).thenReturn("Admin");
        when(adminUser.getLastName()).thenReturn("User");

        final Contentlet contentlet = mock(Contentlet.class);
        final Map<String,Object> map = new HashMap<>();
        map.put(FileAssetAPI.FILE_NAME_FIELD,"comments.scss");
        map.put(Contentlet.LANGUAGEID_KEY,"1");

        when(contentlet.getMap()).thenReturn(map);

        final HttpServletRequest request = mock(HttpServletRequest.class);

        final ResourceLinkBuilder resourceLinkBuilder = new ResourceLink.ResourceLinkBuilder(){

            @Override
            Identifier getIdentifier(final Contentlet contentlet) throws DotDataException {
                final Identifier identifier = mock(Identifier.class);
                when(identifier.getInode()).thenReturn("83864b2c-3988-4acc-953d-ff8d0ba5e093");
                return identifier;
            }

            @Override
            FileAsset getFileAsset(final Contentlet contentlet){
                FileAsset fileAsset = mock(FileAsset.class);
                return fileAsset;
            }

        };

        final ResourceLink link = resourceLinkBuilder.build(request, adminUser, contentlet);

    }

}
