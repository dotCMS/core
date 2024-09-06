package com.dotcms.test.util.assertion;

import com.dotcms.enterprise.publishing.remote.bundler.FileBundlerTestUtil;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.list;

/**
 * {@link AssertionChecker} concrete class for {@link Template}
 */
public class TemplateAssertionChecker implements AssertionChecker<Template> {

    @Override
    public File getFileInner(Template template, File bundleRoot) {
        try {
            return FileBundlerTestUtil.getTemplatePath(template, bundleRoot);
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Object> getFileArguments(final Template templateParams, final File file) {
        try {
            final Identifier identifier = APILocator.getIdentifierAPI().find(templateParams.getIdentifier());

            final boolean isLive = file.getAbsolutePath().contains("/live/");

            final User systemUser = APILocator.systemUser();

            final Template workingTemplate = APILocator.getTemplateAPI().findWorkingTemplate(
                    templateParams.getIdentifier(), systemUser, false);
            final Template liveTemplate = APILocator.getTemplateAPI().findLiveTemplate(templateParams.getIdentifier(), systemUser, false);

            Template template = isLive ? liveTemplate : workingTemplate;

            return Map.of(
                    "id", template.getIdentifier(),
                    "host_id", identifier.getHostId(),
                    "inode", template.getInode(),
                    "title", template.getTitle(),
                    "friendly_name", template.getFriendlyName(),
                    "sort_order", template.getSortOrder(),
                    "asset_name", identifier.getAssetName(),
                    "drawed", template.isDrawed(),
                    "working_inode", workingTemplate.getInode(),
                    "live_inode", liveTemplate != null ? liveTemplate.getInode() : "null"

                    );
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getFilePathExpected(File file) {
        return "/bundlers-test/template/template.template.xml";
    }

    @Override
    public Collection<String> getRegExToRemove(File file) {
        return list(
                "<createDate class=\"sql-timestamp\">.*</createDate>",
                "<iDate>.*</iDate>",
                "<modDate>.*</modDate>",
                "<lockedOn>.*</lockedOn>",
                "<versionTs>.*</versionTs>",
                "<body>.*</body>",
                "<drawedBody>.*</drawedBody>",
                "<lockedOn class=\"sql-timestamp\">.*</lockedOn>",
                "<versionTs class=\"sql-timestamp\">.*</versionTs>",
                "<modDate class=\"sql-timestamp\">.*</modDate>",
                "<iDate class=\"sql-timestamp\">.*</iDate>",
                "<header>null</header>",
                "<footer>null</footer>",
                "<image>null</image>",
                "<liveInode>null</liveInode>",
                "<owner>.*</owner>"
        );
    }
}
