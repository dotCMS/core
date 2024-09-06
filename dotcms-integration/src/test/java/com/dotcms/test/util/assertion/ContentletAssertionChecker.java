package com.dotcms.test.util.assertion;

import com.dotcms.enterprise.publishing.remote.bundler.ContentBundler;
import com.dotcms.enterprise.publishing.remote.bundler.FileBundlerTestUtil;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.liferay.portal.model.User;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.list;

/**
 * {@link AssertionChecker} concrete class for {@link Contentlet}
 */
public class ContentletAssertionChecker implements AssertionChecker<Contentlet> {

    public static final String CONTENT_EXPECTED_DEFAULT_FILE_PATH = "/bundlers-test/contentlet/contentlet.content.xml";
    public static final String WORKFLOW_TASK_EXPECTED_FILE_PATH = "/bundlers-test/contentlet/workflow/workflow_task.contentworkflow.xml";
    public static final String BINARY_FILE_PATH = "/images/test.jpg";

    @Override
    public Map<String, Object> getFileArguments(final Contentlet contentlet, File file) {
       try {
           final String fileName = file.getName();

           if (fileName.endsWith(ContentBundler.CONTENT_EXTENSION)) {
               return getContentletFileArguments(contentlet);
           } else   if (fileName.endsWith(ContentBundler.CONTENT_WORKFLOW_EXTENSION)) {
               return getWorkflowTaskFileArguments(contentlet);
           } else {
               return new HashMap<>();
           }
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private Map<String, Object> getWorkflowTaskFileArguments(Contentlet contentlet) throws DotDataException {
        final WorkflowTask taskByContentlet = APILocator.getWorkflowAPI().findTaskByContentlet(contentlet);
        return Map.of(
                "id", taskByContentlet.getId(),
                "title", taskByContentlet.getTitle(),
                "description", taskByContentlet.getDescription().replaceAll("\"", "&quot;"),
                "status", taskByContentlet.getStatus(),
                "webasset", taskByContentlet.getWebasset(),
                "language_id", taskByContentlet.getLanguageId()
        );
    }

    @NotNull
    private Map<String, Object> getContentletFileArguments(Contentlet contentlet) throws DotDataException, DotSecurityException {
        final Identifier  identifier = APILocator.getIdentifierAPI().find(contentlet.getIdentifier());

        final Map<String, Object> map = new HashMap<>(Map.of(
                "id", contentlet.getIdentifier(),
                "inode", contentlet.getInode(),
                "sort_order", contentlet.getSortOrder(),
                "host_id", identifier.getHostId(),
                "title", contentlet.getTitle(),
                "asset_name", identifier.getAssetName(),
                "parent_path", identifier.getParentPath(),
                "stInode", contentlet.getMap().get("stInode")
        ));

        final FileAsset fileAsset = APILocator.getFileAssetAPI()
                .find(contentlet.getInode(), APILocator.systemUser(), false);

        if (fileAsset != null) {
             map.put("folder", fileAsset.getFolder());
             map.put("file_asset", fileAsset.getFileAsset().getAbsolutePath());
         }

        return map;
    }

    @Override
    public String getFilePathExpected(File file) {
        final String fileName = file.getName();

        if (fileName.endsWith(ContentBundler.CONTENT_EXTENSION)) {
            return CONTENT_EXPECTED_DEFAULT_FILE_PATH;
        } else   if (fileName.endsWith(ContentBundler.CONTENT_WORKFLOW_EXTENSION)) {
            return WORKFLOW_TASK_EXPECTED_FILE_PATH;
        } else {
            return BINARY_FILE_PATH;
        }
    }

    @Override
    public Collection<String> getFilesPathExpected() {
        return list(
                CONTENT_EXPECTED_DEFAULT_FILE_PATH,
                WORKFLOW_TASK_EXPECTED_FILE_PATH,
                BINARY_FILE_PATH
        );
    }

    @Override
    public Collection<File> getFile(final Contentlet contentlet, File bundleRoot) {
        try {

            final List<ContentletVersionInfo> contentletVersionInfos = APILocator.getVersionableAPI()
                    .findContentletVersionInfos(contentlet.getIdentifier());

            final boolean live = contentlet.isLive();
            final List<Contentlet> contentlets = contentletVersionInfos.stream()
                    .map(contentletVersionInfo -> getContentlet(contentletVersionInfo, live))
                    .collect(Collectors.toList());

            final List<File> fileList = new ArrayList<>();

            int counter = 0;

            for (final Contentlet innerContentlet : contentlets) {
                fileList.add(FileBundlerTestUtil.getContentletPath(innerContentlet, bundleRoot, counter++));
            }

            fileList.add(FileBundlerTestUtil.getWorkflowTaskFilePath(contentlet, bundleRoot));

            final Optional<String> binaryFilePathOptional = getBinaryFilePath(contentlet);

            if (binaryFilePathOptional.isPresent()) {
                final String binaryFilePath = bundleRoot.getPath() + File.separator + "assets" +
                        File.separator + binaryFilePathOptional.get();
                fileList.add(new File(binaryFilePath));
            }

            return  fileList;
        } catch (DotDataException | DotSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Contentlet getContentlet(final ContentletVersionInfo contentletVersionInfo, final boolean live) {

        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        final User systemUser = APILocator.systemUser();

        try {
            return live ? contentletAPI.find(contentletVersionInfo.getLiveInode(), systemUser, false)
                    : contentletAPI.find(contentletVersionInfo.getWorkingInode(), systemUser, false);
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<String> getBinaryFilePath(final Contentlet contentlet) throws IOException {
        final List<Field> fields= FieldsCache.getFieldsByStructureInode(contentlet.getStructureInode());
        final String inode = contentlet.getInode();

        for(Field ff : fields) {
            if (ff.getFieldType().equals(Field.FieldType.BINARY.toString())) {

                File sourceFile = contentlet.getBinary(ff.getVelocityVarName());

                return  Optional.of(inode.charAt(0) + File.separator + inode.charAt(1) + File.separator +
                        inode + File.separator + ff.getVelocityVarName() + File.separator + sourceFile.getName());

            }
        }

        return Optional.empty();
    }

    @Override
    public Collection<String> getRegExToRemove(final File file) {

        final String fileName = file.getName();

        if (fileName.endsWith(ContentBundler.CONTENT_EXTENSION)) {
            return list(
                    "lockedOn class=\"sql-timestamp\">.*</lockedOn>",
                    "<versionTs class=\"sql-timestamp\">.*</versionTs>",
                    "<date>.*</date>",
                    "<createDate class=\"sql-timestamp\">.*</createDate>",
                    "<file>.*</file>"
            );
        } else   if (fileName.endsWith(ContentBundler.CONTENT_WORKFLOW_EXTENSION)) {
            return list(
                    "<modDate>.*</modDate>",
                    "creationDate>.*</creationDate>",
                    "<lazyComputeDimensions serialization=\"custom\">.*</lazyComputeDimensions>",
                    " class=\"com\\.dotmarketing\\.portlets\\.fileassets\\.business\\.FileAsset\""
            );
        } else {
            return list();
        }

    }

    public boolean checkFileContent(Contentlet asset) {
        return false;
    }
}
