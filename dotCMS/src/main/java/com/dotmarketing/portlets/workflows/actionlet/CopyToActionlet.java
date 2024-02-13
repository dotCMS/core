package com.dotmarketing.portlets.workflows.actionlet;


import com.dotcms.business.WrapInTransaction;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.util.ContentletUtil;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.workflows.actionlet.event.CopyActionletEvent;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.HostUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Makes the copy and move the contentlet to another site/folder
 * @author jsanca
 */
public class CopyToActionlet extends WorkFlowActionlet {

    private static final long serialVersionUID              = 1L;
    public static final String NOTIFY_SYNC_COPY_EVENT       = "notify.sync.copyto.event";
    private final ContentletAPI contentletAPI               = APILocator.getContentletAPI();
    private final LocalSystemEventsAPI localSystemEventsAPI = APILocator.getLocalSystemEventsAPI();

    public static final String CONTENTLET_PATH_KEY = Contentlet.PATH_TO_MOVE;

    @Override
    public List<WorkflowActionletParameter> getParameters() {
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return "Copy To Contentlet";
    }

    @Override
    public String getHowTo() {
        return "This workflow actionlet copies the edited contentlet and gives the user the option to move it to another site/folder.";
    }

    @Override
    public void executeAction(final WorkflowProcessor processor,
                    final Map<String, WorkflowActionClassParameter> params)
                    throws WorkflowActionFailureException {

        try {

            final Contentlet contentlet = processor.getContentlet();
            final User user             = processor.getUser();
            final String path           = contentlet.getStringProperty(CONTENTLET_PATH_KEY);
            this.performCopyTo(contentlet, user, path);
        } catch (DotContentletStateException | DotDataException | DotSecurityException | IOException e) {

            Logger.error(this, e.getMessage(), e);
            throw new WorkflowActionFailureException(e.getMessage(), e);
        }
    } // executeAction.

    @WrapInTransaction
    private void performCopyTo(final Contentlet contentletToCopy,
                               final User user, final String path) throws DotDataException, DotSecurityException, IOException {

        Logger.debug(this, () -> "Copying to contentlet: " + contentletToCopy.getIdentifier() + " to: "
                + path);

         final Contentlet copyContentlet = UtilMethods.isNotSet(path)?
                 // if not path set, perform the normal copy
                 copyContentletNormal(contentletToCopy, user):
                 // performing the copy to
                 copyContentletTo(contentletToCopy, user, path);

        if (null != copyContentlet) {

            if (contentletToCopy.getMap().containsKey(NOTIFY_SYNC_COPY_EVENT) && contentletToCopy.getBoolProperty(NOTIFY_SYNC_COPY_EVENT)) { // for testing

                this.localSystemEventsAPI.notify(new CopyActionletEvent(contentletToCopy, copyContentlet));
            } else {
                this.localSystemEventsAPI.asyncNotify(new CopyActionletEvent(contentletToCopy, copyContentlet));
            }
        }
    } // performCopyTo.

    private Contentlet copyContentletNormal(final Contentlet contentletToCopy, final User user)
            throws DotDataException, DotSecurityException {

        Logger.debug(this, ()-> "Path is not set, performing the normal copy");
        return this.contentletAPI.copyContentlet(contentletToCopy, user, false);
    }
    private Contentlet copyContentletTo(final Contentlet contentletToCopy, final User user, final String path)
            throws DotDataException, DotSecurityException {

        // split the path in site + folder
        final Tuple2<String, Host> hostPathTuple = Try.of(
                () -> HostUtil.splitPathHost(path, user,
                        StringPool.FORWARD_SLASH)).getOrElseThrow(e -> new DotRuntimeException(e));

        Logger.debug(this, ()-> "Retrieving from path: " + path + ", the host: "
                + hostPathTuple._2() + " and the folder: " + hostPathTuple._1());

        // if the site on the path is valid take it, otherwise take the site from the contentlet
        final Host site = Objects.nonNull(hostPathTuple._2())?
                hostPathTuple._2(): APILocator.getHostAPI().find(contentletToCopy.getHost(), user, false);
        final String folderPathParam = Objects.nonNull(hostPathTuple._1())? hostPathTuple._1(): StringPool.SLASH; // root by default
        final String folderPath = folderPathParam.endsWith(StringPool.SLASH) ? folderPathParam : folderPathParam + StringPool.SLASH;

        Logger.debug(this, ()-> "Final folder path: " + folderPath + " and site: " + site.getHostname());

        //Check if the folder exists via Admin user, b/c user couldn't have VIEW Permissions over the folder
        final Folder folder = Try.of(() -> APILocator.getFolderAPI()
                        .findFolderByPath(folderPath, site, APILocator.systemUser(), false)).getOrNull();
        final String copySuffix = ContentletUtil.generateCopySuffix(contentletToCopy, site, folder);

        Logger.debug(this, ()-> "Copying to contentlet: " + contentletToCopy.getIdentifier() + " to: "
                + folderPath + " and site: " + site.getHostname() + " with suffix: " + copySuffix);

        return this.contentletAPI.copyContentlet(contentletToCopy, site, folder, user, copySuffix, false);
    }

}
