package com.dotcms.test.util.assertion;

import com.dotcms.enterprise.publishing.remote.bundler.FileBundlerTestUtil;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.liferay.portal.model.User;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.list;

/**
 * {@link AssertionChecker} concrete class for {@link Container}
 */
public class ContainerAssertionChecker implements AssertionChecker<Container> {
    @Override
    public Map<String, Object> getFileArguments(Container containerParams, File file) {

        try {
            final Identifier identifier = APILocator.getIdentifierAPI().find(containerParams.getIdentifier());

            final List<ContainerStructure> containerStructures;

            containerStructures = APILocator.getContainerAPI()
                    .getContainerStructures(containerParams);

            final boolean isLive = file.getAbsolutePath().contains("/live/");
            final User systemUser = APILocator.systemUser();
            final Container containerWorking = APILocator.getContainerAPI().getWorkingContainerById(
                    containerParams.getIdentifier(), systemUser, false);
            final Container containerLive = APILocator.getContainerAPI().getLiveContainerById(
                    containerParams.getIdentifier(), systemUser, false);
            Container container = isLive ? containerLive : containerWorking;

            Map<String, Object> arguments = new HashMap<>(Map.of(
                    "id", container.getIdentifier(),
                    "asset_name", identifier.getAssetName(),
                    "host_id", identifier.getHostId(),
                    "inode", container.getInode(),
                    "title", container.getTitle(),
                    "friendly_name", container.getFriendlyName(),
                    "notes", container.getNotes(),
                    "sort_order", container.getSortOrder(),
                    "working_inode", containerWorking.getInode(),
                    "live_inode", containerLive != null ? containerLive.getInode() : "null"

            ));

            if (!containerStructures.isEmpty()) {
                arguments.put("content_type_id", containerStructures.get(0).getStructureId());
                arguments.put("container_structure_id", containerStructures.get(0).getId());
                arguments.put("code", containerStructures.get(0).getCode());
            }

            return arguments;
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getFilePathExpected(File file) {
        return "/bundlers-test/container/container.containers.container.xml";
    }

    @Override
    public File getFileInner(Container container, File bundleRoot) {
        try {
            return FileBundlerTestUtil.getContainerPath(container, bundleRoot);
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Collection<String> getRegExToRemove(File file) {
        return list(
                "<createDate class=\"sql-timestamp\">.*</createDate>",
                "<iDate>.*</iDate>",
                "<modDate>.*</modDate>",
                "<lockedOn>.*/lockedOn>",
                "<versionTs>.*</versionTs>",
                "<iDate class=\"sql-timestamp\">.*</iDate>",
                "<modDate class=\"sql-timestamp\">.*</modDate>",
                "<lockedOn class=\"sql-timestamp\">.*</lockedOn>",
                "<versionTs class=\"sql-timestamp\">.*</versionTs>",
                "<liveInode>null</liveInode>",
                "<owner>.*</owner>",
                "<csList class=\"com\\.google\\.common\\.collect\\.RegularImmutableList\" resolves\\-to=\"com\\.google\\.common\\.collect\\.ImmutableList\\$SerializedForm\"><elements/></csList>",
                "<csList/>",
                " class=\"com.google.common.collect.RegularImmutableList\" resolves-to=\"com.google.common.collect.ImmutableList$SerializedForm\"",
                "<elements>",
                "</elements>"
        );
    }
}
