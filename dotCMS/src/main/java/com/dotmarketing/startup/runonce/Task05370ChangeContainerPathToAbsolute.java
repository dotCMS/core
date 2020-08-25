package com.dotmarketing.startup.runonce;

import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.Params;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.startup.StartupTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Change all the relative path into the template by absolute path, resolving them with the template's host
 * @since 5.3.7
 */
public class Task05370ChangeContainerPathToAbsolute implements StartupTask {

    final String GET_TEMPLATES_QUERY = "SELECT DISTINCT contentlet.title as host_name, template.inode, template.drawed_body, template.body " +
            "FROM ((identifier " +
                "INNER JOIN template ON identifier.id = template.identifier) " +
                "INNER JOIN contentlet ON contentlet.identifier = identifier.host_inode) " +
            "where template.drawed_body is not null";


    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        final List<Map<String, Object>> templates = getAllDrawedTemplates();

        if (null != templates) {
            final List<Params> params = getParameters(templates);

            if (!params.isEmpty()) {
                final DotConnect dotConnect = new DotConnect();
                dotConnect.executeBatch("update template set drawed_body = ?, body = ? where inode =?", params);
            }
        }
    }

    private List<Params> getParameters(final List<Map<String, Object>> templates) {
        final List<Params> params = new ArrayList<>();

        for (final Map<String, Object> template : templates) {
            final String drawedBody = (String) template.get("drawed_body");
            final String body = (String) template.get("body");
            final String hostName = (String) template.get("host_name");

            final List<String> relativePaths = getRelativePaths(drawedBody);
            final String newDrawBody = replaceAllContainerRelativePath(drawedBody, hostName, relativePaths);
            final String newBody = body != null ? replaceAllContainerRelativePath(body, hostName, relativePaths) : null;

            if (!newDrawBody.equals(drawedBody)) {
                Params templateParams = new Params.Builder()
                        .add(newDrawBody, newBody, template.get("inode"))
                        .build();

                params.add(templateParams);
            }

        }
        return params;
    }

    private String replaceAllContainerRelativePath(
            final String string,
            final String hostName,
            List<String> relativePaths) {

        String newString = string;

        for (final String relativePath : relativePaths) {
            newString = newString.replaceAll(
                    relativePath,
                    FileAssetContainerUtil.getInstance().getFullPath(hostName, relativePath)
            );
        }

        return newString;
    }

    private List<String> getRelativePaths(final String drawedBody) {
        final TemplateLayout templateLayoutFromJSON = DotTemplateTool.getTemplateLayout(drawedBody);

        return templateLayoutFromJSON.getContainersIdentifierOrPath()
                .stream()
                .filter((String idOrPath) -> FileAssetContainerUtil.getInstance().isFolderAssetContainerId(idOrPath))
                .filter((String idOrPath) -> !FileAssetContainerUtil.getInstance().isFullPath(idOrPath))
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getAllDrawedTemplates() throws DotDataException {
        return new DotConnect().setSQL(GET_TEMPLATES_QUERY).loadObjectResults();
    }
}
