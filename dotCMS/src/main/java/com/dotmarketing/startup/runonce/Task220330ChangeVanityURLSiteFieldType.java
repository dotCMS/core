package com.dotmarketing.startup.runonce;

import static java.util.stream.Collectors.toMap;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.Params;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Change the {@link com.dotcms.contenttype.model.type.VanityUrlContentType}'s site field from a
 * {@link com.dotcms.contenttype.model.field.CustomField} to a {@link com.dotcms.contenttype.model.field.HostFolderField},
 * executing the folloe:
 *
 * <code>
 * UPDATE field SET field_type = 'com.dotcms.contenttype.model.field.HostFolderField', field_contentlet = 'system_field'
 * WHERE velocity_var_name = 'site' AND structure_inode IN (select inode from structure where structuretype = 7
 * </code>
 *
 * Also updates the host_inode field in the Identifier table
 * taking the value from the proper field/column in the contentlet table or from the JSON  in the case of JSON contentlets.
 *
 *
 */
public class Task220330ChangeVanityURLSiteFieldType implements StartupTask {

    final String UPDATE_FIELD_TYPE = "UPDATE field SET field_type = 'com.dotcms.contenttype.model.field.HostFolderField', field_contentlet = 'system_field' "
            + "WHERE velocity_var_name = 'site' AND structure_inode IN (select inode from structure where structuretype = 7)";


    final String GET_CONTENTLET_NOT_JSON = "SELECT identifier,structure_inode,%s FROM contentlet "
            + "WHERE contentlet_as_json is null AND structure_inode in (SELECT inode FROM structure WHERE structuretype = 7)";


    final String GET_CONTENTLET_JSON = "SELECT identifier,contentlet_as_json FROM contentlet "
            + "WHERE contentlet_as_json is not null AND structure_inode in (SELECT inode FROM structure WHERE structuretype = 7)";

   final String GET_FIELD_CONTENTLET = "SELECT structure_inode,field_contentlet FROM field "
            + "WHERE velocity_var_name ='site' AND field_contentlet <> 'system_field' AND structure_inode in (SELECT inode FROM structure WHERE structuretype = 7)";

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        updateFieldType();

        CacheLocator.getContentTypeCache2().clearCache();
    }

    private void addJsonContentlet(final List<ContentletHost> result)
            throws DotDataException, DotSecurityException, JsonProcessingException {

        final List<Map<String, Object>> contentletsFromQuery = getFromQuery(GET_CONTENTLET_JSON);

        for (final Map<String, Object> contentlet : contentletsFromQuery) {
            final Contentlet contentletFromJSON = APILocator.getContentletJsonAPI()
                    .mapContentletFieldsFromJson(contentlet.get("contentlet_as_json").toString());

            result.add(new ContentletHost(contentlet.get("identifier").toString(),
                    contentletFromJSON.getStringProperty("site")));
        }
    }

    private void addNotJsonContentlet(final List<ContentletHost> result) throws DotDataException {
        final List<Map<String, Object>> fieldContentlets = getFromQuery(GET_FIELD_CONTENTLET);
        final Map<String, String> fieldContentletsMap = sortByStructure(fieldContentlets);
        final Optional<String> contentletQueryOptional = getContentletQuery(fieldContentlets);

        if (contentletQueryOptional.isPresent()) {
            final List<Map<String, Object>> contentletsFromQuery = getFromQuery(
                    contentletQueryOptional.get());

            for (final Map<String, Object> contentlet : contentletsFromQuery) {
                final String fieldContentlet = fieldContentletsMap.get(
                        contentlet.get("structure_inode"));

                if (UtilMethods.isSet(fieldContentlet)) {
                    result.add(new ContentletHost(contentlet.get("identifier").toString(),
                            contentlet.get(fieldContentlet).toString()));
                }
            }
        }
    }

    private void updateFieldType() throws DotDataException {
        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL(UPDATE_FIELD_TYPE);
        dotConnect.loadResult();
    }

    private List<Map<String, Object>> getFromQuery(final  String contentletQuery)
            throws DotDataException {

        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL(contentletQuery);

        return (List<Map<String, Object>>) dotConnect.loadResults();
    }

    private Optional<String> getContentletQuery(final List<Map<String, Object>> fieldContentlets) {
        final String fieldContentletsString = fieldContentlets.stream()
                .map(fields -> fields.get("field_contentlet").toString())
                .collect(Collectors.toSet())
                .stream()
                .collect(Collectors.joining(","));

        return UtilMethods.isSet(fieldContentletsString) ?
                Optional.of(String.format(GET_CONTENTLET_NOT_JSON, fieldContentletsString)) :
                Optional.empty();
    }

    private Map<String, String> sortByStructure(final  List<Map<String, Object>> fieldContentlets) {
        final Map<String, String> fieldContentletsMap = fieldContentlets.stream()
                .collect(toMap(map -> map.get("structure_inode").toString(),
                        map -> map.get("field_contentlet").toString()));
        return fieldContentletsMap;
    }

    private class ContentletHost {
        final String contentletIdentifier;
        final String hostInode;

        public ContentletHost(String contentletIdentifier, String hostInode) {
            this.contentletIdentifier = contentletIdentifier;
            this.hostInode = hostInode;
        }
    }
}
