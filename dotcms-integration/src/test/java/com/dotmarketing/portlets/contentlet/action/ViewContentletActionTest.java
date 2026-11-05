package com.dotmarketing.portlets.contentlet.action;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.liferay.portlet.RenderRequestImpl;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link ViewContentletAction}.
 *
 * @author hassandotcms
 */
public class ViewContentletActionTest {

    /** Name of the session attribute the legacy edit screen used for its uploaded files. */
    private static final String LEGACY_TEMP_BINARY_LIST_ATTRIBUTE = "tempBinaryImageInodesList";

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * <b>Method to Test:</b> {@link ViewContentletAction#_viewContentlets(com.dotcms.repackage.javax.portlet.RenderRequest, User)}<p>
     * <b>When:</b> the Content Search portlet is rendered for a user without permissions on a
     * file asset, and the user's session holds the inode of that file asset under
     * {@code tempBinaryImageInodesList}<p>
     * <b>Should:</b> leave the file asset in place
     */
    @Test
    public void viewContentlets_leavesContentInPlace() throws Exception {

        final User user = new UserDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();
        final Contentlet fileAsset = new FileAssetDataGen(folder,
                "file asset body " + System.currentTimeMillis()).nextPersisted();

        try {
            final HttpSession session = mock(HttpSession.class);
            when(session.getAttribute(LEGACY_TEMP_BINARY_LIST_ATTRIBUTE))
                    .thenReturn(new ArrayList<>(List.of(fileAsset.getInode())));

            final HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getSession()).thenReturn(session);
            when(request.getSession(false)).thenReturn(session);
            when(request.getAttribute(WebKeys.USER)).thenReturn(user);
            when(request.getAttribute("initParams")).thenReturn(new HashMap<String, String>());
            when(request.getParameter("p_p_id")).thenReturn("content");

            final RenderRequestImpl renderRequest = mock(RenderRequestImpl.class);
            when(renderRequest.getHttpServletRequest()).thenReturn(request);

            new ViewContentletAction()._viewContentlets(renderRequest, user);

            assertNotNull("The file asset should still exist", Try.of(() -> APILocator
                    .getContentletAPI().find(fileAsset.getInode(), APILocator.systemUser(), false))
                    .getOrNull());
        } finally {
            FolderDataGen.remove(folder);
            UserDataGen.remove(user);
        }
    }
}
