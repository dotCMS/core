package com.dotcms.test.util.assertion;

import com.dotcms.enterprise.publishing.remote.bundler.FileBundlerTestUtil;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.links.model.Link;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.list;

/**
 * {@link AssertionChecker} concrete class for {@link Link}
 */
public class LinkAssertionChecker implements AssertionChecker<Link> {
    @Override
    public Map<String, Object> getFileArguments(Link asset, File file) {
        try {
            final Identifier identifier = APILocator.getIdentifierAPI().find(asset.getIdentifier());

            final VersionInfo info =APILocator.getVersionableAPI().getVersionInfo(asset.getIdentifier());

            return Map.of(
                    "id", asset.getIdentifier(),
                    "asset_name", identifier.getAssetName(),
                    "parent_path", identifier.getParentPath(),
                    "host_id", identifier.getHostId(),
                    "inode", asset.getInode(),
                    "title", asset.getTitle(),
                    "friendly_name", asset.getFriendlyName(),
                    "sort_order", asset.getSortOrder(),
                    "live_inode", info.getLiveInode() != null ? info.getLiveInode() : "null"
            );
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getFilePathExpected(File file) {
        return "/bundlers-test/link/link.link.xml";
    }

    @Override
    public File getFileInner(Link link, File bundleRoot) {
        try {
            return FileBundlerTestUtil.getLinkPath(link, bundleRoot);
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Collection<String> getRegExToRemove(File file) {
        return list(
                "<createDate class=\"sql-timestamp\">.*</createDate>",
                "<iDate class=\"sql-timestamp\">.*</iDate>",
                "<modDate class=\"sql-timestamp\">.*</modDate>",
                "<lockedOn class=\"sql-timestamp\">.*</lockedOn>",
                "<versionTs class=\"sql-timestamp\">.*</versionTs>",
                "<lockedOn>.*</lockedOn>",
                "<versionTs>.*</versionTs>",
                "<liveInode>.*</liveInode>",
                "<owner>.*</owner>"
        );
    }
}
