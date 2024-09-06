package com.dotcms.test.util.assertion;

import com.dotcms.enterprise.publishing.remote.bundler.FileBundlerTestUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.languagesmanager.model.Language;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotmarketing.beans.Host.HOST_NAME_KEY;

/**
 * {@link AssertionChecker} concrete class for {@link Host}
 */
public class HostAssertionChecker implements AssertionChecker<Contentlet> {
    @Override
    public Map<String, Object> getFileArguments(final Contentlet host, File file) {
        final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        final ContentletVersionInfo contentletVersionInfo = APILocator.getVersionableAPI()
                .getContentletVersionInfo(host.getIdentifier(), defaultLanguage.getId()).get();

        final Identifier identifier;
        try {
            identifier = APILocator.getIdentifierAPI().find(host.getIdentifier());

            return Map.of(
                    "host_id", host.getIdentifier(),
                    "host_live_inode", contentletVersionInfo.getLiveInode() != null ? contentletVersionInfo.getLiveInode() : "null",
                    "host_working_inode", contentletVersionInfo.getWorkingInode(),
                    "host_lang", host.getLanguageId(),
                    "host_name", host.getStringProperty(HOST_NAME_KEY),
                    "host_inode", host.getInode(),
                    "asset_name", identifier.getAssetName()

            );
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public String getFilePathExpected(File file) {
        return "/bundlers-test/host/host.host.xml";
    }

    @Override
    public File getFileInner(final Contentlet host, final File bundleRoot) {
        try {
            return FileBundlerTestUtil.getHostFilePath(host, bundleRoot);
        } catch (DotSecurityException | DotDataException e) {
            throw new RuntimeException(e);
        }
    }

    public Collection<String> getRegExToRemove(File file) {
        return list(
                "<createDate class=\"sql-timestamp\">.*</createDate>",
                "<lockedOn class=\"sql-timestamp\">.*</lockedOn>",
                "<versionTs class=\"sql-timestamp\">.*</versionTs>",
                "<string>modDate</string><sql-timestamp>.*</sql-timestamp>",
                "<string>modDate</string><date>.*</date>",
                "<liveInode>null</liveInode>",
                "<lockedBy>system</lockedBy>",
                "<concurrent-hash-map>.*</concurrent-hash-map>",
                "<owner>.*</owner>"
        );
    }
}
