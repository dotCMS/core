package com.dotmarketing.startup.runonce;

import com.dotcms.contenttype.transform.JsonTransformer;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.Params;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.util.DesignTemplateUtil;
import com.dotmarketing.startup.StartupTask;

import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Change all the relative path into the template by absolute path, resolving them with the template's host
 * @since 5.3.7
 */
public class Task05380ChangeContainerPathToAbsolute implements StartupTask {

    final static String GET_TEMPLATES_QUERY = "SELECT contentlet.%s as host_name, template.inode, template.identifier, template.drawed_body, template.body " +
        "FROM identifier " +
            "INNER JOIN template ON identifier.id = template.identifier " +
            "INNER JOIN contentlet_version_info cvi on identifier.host_inode = cvi.identifier " +
            "INNER JOIN contentlet ON cvi.working_inode = contentlet.inode " +
        "WHERE template.drawed_body is not null order by template.inode";

    final static String GET_HOSTNAME_COLUMN = "SELECT field.field_contentlet\n"
            + "FROM field JOIN structure s ON field.structure_inode = s.inode\n"
            + "WHERE s.velocity_var_name = 'Host' AND field.velocity_var_name = 'hostName'";

    final static String UPDATE_TEMPLATES = "update template set drawed_body = ?, body = ? where inode =?";

    final static String UPDATE_TEMPLATES_ORACLE = "update template set drawed_body = TO_CLOB(?), body = ? where inode =?";

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
                dotConnect.executeBatch(
                        DbConnectionFactory.isOracle() ? UPDATE_TEMPLATES_ORACLE : UPDATE_TEMPLATES,
                        params);
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
        final Set<ContainerUUID> templateContainers = getTemplateContainers(drawedBody);

        return getContainersIdentifierOrPath(templateContainers)
                .filter((String idOrPath) -> FileAssetContainerUtil.getInstance().isFolderAssetContainerId(idOrPath))
                .filter((String idOrPath) -> !FileAssetContainerUtil.getInstance().isFullPath(idOrPath))
                .collect(Collectors.toList());
    }

    private static Stream<String> getContainersIdentifierOrPath(final Set<ContainerUUID> containerUUIDS) {

        return containerUUIDS
                .stream()
                .map(ContainerUUID::getIdentifier);
    }

    private List<Map<String, Object>> getAllDrawedTemplates() throws DotDataException {
        final Map<String, Object> results = new DotConnect().setSQL(GET_HOSTNAME_COLUMN)
                .loadObjectResults().get(0);

        final String hostNameColumnName = (String) results.get("field_contentlet");

        return new DotConnect()
                .setSQL(String.format(GET_TEMPLATES_QUERY,hostNameColumnName))
                .loadObjectResults();
    }

    public static Set<ContainerUUID> getTemplateContainers(final String drawedBodyAsString) {

        try {
            return getContainers(drawedBodyAsString);
        } catch (IOException e) {
            return DesignTemplateUtil.getColumnContainersFromVelocity(drawedBodyAsString);
        }
    }

    private static Set<ContainerUUID>  getContainers(String drawedBodyAsString)
            throws JsonProcessingException {

        final Set<ContainerUUID> result = new HashSet<>();

        final Map templateLayoutMap = JsonTransformer.mapper.readValue(drawedBodyAsString, Map.class);

        if (UtilMethods.isSet(templateLayoutMap.get("body")) &&
                UtilMethods.isSet(((Map) templateLayoutMap.get("body")).get("rows"))) {

            final List<Map> rows = (List<Map>) ((Map) templateLayoutMap.get("body")).get("rows");
            final List<ContainerUUID> containerUUIDS = rows.stream()
                    .flatMap(row -> getMapStream(row, "columns"))
                    .flatMap(column -> getMapStream(column, "containers"))
                    .map(container -> new ContainerUUID(
                            container.get("identifier").toString(),
                            container.get("uuid").toString()))
                    .collect(Collectors.toList());
            result.addAll(containerUUIDS);
        }

        if (UtilMethods.isSet(templateLayoutMap.get("sidebar")) &&
                UtilMethods.isSet(((Map) templateLayoutMap.get("sidebar")).get("sidebar"))) {

            final Map sidebarMap = (Map) templateLayoutMap.get("sidebar");
            final List<ContainerUUID> sidebarsContainerUUIDS = ((List<Map>) sidebarMap.get("containers"))
                    .stream()
                    .map(container -> new ContainerUUID(container.get("identifier").toString(),
                            container.get("uuid").toString()))
                    .collect(Collectors.toList());

            result.addAll(sidebarsContainerUUIDS);
        }

        return result;
    }

    private static Stream<Map> getMapStream(final Map map, final String key) {
        return UtilMethods.isSet(map.get(key)) ?
                ((List<Map>) map.get(key)).stream()
                : ((List<Map>) Collections.EMPTY_LIST).stream();
    }
}
