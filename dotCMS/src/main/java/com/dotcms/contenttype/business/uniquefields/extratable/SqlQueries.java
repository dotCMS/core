package com.dotcms.contenttype.business.uniquefields.extratable;

import static com.dotcms.content.elasticsearch.business.ESContentletAPIImpl.UNIQUE_PER_SITE_FIELD_VARIABLE_NAME;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.CONTENTLET_IDS_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.CONTENT_TYPE_ID_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.FIELD_VALUE_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.FIELD_VARIABLE_NAME_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.LANGUAGE_ID_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.LIVE_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.SITE_ID_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.UNIQUE_PER_SITE_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.VARIANT_ATTR;

/**
 * This is a simple utility class that exposes the different SQL queries used to perform CRUD
 * operations on the {@code unique_fields} table.
 *
 * @author Jose Castro
 * @since Aug 6th, 2025
 */
public final class SqlQueries {

    private SqlQueries() {}

    public static final String INSERT_SQL = "INSERT INTO unique_fields (unique_key_val, supporting_values) " +
            "VALUES (encode(sha256(convert_to(?::text, 'UTF8')), 'hex'), ?)";

    public static final String RECALCULATE_UNIQUE_KEY_VAL = "UPDATE unique_fields\n" +
            "SET unique_key_val = encode(sha256(" +
            "convert_to(\n" +
            "CONCAT(" +
            "    jsonb_extract_path_text(supporting_values, '" + CONTENT_TYPE_ID_ATTR + "')::text || \n" +
            "    jsonb_extract_path_text(supporting_values, '" + FIELD_VARIABLE_NAME_ATTR + "')::text || \n" +
            "    jsonb_extract_path_text(supporting_values, '" + LANGUAGE_ID_ATTR + "')::text || \n" +
            "    jsonb_extract_path_text(supporting_values, '" + FIELD_VALUE_ATTR + "')::text\n" +
            "    %s \n" +
            "),'UTF8'\n" +
            ")\n" +
            "), 'hex'), \n" +
            "supporting_values = jsonb_set(supporting_values, '{" + UNIQUE_PER_SITE_ATTR + "}', '%s') \n" +
            "WHERE supporting_values->>'" + CONTENT_TYPE_ID_ATTR + "' = ?\n" +
            "AND supporting_values->>'" + FIELD_VARIABLE_NAME_ATTR + "' = ?";

    public static final String UPDATE_CONTENT_LIST ="UPDATE unique_fields " +
            "SET supporting_values = jsonb_set(supporting_values, '{" + CONTENTLET_IDS_ATTR + "}', ?::jsonb) " +
            "WHERE unique_key_val = encode(sha256(convert_to(?::text, 'UTF8')), 'hex')";

    public static final String UPDATE_CONTENT_LIST_WITH_HASH ="UPDATE unique_fields " +
            "SET supporting_values = jsonb_set(supporting_values, '{" + CONTENTLET_IDS_ATTR + "}', ?::jsonb) " +
            "WHERE unique_key_val = ?";

    public static final String GET_UNIQUE_FIELDS_BY_CONTENTLET = "SELECT * FROM unique_fields " +
            "WHERE supporting_values->'" + CONTENTLET_IDS_ATTR + "' @> ?::jsonb " +
            "AND supporting_values->>'" + VARIANT_ATTR + "' = ? " +
            "AND (supporting_values->>'"+ LANGUAGE_ID_ATTR + "')::BIGINT = ? " +
            "AND supporting_values->>'" + FIELD_VARIABLE_NAME_ATTR + "' = ?";

    public static final String DELETE_UNIQUE_FIELDS_BY_CONTENTLET = "DELETE FROM unique_fields " +
            "WHERE supporting_values->'" + CONTENTLET_IDS_ATTR + "' @> ?::jsonb AND supporting_values->>'" + VARIANT_ATTR + "' = ? " +
            "AND (supporting_values->>'"+ LANGUAGE_ID_ATTR + "')::BIGINT = ? " +
            "AND (supporting_values->>'" + LIVE_ATTR + "')::BOOLEAN = ?";

    public static final String SET_LIVE_BY_CONTENTLET = "UPDATE unique_fields " +
            "SET supporting_values = jsonb_set(supporting_values, '{" + LIVE_ATTR +  "}', ?::jsonb) " +
            "WHERE supporting_values->'" + CONTENTLET_IDS_ATTR + "' @> ?::jsonb " +
            "AND supporting_values->>'" + VARIANT_ATTR + "' = ? " +
            "AND (supporting_values->>'"+ LANGUAGE_ID_ATTR + "')::BIGINT = ? " +
            "AND (supporting_values->>'" + LIVE_ATTR + "')::BOOLEAN = false";

    public static final String GET_UNIQUE_FIELDS_BY_CONTENTLET_AND_LANGUAGE = "SELECT * FROM unique_fields " +
            "WHERE supporting_values->'" + CONTENTLET_IDS_ATTR + "' @> ?::jsonb AND (supporting_values->>'" + LANGUAGE_ID_ATTR +"')::BIGINT = ?";

    public static final String GET_UNIQUE_FIELDS_BY_CONTENTLET_AND_VARIANT= "SELECT * FROM unique_fields " +
            "WHERE supporting_values->'" + CONTENTLET_IDS_ATTR + "' @> ?::jsonb AND supporting_values->>'" + VARIANT_ATTR + "' = ?";

    public static final String DELETE_UNIQUE_FIELDS = "DELETE FROM unique_fields WHERE unique_key_val = ?";

    public static final String GET_UNIQUE_FIELDS_BY_UNIQUE_FIELD_CRITERIA = "SELECT * FROM unique_fields " +
            "WHERE unique_key_val = encode(sha256(convert_to(?::text, 'UTF8')), 'hex')";

    public static final String DELETE_UNIQUE_FIELDS_BY_FIELD = "DELETE FROM unique_fields " +
            "WHERE supporting_values->>'" + FIELD_VARIABLE_NAME_ATTR + "' = ?";

    public static final String DELETE_UNIQUE_FIELDS_BY_CONTENT_TYPE = "DELETE FROM unique_fields " +
            "WHERE supporting_values->>'" + CONTENT_TYPE_ID_ATTR + "' = ?";

    public static final String POPULATE_UNIQUE_FIELDS_VALUES_QUERY = "INSERT INTO unique_fields (unique_key_val, supporting_values) " +
            "SELECT  encode(" +
            "            sha256(" +
            "                    convert_to(" +
            "                            CONCAT(" +
            "                                    content_type_id::text," +
            "                                    field_var_name::text," +
            "                                    language_id::text," +
            "                                    LOWER(field_value)::text," +
            "                                    CASE WHEN uniquePerSite = 'true' THEN COALESCE(host_id::text, '') ELSE '' END" +
            "                            )," +
            "                            'UTF8'" +
            "                    )" +
            "            )," +
            "            'hex'" +
            "       ) AS unique_key_val, " +
            "       json_build_object('" + CONTENT_TYPE_ID_ATTR + "', content_type_id, " +
            "'" + FIELD_VARIABLE_NAME_ATTR + "', field_var_name, " +
            "'" + LANGUAGE_ID_ATTR + "', language_id, " +
            "'" + FIELD_VALUE_ATTR +"', LOWER(field_value), " +
            "'" + SITE_ID_ATTR + "', host_id, " +
            "'" + VARIANT_ATTR + "', variant_id, " +
            "'" + UNIQUE_PER_SITE_ATTR + "', " + "uniquePerSite, " +
            "'" + LIVE_ATTR + "', live, " +
            "'" + CONTENTLET_IDS_ATTR + "', contentlet_identifier) AS supporting_values " +
            "FROM (" +
            "        SELECT structure.inode                                       AS content_type_id," +
            "               field.velocity_var_name                               AS field_var_name," +
            "               contentlet.language_id                                AS language_id," +
            "               (CASE WHEN field_variable.variable_value = 'true' THEN identifier.host_inode ELSE '' END) AS host_id," +
            "               jsonb_extract_path_text(contentlet_as_json -> 'fields', field.velocity_var_name)::jsonb ->>'value' AS field_value," +
            "               ARRAY_AGG(DISTINCT contentlet.identifier)                      AS contentlet_identifier," +
            "               (CASE WHEN COUNT(DISTINCT contentlet_version_info.variant_id) > 1 THEN 'DEFAULT' ELSE MAX(contentlet_version_info.variant_id) END) AS variant_id, " +
            "               ((CASE WHEN COUNT(*) > 1 AND COUNT(DISTINCT contentlet_version_info.live_inode = contentlet.inode) > 1 THEN 0 " +
            "                   ELSE MAX((CASE WHEN contentlet_version_info.live_inode = contentlet.inode THEN 1 ELSE 0 END)::int) " +
            "                   END) = 1) AS live," +
            "               (MAX(CASE WHEN field_variable.variable_value = 'true' THEN 1 ELSE 0 END)) = 1 AS uniquePerSite" +
            "        FROM contentlet" +
            "                 INNER JOIN structure ON structure.inode = contentlet.structure_inode" +
            "                 INNER JOIN field ON structure.inode = field.structure_inode" +
            "                 INNER JOIN identifier ON contentlet.identifier = identifier.id" +
            "                 INNER JOIN contentlet_version_info ON contentlet_version_info.live_inode = contentlet.inode OR" +
            "                                                       contentlet_version_info.working_inode = contentlet.inode" +
            "                 LEFT JOIN field_variable ON field_variable.field_id = field.inode AND field_variable.variable_key = '" + UNIQUE_PER_SITE_FIELD_VARIABLE_NAME + "'" +
            "        WHERE jsonb_extract_path_text(contentlet_as_json -> 'fields', field.velocity_var_name) IS NOT NULL" +
            "          AND field.unique_ = true" +
            "        GROUP BY structure.inode," +
            "                 field.velocity_var_name," +
            "                 contentlet.language_id," +
            "                 (CASE WHEN field_variable.variable_value = 'true' THEN identifier.host_inode ELSE '' END)," +
            "                 jsonb_extract_path_text(contentlet_as_json -> 'fields', field.velocity_var_name)::jsonb ->>'value') as data_to_populate";

    /**
     * Returns the number of records that share the same hash, a.k.a. unique key value. Such records
     * must be fixed for dotCMS to start up correctly.
     */
    public static final String GET_RECORDS_WITH_SAME_HASH = "SELECT unique_key_val, COUNT(unique_key_val) " +
            "FROM unique_fields u " +
            "GROUP BY unique_key_val " +
            "HAVING COUNT(unique_key_val) > 1;";

    /**
     * Returns all unique fields with the same hash, a.k.a. the same unique key value.
     */
    public static final String GET_UNIQUE_FIELDS_BY_HASH = "SELECT * FROM unique_fields " +
            "WHERE unique_key_val = ?";

    /**
     * Updates the unique value of a single conflicting entry in the {@code unique_fields} table.
     * Conflicting entries may fall into one of the following categories:
     * <ul>
     *     <li>They belong to separate Contentlets; i.e. different Contentlet Identifiers, that have
     *     the exact same unique value.</li>
     *     <li>The same Contentlet. However, one entry belongs to the Live version, and the other
     *     one belongs to the Working version.</li>
     * </ul>
     * The solution is to add some additional characters to the unique value, and re-generate the
     * hash for the database record so it's now unique. In the end, this query is meant to <b>fix
     * one duplicate entry at a time</b>, so it's very important that it matches only one record.
     */
    public static final String FIX_DUPLICATE_ENTRY = "UPDATE unique_fields " +
            "SET unique_key_val = encode(sha256(convert_to(?::text, 'UTF8')), 'hex'), " +
            "supporting_values = ? " +
            "WHERE unique_key_val = ? AND supporting_values->'" + CONTENTLET_IDS_ATTR + "' @> ?::jsonb " +
            "AND (supporting_values->>'" + LIVE_ATTR + "')::BOOLEAN = ?";

}
