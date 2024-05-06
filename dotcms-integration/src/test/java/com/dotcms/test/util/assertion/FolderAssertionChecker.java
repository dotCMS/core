package com.dotcms.test.util.assertion;


import com.dotcms.enterprise.publishing.remote.bundler.FileBundlerTestUtil;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.list;

/**
 * {@link AssertionChecker} concrete class for {@link Folder}
 */
public class FolderAssertionChecker implements AssertionChecker<Folder> {
    @Override
    public Map<String, Object> getFileArguments(final Folder folder, File file) {
       try {
           final String parentPath = getParentPath(folder) + File.separator;

           final Identifier  identifier = APILocator.getIdentifierAPI().find(folder.getIdentifier());

            final Identifier hostIdentifier = APILocator.getIdentifierAPI().find(folder.getHostId());

           final Map<String, Object> map = new HashMap<>();

           map.put("inode", folder.getInode());
           map.put("id", folder.getIdentifier());
           map.put("name", folder.getName());
           map.put("sort_order", folder.getSortOrder());
           map.put("host_id", folder.getHost().getIdentifier());
           map.put("host_name", folder.getHost().getHostname());
           map.put("host_title", folder.getHost().getTitle());
           map.put("host_inode", folder.getHost().getInode());
           map.put("host_asset_name", hostIdentifier.getAssetName());
           map.put("title", folder.getTitle());
           map.put("asset_name", identifier.getAssetName());
           map.put("parent_path", parentPath);
           return map;
       } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private String getParentPath(Folder folder) throws DotDataException, DotSecurityException {
        final StringBuffer path = new StringBuffer();
        final User systemUser = APILocator.systemUser();
        Folder parentFolder = APILocator.getFolderAPI().findParentFolder(folder, systemUser, false);

        while(parentFolder != null) {
            path.append(File.separator);
            path.append(parentFolder.getName());
            parentFolder = APILocator.getFolderAPI().findParentFolder(parentFolder, systemUser, false);
        }

        return path.toString();
    }

    @Override
    public String getFilePathExpected(File file) {
        return "/bundlers-test/folder/folder.folder.xml";
    }

    @Override
    public File getFileInner(final Folder folder, final File bundleRoot) {
        try {
            return FileBundlerTestUtil.getFolderFilePath(folder, bundleRoot);
        } catch (DotSecurityException | DotDataException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Collection<String> getRegExToRemove(File file) {
        return list(
                "<iDate class=\"sql\\-timestamp\">.*</iDate>",
                "<modDate class=\"sql\\-timestamp\">.*</modDate>",
                "<createDate class=\"sql\\-timestamp\">.*</createDate>",
                "<date>.*</date>"
        );
    }
}
