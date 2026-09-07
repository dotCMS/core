package com.dotcms.enterprise.publishing.staticpublishing;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manages operational persistence for vanity aliases published on S3.
 */
public class S3VanityAliasRepository {

    private static final String TABLE_NAME = "static_s3_vanity_mapping";

    private static final String SELECT_BY_LOOKUP =
            "SELECT endpoint_id, host_id, language_id, canonical_path, vanity_path, vanity_url_id, "
                    + "bucket_name, bucket_region, bucket_prefix "
                    + "FROM static_s3_vanity_mapping "
                    + "WHERE endpoint_id = ? AND host_id = ? AND language_id = ? "
                    + "AND canonical_path_hash = ? AND canonical_path = ?";
    private static final String SELECT_BY_VANITY_URL_ID =
            "SELECT endpoint_id, host_id, language_id, canonical_path, vanity_path, vanity_url_id, "
                    + "bucket_name, bucket_region, bucket_prefix "
                    + "FROM static_s3_vanity_mapping "
                    + "WHERE endpoint_id = ? AND language_id = ? AND vanity_url_id = ?";
    private static final String SELECT_BY_VANITY_URL_ID_ANY_LANGUAGE =
            "SELECT endpoint_id, host_id, language_id, canonical_path, vanity_path, vanity_url_id, "
                    + "bucket_name, bucket_region, bucket_prefix "
                    + "FROM static_s3_vanity_mapping "
                    + "WHERE endpoint_id = ? AND vanity_url_id = ?";
    private static final String DELETE_BY_LOOKUP =
            "DELETE FROM static_s3_vanity_mapping "
                    + "WHERE endpoint_id = ? AND host_id = ? AND language_id = ? "
                    + "AND canonical_path_hash = ? AND canonical_path = ?";
    private static final String DELETE_ALIAS =
            "DELETE FROM static_s3_vanity_mapping "
                    + "WHERE endpoint_id = ? AND host_id = ? AND language_id = ? "
                    + "AND canonical_path_hash = ? AND canonical_path = ? "
                    + "AND vanity_path_hash = ? AND vanity_path = ?";
    private static final String DELETE_BY_VANITY_URL_ID =
            "DELETE FROM static_s3_vanity_mapping WHERE endpoint_id = ? AND language_id = ? AND vanity_url_id = ?";
    private static final String DELETE_BY_VANITY_URL_ID_ANY_LANGUAGE =
            "DELETE FROM static_s3_vanity_mapping WHERE endpoint_id = ? AND vanity_url_id = ?";
    private static final String INSERT_ALIAS =
            "INSERT INTO static_s3_vanity_mapping "
                    + "(endpoint_id, host_id, language_id, canonical_path, canonical_path_hash, "
                    + "vanity_path, vanity_path_hash, vanity_url_id, bucket_name, bucket_region, "
                    + "bucket_prefix, mod_date) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";

    /**
     * Finds operational mappings associated with the canonical page.
     *
     * @param lookup logical mapping key
     * @return persisted mappings
     * @throws DotDataException when reading mappings fails
     */
    @CloseDBIfOpened
    public List<S3VanityAlias> findByLookup(final S3VanityAliasLookup lookup) throws DotDataException {
        final List<Map<String, Object>> rows = new DotConnect()
                .setSQL(SELECT_BY_LOOKUP)
                .addParam(lookup.endpointId)
                .addParam(lookup.hostId)
                .addParam(lookup.languageId)
                .addParam(pathHash(lookup.canonicalPath))
                .addParam(lookup.canonicalPath)
                .loadObjectResults();

        return aliasesFromRows(rows);
    }

    /**
     * Finds operational mappings associated with one Vanity URL.
     *
     * @param endpointId static endpoint identifier
     * @param languageId Vanity URL language identifier
     * @param vanityUrlId Vanity URL identifier
     * @return persisted mappings for the Vanity URL
     * @throws DotDataException when reading mappings fails
     */
    @CloseDBIfOpened
    public List<S3VanityAlias> findByVanityUrlId(final String endpointId,
                                                 final long languageId,
                                                 final String vanityUrlId) throws DotDataException {
        final List<Map<String, Object>> rows = new DotConnect()
                .setSQL(SELECT_BY_VANITY_URL_ID)
                .addParam(endpointId)
                .addParam(languageId)
                .addParam(vanityUrlId)
                .loadObjectResults();

        return aliasesFromRows(rows);
    }

    /**
     * Finds operational mappings associated with one Vanity URL in any language.
     *
     * @param endpointId static endpoint identifier
     * @param vanityUrlId Vanity URL identifier
     * @return persisted mappings for the Vanity URL
     * @throws DotDataException when reading mappings fails
     */
    @CloseDBIfOpened
    public List<S3VanityAlias> findByVanityUrlId(final String endpointId,
                                                 final String vanityUrlId) throws DotDataException {
        final List<Map<String, Object>> rows = new DotConnect()
                .setSQL(SELECT_BY_VANITY_URL_ID_ANY_LANGUAGE)
                .addParam(endpointId)
                .addParam(vanityUrlId)
                .loadObjectResults();

        return aliasesFromRows(rows);
    }

    /**
     * Transactionally replaces all mappings associated with the lookup key.
     *
     * @param lookup logical mapping key
     * @param aliases mappings to save
     * @throws DotDataException when writing mappings fails
     */
    @WrapInTransaction
    public void replaceMappings(final S3VanityAliasLookup lookup,
                                final List<S3VanityAlias> aliases) throws DotDataException {
        deleteByLookupInternal(lookup);
        for (final S3VanityAlias alias : aliases) {
            insertAlias(alias);
        }
    }

    /**
     * Transactionally replaces all mappings associated with one Vanity URL.
     *
     * @param endpointId static endpoint identifier
     * @param languageId Vanity URL language identifier
     * @param vanityUrlId Vanity URL identifier
     * @param aliases mappings to save
     * @throws DotDataException when writing mappings fails
     */
    @WrapInTransaction
    public void replaceMappingsByVanityUrlId(final String endpointId, final long languageId,
                                             final String vanityUrlId,
                                             final List<S3VanityAlias> aliases) throws DotDataException {
        deleteByVanityUrlIdInternal(endpointId, languageId, vanityUrlId);
        for (final S3VanityAlias alias : aliases) {
            insertAlias(alias);
        }
    }

    /**
     * Removes one materialized mapping.
     *
     * @param alias alias to remove from the table
     * @throws DotDataException when deleting the mapping fails
     */
    @WrapInTransaction
    public void deleteAlias(final S3VanityAlias alias) throws DotDataException {
        new DotConnect().executeUpdate(DELETE_ALIAS, false,
                alias.endpointId,
                alias.hostId,
                alias.languageId,
                pathHash(alias.canonicalPath),
                alias.canonicalPath,
                pathHash(alias.vanityPath),
                alias.vanityPath);
    }

    /**
     * Removes all mappings associated with the lookup key.
     *
     * @param lookup logical mapping key
     * @throws DotDataException when deleting mappings fails
     */
    @WrapInTransaction
    public void deleteByLookup(final S3VanityAliasLookup lookup) throws DotDataException {
        deleteByLookupInternal(lookup);
    }

    /**
     * Removes all mappings for a Vanity URL in a specific language.
     *
     * @param endpointId static endpoint identifier
     * @param languageId Vanity URL language identifier
     * @param vanityUrlId Vanity URL identifier
     * @throws DotDataException when deleting mappings fails
     */
    @WrapInTransaction
    public void deleteByVanityUrlId(final String endpointId, final long languageId,
                                    final String vanityUrlId) throws DotDataException {
        deleteByVanityUrlIdInternal(endpointId, languageId, vanityUrlId);
    }

    /**
     * Removes all mappings for a Vanity URL across all languages.
     *
     * @param endpointId static endpoint identifier
     * @param vanityUrlId Vanity URL identifier
     * @throws DotDataException when deleting mappings fails
     */
    @WrapInTransaction
    public void deleteByVanityUrlId(final String endpointId,
                                    final String vanityUrlId) throws DotDataException {
        new DotConnect().executeUpdate(DELETE_BY_VANITY_URL_ID_ANY_LANGUAGE, false, endpointId, vanityUrlId);
    }

    /**
     * Removes all mappings associated with the lookup key.
     *
     * @param lookup logical mapping key
     * @throws DotDataException when deleting mappings fails
     */
    private void deleteByLookupInternal(final S3VanityAliasLookup lookup) throws DotDataException {
        new DotConnect().executeUpdate(DELETE_BY_LOOKUP, false,
                lookup.endpointId,
                lookup.hostId,
                lookup.languageId,
                pathHash(lookup.canonicalPath),
                lookup.canonicalPath);
    }

    /**
     * Removes all mappings associated with one Vanity URL.
     *
     * @param endpointId static endpoint identifier
     * @param languageId Vanity URL language identifier
     * @param vanityUrlId Vanity URL identifier
     * @throws DotDataException when deleting mappings fails
     */
    private void deleteByVanityUrlIdInternal(final String endpointId, final long languageId,
                                             final String vanityUrlId)
            throws DotDataException {
        new DotConnect().executeUpdate(DELETE_BY_VANITY_URL_ID, false, endpointId, languageId, vanityUrlId);
    }

    /**
     * Inserts one materialized mapping.
     *
     * @param alias mapping to save
     * @throws DotDataException when writing the mapping fails
     */
    private void insertAlias(final S3VanityAlias alias) throws DotDataException {
        new DotConnect().executeUpdate(INSERT_ALIAS, false,
                alias.endpointId,
                alias.hostId,
                alias.languageId,
                alias.canonicalPath,
                pathHash(alias.canonicalPath),
                alias.vanityPath,
                pathHash(alias.vanityPath),
                alias.vanityUrlId,
                alias.bucketName,
                alias.bucketRegion,
                alias.bucketPrefix);
    }

    /**
     * Converts SQL rows into domain mappings.
     *
     * @param rows rows read from the database
     * @return matching vanity mappings
     */
    private List<S3VanityAlias> aliasesFromRows(final List<Map<String, Object>> rows) {
        final List<S3VanityAlias> aliases = new ArrayList<>();
        for (final Map<String, Object> row : rows) {
            aliases.add(aliasFromRow(row));
        }
        return aliases;
    }

    /**
     * Converts one SQL row into a domain mapping.
     *
     * @param row row read from the database
     * @return matching vanity mapping
     */
    private S3VanityAlias aliasFromRow(final Map<String, Object> row) {
        return new S3VanityAlias(
                stringValue(row.get("endpoint_id")),
                stringValue(row.get("host_id")),
                longValue(row.get("language_id")),
                stringValue(row.get("canonical_path")),
                stringValue(row.get("vanity_path")),
                stringValue(row.get("vanity_url_id")),
                stringValue(row.get("bucket_name")),
                stringValue(row.get("bucket_region")),
                stringValue(row.get("bucket_prefix")));
    }

    /**
     * Converts a SQL value into a string.
     *
     * @param value SQL value
     * @return string value
     */
    private String stringValue(final Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * Converts a SQL value into a long.
     *
     * @param value SQL value
     * @return numeric value
     */
    private long longValue(final Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    /**
     * Calculates a deterministic short key for potentially long paths.
     *
     * @param path path to normalize
     * @return hexadecimal SHA-256 hash
     */
    private String pathHash(final String path) {
        return DigestUtils.sha256Hex(path);
    }
}
