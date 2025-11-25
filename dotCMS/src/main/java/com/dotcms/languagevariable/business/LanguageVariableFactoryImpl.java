package com.dotcms.languagevariable.business;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.util.transform.DBColumnToJSONConverter;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.exception.DotDataException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Implementation class for the {@link LanguageVariableFactory}.
 */
public class LanguageVariableFactoryImpl implements LanguageVariableFactory {

    public static final String ORDER_BY = " order by ";

    /**
     * {@inheritDoc}
     */
    @Override
    public List<LanguageVariable> findVariables(final ContentType contentType,
            final long languageId,
            final int offset, final int limit, final String sortBy)
            throws DotDataException {
        final DotConnect dotConnect = new DotConnect();

        String select = "select contentlet.contentlet_as_json->'fields'->'key'->'value' as key ,\n"
                + "  contentlet.contentlet_as_json->'fields'->'value'->'value' as value, \n"
                + "  contentlet.identifier \n  "
                + "from contentlet, contentlet_version_info, inode \n"
                + "  where structure_inode = '" + contentType.inode() + "' \n"
                + "  and contentlet_version_info.identifier=contentlet.identifier \n"
                + "  and contentlet_version_info.working_inode=contentlet.inode \n"
                + "  and contentlet.inode = inode.inode \n"
                + "  and contentlet.language_id = " + languageId + " \n"
                // item can be in either live or working state
                + "  and ( contentlet_version_info.working_inode = contentlet.inode or contentlet_version_info.live_inode = contentlet.inode ) \n"
                + "  and contentlet_version_info.deleted = 'false' \n ";

        final String sanitizedSort = SQLUtil.sanitizeSortBy(sortBy);
        if (!sanitizedSort.isEmpty()) {
            select += ORDER_BY + sanitizedSort;
        }
        dotConnect.setSQL(select);

        if ((offset >= 0) && (limit > 0)) {
            dotConnect.setStartRow(offset);
            dotConnect.setMaxRows(limit);
        }

        final List<LanguageVariable> languageVariables = new ArrayList<>();
        final List<Map<String, Object>> maps = dotConnect.loadObjectResults();
        for (Map<String, Object> map : maps) {
            final String key = DBColumnToJSONConverter.getObjectFromDBJson(map.get("key"), String.class);
            final String value =  DBColumnToJSONConverter.getObjectFromDBJson(map.get("value"), String.class);
            final String identifier = (String) map.get("identifier");
            languageVariables.add(ImmutableLanguageVariable.builder()
                    .identifier(identifier)
                    .key(key)
                    .value(value)
                    .build());
        }
        return Collections.unmodifiableList(languageVariables);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<LanguageVariableExt> findVariablesForPagination(ContentType contentType, int offset,
            int limit, String orderBy) throws DotDataException {
        String select = "select contentlet.contentlet_as_json->'fields'->'key'->'value' as key, \n"
                + "          contentlet.contentlet_as_json->'fields'->'value'->'value' as value, \n"
                + "          contentlet.identifier,\n"
                + "          contentlet.language_id\n"
                + "     from contentlet   \n"
                + "      inner join ( \n"
                + "     select distinct c.identifier, vi.lang \n"
                + "       from contentlet c, contentlet_version_info vi  \n"
                + "     where \n"
                + "      c.structure_inode = '"+contentType.inode()+"' \n"
                + "      and vi.identifier = c.identifier \n"
                + "      and ( vi.working_inode = c.inode or vi.live_inode = c.inode ) \n"
                + "      and vi.deleted = 'false' \n"
                + "        order by c.identifier \n"
                + "        offset "+offset+" limit "+limit+"  \n"
                + "    ) con_ident on contentlet.identifier = con_ident.identifier and contentlet.language_id = con_ident.lang \n";
        final String sanitizedSort = SQLUtil.sanitizeSortBy(orderBy);
        if (!sanitizedSort.isEmpty()) {
            select += ORDER_BY + sanitizedSort;
        } else {
            select += ORDER_BY + " contentlet.identifier";
        }
        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL(select);
        final List<LanguageVariableExt> languageVariables = new ArrayList<>();
        final List<Map<String, Object>> maps = dotConnect.loadObjectResults();
        for (Map<String, Object> map : maps) {
            final String key = DBColumnToJSONConverter.getObjectFromDBJson(map.get("key"), String.class);
            final String value =  DBColumnToJSONConverter.getObjectFromDBJson(map.get("value"), String.class);
            final String identifier = (String) map.get("identifier");
            final long languageId = (long) map.get("language_id");
            languageVariables.add(
                    ImmutableLanguageVariableExt.builder()
                            .identifier(identifier)
                            .key(key)
                            .value(value)
                            .languageId(languageId)
                            .build()
            );
        }
        return Collections.unmodifiableList(languageVariables);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int countVariablesByKey(ContentType contentType) {
        final DotConnect dotConnect = new DotConnect();
        final String select = "select count( distinct( contentlet.contentlet_as_json->'fields'->'key'->'value' )) as x \n"
                + "     from contentlet  \n"
                + "     inner join ( \n"
                + "      select distinct c.identifier, vi.lang \n"
                + "       from contentlet c, contentlet_version_info vi \n"
                + "      where \n"
                + "       c.structure_inode = '"+contentType.inode()+"' \n"
                + "       and vi.identifier = c.identifier \n"
                + "       and ( vi.working_inode = c.inode or vi.live_inode = c.inode ) \n"
                + "       and vi.deleted = 'false' \n"
                + "        order by c.identifier       \n"
                + "    ) con_ident on contentlet.identifier = con_ident.identifier and contentlet.language_id = con_ident.lang \n";
        dotConnect.setSQL(select);
        return dotConnect.getInt("x");
    }


    @Override
    public int countVariablesByKey(ContentType contentType, final long languageId) {
        final DotConnect dotConnect = new DotConnect();
        final String select = "select count( distinct( contentlet.contentlet_as_json->'fields'->'key'->'value' )) as x \n"
                + "     from contentlet  \n"
                + "     inner join ( \n"
                + "      select distinct c.identifier, vi.lang \n"
                + "       from contentlet c, contentlet_version_info vi \n"
                + "      where \n"
                + "       c.structure_inode = '"+contentType.inode()+"' \n"
                + "       and vi.identifier = c.identifier \n"
                + "       and ( vi.working_inode = c.inode or vi.live_inode = c.inode ) \n"
                + "       and vi.deleted = 'false' \n"
                + "        order by c.identifier       \n"
                + "    ) con_ident on contentlet.identifier = con_ident.identifier and contentlet.language_id = "+languageId+ " \n";
        dotConnect.setSQL(select);
        return dotConnect.getInt("x");
    }



}
