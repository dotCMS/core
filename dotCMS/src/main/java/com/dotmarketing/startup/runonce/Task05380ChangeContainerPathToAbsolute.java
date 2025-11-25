package com.dotmarketing.startup.runonce;

import com.dotcms.contenttype.transform.JsonTransformer;
import com.dotcms.rendering.velocity.directive.ParseContainer;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.Params;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.liferay.util.StringPool;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.dotmarketing.util.Constants.CONTAINER_FOLDER_PATH;

/**
 * Change all the relative path into the template by absolute path, resolving them with the template's host
 * @since 5.3.7
 */
public class Task05380ChangeContainerPathToAbsolute implements StartupTask {

    private static final Pattern parseContainerPatter = Pattern.compile( "(?<=#parseContainer\\(').*?(?='\\))" );
    private static final String HOST_INDICATOR       = "//";
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

            Logger.debug(this,"Original body: " + body != null ? body : "NULL");
            Logger.debug(this,"Original Drawed_body: " + drawedBody);
            Logger.debug(this,"hostName: " + hostName);
            final Set<String> relativePaths = getRelativePaths(drawedBody);
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
            final String drawed_body,
            final String hostName,
            Set<String> relativePaths) {

        String newDrawed_body = drawed_body;

        for (final String relativePath : relativePaths) {
            Logger.debug(this,"Relative Path: " + relativePath);
            final String fullPath = FileAssetContainerUtil.getInstance().getFullPath(hostName, relativePath);
            Logger.debug(this,"Replacement: " + fullPath);
            newDrawed_body = newDrawed_body.replaceAll(
                    relativePath,
                    fullPath
            );
        }

        return newDrawed_body;
    }

    private Set<String> getRelativePaths(final String drawedBody) {
        return  getTemplateContainers(drawedBody).stream()
                .map(containerUUID -> containerUUID.get("identifier").toString())
                .filter((String idOrPath) -> isFolderAssetContainerId(idOrPath))
                .filter((String idOrPath) -> !isFullPath(idOrPath))
                .collect(Collectors.toSet());
    }

    public boolean isFullPath(final String path) {
        return path != null && path.startsWith(HOST_INDICATOR);
    }

    public boolean isFolderAssetContainerId(final String containerPath) {

        return UtilMethods.isSet(containerPath) && containerPath.contains(CONTAINER_FOLDER_PATH);
    }

    private List<Map<String, Object>> getAllDrawedTemplates() throws DotDataException {
        final Map<String, Object> results = new DotConnect().setSQL(GET_HOSTNAME_COLUMN)
                .loadObjectResults().get(0);

        final String hostNameColumnName = (String) results.get("field_contentlet");

        return new DotConnect()
                .setSQL(String.format(GET_TEMPLATES_QUERY,hostNameColumnName))
                .loadObjectResults();
    }

    public static Set<Map> getTemplateContainers(final String drawedBodyAsString) {

        try {
            return getContainers(drawedBodyAsString);
        } catch (IOException e) {
            return getColumnContainersFromVelocity(drawedBodyAsString);
        }
    }

    private static Set<Map>  getContainers(String drawedBodyAsString)
            throws JsonProcessingException {

        final Set<Map> result = new HashSet<>();

        final Map templateLayoutMap = JsonTransformer.mapper.readValue(drawedBodyAsString, Map.class);

        if (UtilMethods.isSet(templateLayoutMap.get("body")) &&
                UtilMethods.isSet(((Map) templateLayoutMap.get("body")).get("rows"))) {

            final List<Map> rows = (List<Map>) ((Map) templateLayoutMap.get("body")).get("rows");
            final List<Map> containerUUIDS = rows.stream()
                    .flatMap(row -> getMapStream(row, "columns"))
                    .flatMap(column -> getMapStream(column, "containers"))
                    .collect(Collectors.toList());
            result.addAll(containerUUIDS);
        }

        if (UtilMethods.isSet(templateLayoutMap.get("sidebar")) &&
                UtilMethods.isSet(((Map) templateLayoutMap.get("sidebar")).get("containers"))) {

            final Map sidebarMap = (Map) templateLayoutMap.get("sidebar");
            final List<Map<String, String>> sidebarsContainerUUIDS = ((List<Map<String, String>>) sidebarMap.get("containers"));

            result.addAll(sidebarsContainerUUIDS);
        }

        return result;
    }

    private static Stream<Map> getMapStream(final Map map, final String key) {
        return UtilMethods.isSet(map.get(key)) ?
                ((List<Map>) map.get(key)).stream()
                : ((List<Map>) Collections.EMPTY_LIST).stream();
    }

    /**
     * Method that will parse and return the containers inside a given Velocity code.
     *
     * Also, it returns the container ID or path exactly how it is into the code, it means that
     * if in the HTML code is using a FileContainer by the ID it returns the ID and not the PATH.
     *
     * For example if you have to follow velocity code:
     *
     * <code>
     * ...
     * #parseContainer('69b3d24d-7e80-4be6-b04a-d352d16493ee','1')
     * ...
     * </code>
     *
     * Where '69b3d24d-7e80-4be6-b04a-d352d16493ee' is the ID for a {@link com.dotmarketing.portlets.containers.model.FileAssetContainer}
     * in '//demo.dotcms.com/application/containers/default/'.
     *
     * it is going to return a {@link Map} with
     * - "uuid" equals to 1
     * - "identifier" equals to '69b3d24d-7e80-4be6-b04a-d352d16493ee'
     *
     * @param velocityCOde code
     * @return
     */
    public static Set<Map> getColumnContainersFromVelocity (final String velocityCOde ) {

        //Getting the containers for this html fragment
        Set<Map> containers = new HashSet<>();
        Matcher matcher = parseContainerPatter.matcher( velocityCOde );

        while ( matcher.find() ) {
            String parseContainerArguments = matcher.group();

            if (parseContainerArguments != null) {
                String[] splitArguments = parseContainerArguments.split("'\\s*,");
                String id = cleanId(splitArguments[0]);
                String uuid = splitArguments.length > 1 ? cleanId(splitArguments[1]) : ParseContainer.DEFAULT_UUID_VALUE;

                containers.add(Map.of("identifier", id, "uuid", uuid));
            }
        }

        return containers;
    }

    private static String cleanId(final String identifier) {
        return StringUtils.remove(identifier, StringPool.APOSTROPHE);
    }
}
