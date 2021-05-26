package com.dotcms.storage;

import static com.dotcms.storage.model.BasicMetadataFields.SHA256_META_KEY;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.util.CloseUtils;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.FileByteSplitter;
import com.dotcms.util.FileJoiner;
import com.dotcms.util.ReturnableDelegate;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotCorruptedDataException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.liferay.util.Encryptor;
import com.liferay.util.Encryptor.Hashing;
import com.liferay.util.HashBuilder;
import com.liferay.util.PropertiesUtil;
import com.liferay.util.StringPool;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;

/**
 * Represents a Storage on the database It supports big files since provides the ability to split
 * fat object in smaller pieces, see {@link FileByteSplitter} and {@link com.dotcms.util.FileJoiner}
 * to get more details about the process
 *
 * @author jsanca
 */
public class DataBaseStoragePersistenceAPIImpl implements StoragePersistenceAPI {

    private static final String DATABASE_STORAGE_JDBC_POOL_NAME = "DATABASE_STORAGE_JDBC_POOL_NAME";
    private static final String DB_STORAGE_CHUNK_SIZE = "DB_STORAGE_CHUNK_SIZE";

    /**
     * custom external connection provider method in case we want to store stuff outside our db
     */
    protected Connection getConnection() throws SQLException {

        final String jdbcPool = Config
                .getStringProperty(DATABASE_STORAGE_JDBC_POOL_NAME, StringPool.BLANK);
        final boolean isExternalPool = UtilMethods.isSet(jdbcPool);

        return isExternalPool ?
                DbConnectionFactory.getConnection(jdbcPool) :
                DbConnectionFactory.getDataSource().getConnection();
    }

    /**
     * custom transaction wrapper
     */
    protected <T> T wrapInTransaction(final ReturnableDelegate<T> delegate)
            throws DotDataException {

        final String jdbcPool = Config
                .getStringProperty(DATABASE_STORAGE_JDBC_POOL_NAME, StringPool.BLANK);
        final boolean isExternalPool = UtilMethods.isSet(jdbcPool);

        if (isExternalPool) {
            return this.wrapInExternalTransaction(delegate, jdbcPool);
        } else {
            try {
                return LocalTransaction.wrapReturnWithListeners(delegate);
            } catch (Exception e) {
                Logger.error(DataBaseStoragePersistenceAPIImpl.class, e.getMessage(), e);
                throw new DotDataException(e);
            }
        }
    }

    /**
     * custom transaction wrapper
     */
    private <T> T wrapInExternalTransaction(final ReturnableDelegate<T> delegate,
            final String jdbcPool) {

        T result = null;
        Connection connection = null;
        boolean autocommit = false;

        try {

            connection = DbConnectionFactory.getConnection(jdbcPool);
            autocommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            result = delegate.execute();

            connection.commit();
        } catch (Throwable e) {

            if (null != connection) {

                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    throw new DotRuntimeException(ex);
                }
            }

            throw new DotRuntimeException(e);
        } finally {

            if (null != connection) {

                try {
                    connection.setAutoCommit(autocommit);
                } catch (SQLException e) {

                    Logger.error(this, e.getMessage(), e);
                } finally {

                    CloseUtils.closeQuietly(connection);
                }
            }
        }

        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @param groupName {@link String} group name
     */
    @Override
    public boolean existsGroup(final String groupName) throws DotDataException {
        try (Connection connection = getConnection()) {
            return existsGroup(groupName, connection);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

    /**
     * internal connection aware version
     * @param groupName
     * @param connection
     * @return
     * @throws DotDataException
     */
    private boolean existsGroup(final String groupName,final Connection connection) throws DotDataException {
        final String groupNameLC = groupName.toLowerCase();
        final MutableBoolean exists = new MutableBoolean(false);
            final Number results = Try
                    .of(() -> {
                                final List<Map<String, Object>> result = new DotConnect()
                                        .setSQL("SELECT count(*) as x FROM storage_group WHERE group_name = ?")
                                        .addParam(groupNameLC)
                                        .loadObjectResults(connection);
                                return (Number) result.get(0).get("x");
                            }
                    ).getOrElse(0);
            exists.setValue(results.intValue() > 0);
        return exists.booleanValue();
    }

    /**
     * {@inheritDoc}
     *
     * @param groupName {@link String}
     * @param objectPath {@link String}
     */
    @Override
    public boolean existsObject(final String groupName, final String objectPath)
            throws DotDataException {
        try (Connection connection = getConnection()) {
            return existsObject(groupName, objectPath, connection);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

    /**
     * internal connection aware version
     * @param groupName
     * @param objectPath
     * @param connection
     * @return
     */
    private boolean existsObject(final String groupName, final String objectPath,
            final Connection connection) {
        final String groupNameLC = groupName.toLowerCase();
        final String objectPathLC = objectPath.toLowerCase();
        final Number results = Try
                .of(() -> {
                            final List<Map<String, Object>> result = new DotConnect()
                                    .setSQL("SELECT count(*) as x FROM storage WHERE group_name = ? AND path = ?")
                                    .addParam(groupNameLC).addParam(objectPathLC)
                                    .loadObjectResults(connection);
                            return (Number) result.get(0).get("x");
                        }
                ).getOrElse(0);
        return results.intValue() > 0;
    }

    /**
     * {@inheritDoc}
     *
     * @param groupName {@link String} group name
     */
    @Override
    public boolean createGroup(final String groupName) throws DotDataException {
        return createGroup(groupName, ImmutableMap.of());
    }

    /**
     * {@inheritDoc}
     *
     * @param groupName {@link String} group name
     * @param extraOptions {@link Map} depending on the implementation it might need extra options
     * or not.
     */
    @Override
    public boolean createGroup(final String groupName, final Map<String, Object> extraOptions)
            throws DotDataException {
        final String groupNameLC = groupName.toLowerCase();
        return wrapInTransaction(() ->
                Try.of(() -> {
                    try (Connection connection = getConnection()) {
                        new DotConnect()
                                .setSQL(" INSERT INTO storage_group (group_name) VALUES (?) ")
                                .addParam(groupNameLC).loadResult(
                                connection);
                        return true;
                    }
                }).getOrElseGet(throwable -> false)
        );
    }

    /**
     * {@inheritDoc}
     *
     * @param groupName {@link String} group name
     */
    @Override
    public int deleteGroup(final String groupName) throws DotDataException {
        final String groupNameLC = groupName.toLowerCase();
        return wrapInTransaction(() -> {
            try (Connection connection = getConnection()) {
                final DotConnect dotConnect = new DotConnect();

                final List<Map<String, Object>> maps = dotConnect
                        .setSQL("SELECT hash FROM storage WHERE group_name = ?")
                        .addParam(groupNameLC)
                        .loadObjectResults(connection);

                int count = deleteObjects(maps.stream().map(map -> map.get("hash").toString())
                        .collect(Collectors.toSet()), dotConnect, connection);

                final int storageEntriesCount = dotConnect.executeUpdate(connection,
                        "DELETE FROM storage WHERE group_name = ?", groupNameLC);

                final int groupsCount = dotConnect.executeUpdate(connection,
                        "DELETE FROM storage_group WHERE group_name = ?", groupNameLC);

                Logger.debug(this, () -> String
                        .format("total of `%d` objects allocated in `%d` removed for `%d` group. ",
                                storageEntriesCount, count, groupsCount));
                return storageEntriesCount;
            }
        });

    }

    /**
     * {@inheritDoc}
     *
     * @param groupName {@link String} group name
     * @param path {   @link String} object path
     */
    @Override
    public boolean deleteObjectAndReferences(final String groupName, final String path) throws DotDataException {
        final String groupNameLC = groupName.toLowerCase();
        final String pathLC = path.toLowerCase();
        return wrapInTransaction(() -> {
            try (Connection connection = getConnection()) {

                final DotConnect dotConnect = new DotConnect();

                final List<Map<String, Object>> results = dotConnect
                        .setSQL("SELECT hash FROM storage WHERE group_name = ? AND path = ? ")
                        .addParam(groupNameLC).addParam(pathLC).loadObjectResults(connection);

                if (!results.isEmpty()){
                    final String hash = (String) results.get(0).get("hash");
                    deleteObjects(ImmutableSet.of(hash), dotConnect, connection);
                }
                final int count = dotConnect
                        .executeUpdate(connection,
                                "DELETE FROM storage WHERE group_name = ? AND path = ?",
                                groupNameLC, pathLC);
                return count > 0;
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * @param groupName
     * @param path
     * @return
     * @throws DotDataException
     */
    @Override
    public boolean deleteObjectReference(final String groupName, final String path) throws DotDataException {
        final String groupNameLC = groupName.toLowerCase();
        final String pathLC = path.toLowerCase();
        return wrapInTransaction(() -> {
            try (Connection connection = getConnection()) {

                final DotConnect dotConnect = new DotConnect();

                final List<Map<String, Object>> results = dotConnect
                        .setSQL("SELECT hash FROM storage WHERE group_name = ? AND path = ? ")
                        .addParam(groupNameLC).addParam(pathLC).loadObjectResults(connection);

                if (!results.isEmpty()){
                    final String hash = (String) results.get(0).get("hash");

                    //This basically tells me if there other entries besides this ones
                    // pointing to the stored data. If so we do not delete them.
                    if(!hasFurtherReferences(hash, groupNameLC, pathLC, connection)){
                        deleteObjects(ImmutableSet.of(hash), dotConnect, connection);
                    }
                }
                final int count = dotConnect
                        .executeUpdate(connection,
                                "DELETE FROM storage WHERE group_name = ? AND path = ?",
                                groupNameLC, pathLC);
                return count > 0;
            }
        });
    }

    /**
     *
     * @param hash
     * @param groupNameLC
     * @param objectPathLC
     * @param connection
     * @return
     * @throws DotDataException
     */
    private boolean hasFurtherReferences(final String hash, final String groupNameLC, final String objectPathLC, final Connection connection)
            throws DotDataException {
        final List<Map<String, Object>> result = new DotConnect()
                .setSQL("SELECT count(*) as x FROM storage WHERE hash = ? AND (group_name <> ? OR path <> ?) ")
                .addParam(hash).addParam(groupNameLC).addParam(objectPathLC)
                .loadObjectResults(connection);
        return ((Number) result.get(0).get("x")).intValue() > 0;
    }


    /**
     * object reference removal
     *
     * @param storageHashSet object hash id
     * @param dotConnect DotConnect
     * @param connection external connection
     * @return count of all removed objects
     */
    private int deleteObjects(final Set<String> storageHashSet, final DotConnect dotConnect,
            final Connection connection)
            throws DotDataException {
        int count = 0;
        for (final String storageHash : storageHashSet) {

            final Set<String> dataIdHashSet = dotConnect
                    .setSQL("SELECT data_hash FROM storage_x_data WHERE storage_hash = ?")
                    .addParam(storageHash).loadObjectResults().stream().map(r -> (String) r.get("data_hash"))
                    .collect(Collectors.toSet());

            count += dotConnect.executeUpdate(connection,
                    "DELETE FROM storage_x_data WHERE storage_hash = ?", storageHash);

            for (final String dataHash : dataIdHashSet) {

                //Check it is safe to remove the data chunk if none else's still referencing it from another entry.
                if (dotConnect
                        .setSQL("SELECT data_hash FROM storage_x_data WHERE data_hash = ?")
                        .addParam(dataHash).loadObjectResults().isEmpty()) {
                    count += dotConnect.executeUpdate(connection,
                            "DELETE FROM storage_data WHERE hash_id = ?", dataHash);
                }

            }

        }
        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> listGroups() throws DotDataException {

        final MutableObject<List<String>> result = new MutableObject<>(Collections.emptyList());

        try (Connection connection = getConnection()) {

            final List<Map<String, Object>> results = Try
                    .of(() -> new DotConnect().setSQL("SELECT group_name FROM storage_group")
                            .loadObjectResults(connection)).getOrElse(Collections::emptyList);

            result.setValue(results.stream().map(map -> map.get("group_name").toString())
                    .collect(CollectionsUtils.toImmutableList()));
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }

        return result.getValue();
    }

    @Override
    public Object pushFile(final String groupName, final String path,
            final File file, final Map<String, Serializable> extraMeta) throws DotDataException {

        // 1. generate metadata
        // 2. see if the sha-256 exists
        // 2.1 if exists only insert on the reference
        // 2.2 if does not exists, insert a new one
        final Map<String, Serializable> metaData = hashFile(file, extraMeta);
        final String fileHash = (String) metaData.get(SHA256_META_KEY.key());
        final String hashRef = (String)extraMeta.get(HASH_REF);
        Logger.debug(DataBaseStoragePersistenceAPIImpl.class, " fileHash is : " + fileHash);

            return wrapInTransaction(
                    () -> {
                        try (final Connection connection = getConnection()) {
                            if (!existsGroup(groupName, connection)) {
                                throw new IllegalArgumentException("The groupName: " + groupName +
                                        ", does not exist.");
                            }
                            if (this.existsObject(groupName, path, connection)) {
                                deleteObjectAndReferences(groupName, path);
                                Logger.warn(DataBaseStoragePersistenceAPIImpl.class,
                                        String.format("The existing entry `%s/%s` has been completely replaced.", groupName, path));
                            }
                            return existsHashReference(fileHash, connection) ?
                                    pushFileReference(groupName, path, fileHash, hashRef, connection) :
                                    pushNewFile(groupName, path, fileHash, hashRef, file, connection);
                        }
                    });

    }

    /**
     * Vali
     * @param fileHash
     * @param connection
     * @return
     */
    private boolean existsHashReference(final String fileHash, final Connection connection) {
        final MutableBoolean exists = new MutableBoolean(false);
            final Number results = Try
                    .of(() -> {
                                final List<Map<String, Object>> result = new DotConnect()
                                        .setSQL("SELECT count(*) as x FROM storage_x_data WHERE storage_hash = ?")
                                        .addParam(fileHash)
                                        .loadObjectResults(connection);
                                return (Number) result.get(0).get("x");
                            }
                    ).getOrElse(0);
        final boolean found = results.intValue() > 0;
        Logger.debug(DataBaseStoragePersistenceAPIImpl.class, String.format(" is HashReference [%s] found [%s] ", fileHash,
                BooleanUtils.toStringYesNo(found)));
        exists.setValue(found);
        return exists.getValue();
    }
    
    private Object pushFileReference(final String groupName, final String path, final String fileHash, final String hashRef, final Connection connection) throws DotDataException {
        final String groupNameLC = groupName.toLowerCase();
        final String pathLC = path.toLowerCase();
        Logger.debug(DataBaseStoragePersistenceAPIImpl.class, String.format("Pushing new reference for group [%s] path [%s] hash [%s]", groupNameLC, pathLC, hashRef));
        try {
            UpsertDelegate.newInstance().pushObjectReference(connection, fileHash, pathLC, groupNameLC, hashRef);
            return true;
        } catch (DotDataException e) {
            Logger.error(DataBaseStoragePersistenceAPIImpl.class, e.getMessage(), e);
            throw new DotDataException(e);
        }
    }

    private Object pushNewFile(final String groupName, final String path,
             final String fileHash, final String hashRef, final File file, final Connection connection ) {
        final String groupNameLC = groupName.toLowerCase();
        final String pathLC = path.toLowerCase();
        final UpsertDelegate upsertDelegate = UpsertDelegate.newInstance();
        try (final FileByteSplitter fileSplitter = new FileByteSplitter(file,
                Config.getIntProperty(DB_STORAGE_CHUNK_SIZE, 2048))) {

            final HashBuilder objectHashBuilder = Encryptor.Hashing.sha256();
            final List<String> chunkHashes = new LinkedList<>();

            for (final Tuple2<byte[], Integer> bytesRead : fileSplitter) {

                objectHashBuilder.append(bytesRead._1(), bytesRead._2());
                final String chunkHash = Encryptor.Hashing.sha256().append
                        (bytesRead._1(), bytesRead._2()).buildUnixHash();
                chunkHashes.add(chunkHash);
                    upsertDelegate.pushDataChunk(connection, chunkHash, bytesRead._1().length == bytesRead._2() ?
                            bytesRead._1() : chunkBytes(bytesRead._2(), bytesRead._1()));
            }

            final String objectHash = objectHashBuilder.buildUnixHash();

            assert objectHash.equals(fileHash) : "File hash and objectHash must match." ;

            upsertDelegate.pushHashReference(connection, objectHash, chunkHashes);

            upsertDelegate.pushObjectReference(connection, objectHash, pathLC, groupNameLC, hashRef);

            return true;
        } catch (DotDataException | NoSuchAlgorithmException | IOException e) {
            Logger.error(DataBaseStoragePersistenceAPIImpl.class, e.getMessage(), e);
            throw new DotRuntimeException(String.format("Exception pushing new file. group=[%s], path=[%s], fileHash=[%s]", groupNameLC, pathLC, fileHash), e);
        }

    }

    /**
     * if the param `hashObject` is set then the file itself is decomposed into a sha254 representation
     * so the entries in storage_data table point to the metadata-file bytes and not to the binary owning the meta-data.
     * @param file
     * @param extraMeta
     * @return
     */
    private Map<String, Serializable> hashFile(final File file,
            final Map<String, Serializable> extraMeta) {

        if(UtilMethods.isSet(extraMeta)) {
            //This is here to correct a behavior where we were using the sha256 of the binary file to store the metadata
            //So when we were recovering the Metadata file from db again we would end-up getting the binary
            //So this instructs the Api to regenerate the sha256 with the temp file which has the Metadata written to it.
            if (Try.of(() -> (Boolean) extraMeta.get(HASH_OBJECT)).getOrElse(false)) {
                //WE're saving the prior hash value (as hasRef) and replacing it with the new object hash.
                extraMeta.put(HASH_REF, extraMeta.put(SHA256_META_KEY.key(),
                        Try.of(() -> FileUtil.sha256toUnixHash(file)).getOrElse("unknown")));
            }

            if (extraMeta.containsKey(SHA256_META_KEY.key())) {

                return extraMeta;
            }
        }
        final ImmutableMap.Builder<String, Serializable> metaData = new ImmutableMap.Builder<>();

        if (UtilMethods.isSet(extraMeta)) {

            metaData.putAll(extraMeta);
        }

        metaData.putAll(APILocator.getFileStorageAPI().generateRawBasicMetaData(file));

        return metaData.build();
    }

    /**
     * returns byte array chunks of certain length
     * @param bytesLength
     * @param bytes
     * @return
     */
    private byte[] chunkBytes(final int bytesLength, final byte[] bytes) {

        final byte[] chunkArray = new byte[bytesLength];

        System.arraycopy(bytes, 0, chunkArray, 0, bytesLength);

        return chunkArray;
    }

    @Override
    public Object pushObject(final String groupName, final String path,
            final ObjectWriterDelegate writerDelegate,
            final Serializable object, final Map<String, Serializable> extraMeta)
            throws DotDataException {
        File file = null;
        try {
            file = FileUtil.createTemporaryFile("object-storage", ".tmp");
            writeToFile(writerDelegate, object, file);
            return this.pushFile(groupName, path, file, extraMeta);
        } catch (Exception e) {
            Logger.error(DataBaseStoragePersistenceAPIImpl.class, e.getMessage(), e);
            throw new DotDataException(e);
        } finally {
            if(null != file){
               file.delete();
            }
        }
    }

    /**
     * This will write directly from the Serializer delegate right into a file. No in memory loading
     * takes place like this.
     */
    private void writeToFile(final ObjectWriterDelegate writerDelegate,
            final Serializable object, File file) throws IOException {

        try (final OutputStream outputStream = Files.newOutputStream(file.toPath())) {
            writerDelegate.write(outputStream, object);
        }
    }

    @Override
    public Future<Object> pushFileAsync(final String groupName, final String path, final File file,
            final Map<String, Serializable> extraMeta) {

        return DotConcurrentFactory.getInstance().getSubmitter(STORAGE_POOL).submit(
                () -> this.pushFile(groupName, path, file, extraMeta)
        );
    }

    @Override
    public Future<Object> pushObjectAsync(final String bucketName, final String path,
            final ObjectWriterDelegate writerDelegate,
            final Serializable object, final Map<String, Serializable> extraMeta) {

        return DotConcurrentFactory.getInstance().getSubmitter(STORAGE_POOL).submit(
                () -> this.pushObject(bucketName, path, writerDelegate, object, extraMeta)
        );
    }

    @Override
    public File pullFile(final String groupName, final String path) throws DotDataException {
        final String groupNameLC = groupName.toLowerCase();
        final String pathLC = path.toLowerCase();
        final MutableObject<File> file = new MutableObject<>(null);

        try (Connection connection = getConnection()) {
            final List<Map<String, Object>> storageResult = Try.of(() ->
                    new DotConnect()
                            .setSQL("SELECT hash FROM storage WHERE group_name = ? AND path = ?")
                            .addParam(groupNameLC).addParam(pathLC)
                            .loadObjectResults(connection)).getOrElse(Collections::emptyList);

            if (!storageResult.isEmpty()) {

                final Optional<Object> objectOpt = storageResult.stream()
                        .map(map -> map.get("hash")).findFirst();

                final String objectHash = (String) objectOpt.get();
                if (UtilMethods.isSet(objectHash)) {
                    file.setValue(this.createJoinFile(objectHash, connection));
                }
            } else {
                throw new DoesNotExistException(
                        "The storage, group: " + groupName + ", path: " + path
                                + " does not exists");
            }

        } catch (Exception e) {
            Logger.error(DataBaseStoragePersistenceAPIImpl.class, String.format("error pulling file for group `%s`, and path `%s`", groupName, pathLC), e);
            throw new DotDataException(e);
        }

        return file.getValue();
    }

    private File createJoinFile(final String hashId, final Connection connection) throws IOException, DotDataException {
        final File file = FileUtil.createTemporaryFile("dot-db-storage-recovery", ".tmp", true);
        try (final FileJoiner fileJoiner = new FileJoiner(file)) {
            final HashBuilder fileHashBuilder = Try.of(Hashing::sha256)
                    .getOrElseThrow(DotRuntimeException::new);

            final PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT storage_data.hash_id AS hash, storage_data.data AS data FROM storage_data , storage_x_data " +
                        "WHERE storage_x_data.data_hash  =  storage_data.hash_id " +
                        "AND storage_x_data.storage_hash = ? order by data_order ASC ");
            preparedStatement.setString(1, hashId);
            preparedStatement.setFetchSize(1);
            final ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                final String hash = resultSet.getString("hash");
                final byte[] bytes = resultSet.getBytes("data");
                final String recoveredHash = Try
                        .of(() -> Encryptor.Hashing.sha256().append(bytes).buildUnixHash())
                        .getOrElseThrow(DotRuntimeException::new);
                if (hash.equals(recoveredHash)) {
                    fileJoiner.join(bytes, 0, bytes.length);
                    fileJoiner.flush();
                    fileHashBuilder.append(bytes);
                } else {
                    throw new DotCorruptedDataException(" Checksum hash verification failure. The chunk is not valid");
                }
            }

            if (!hashId.equals(fileHashBuilder.buildUnixHash())) {
                throw new DotCorruptedDataException(String.format(
                        "The file hash `%s` isn't valid. it doesn't match the records in `storage_data/storage_x_data` or they don't exist. ",
                        hashId));
            }
        } catch (Exception e) {
            throw new DotDataException(e.getMessage(), e);
        }

        return file;
    }

    @Override
    public Object pullObject(final String groupName, final String path,
            final ObjectReaderDelegate readerDelegate) throws DotDataException {

        Object object = null;
        final File file = pullFile(groupName, path);

        if (null != file) {
            object = Try.of(() -> {
                        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
                            return readerDelegate.read(inputStream);
                        }
                    }
            ).getOrNull();
        }

        return object;
    }

    @Override
    public Future<File> pullFileAsync(final String groupName, final String path) {

        return DotConcurrentFactory.getInstance().getSubmitter(STORAGE_POOL).submit(
                () -> this.pullFile(groupName, path)
        );
    }

    @Override
    public Future<Object> pullObjectAsync(final String groupName, final String path,
            final ObjectReaderDelegate readerDelegate) {

        return DotConcurrentFactory.getInstance().getSubmitter(STORAGE_POOL).submit(
                () -> this.pullObject(groupName, path, readerDelegate)
        );
    }

    /**
     * SQL Upsert abstract Base Implementation
     */
    static abstract class UpsertDelegate {

        final DotConnect dotConnect;

        final Supplier<String> storageInsertSQL;

        final Supplier<String> dataInsertSQL;

        final Supplier<String> hashReferenceInsertSQL;

         UpsertDelegate(final DotConnect dotConnect,
                final Supplier<String> storageInsertSQL,
                final Supplier<String> dataInsertSQL,
                final Supplier<String> hashReferenceInsertSQL) {
            this.dotConnect = dotConnect;
            this.storageInsertSQL = storageInsertSQL;
            this.dataInsertSQL = dataInsertSQL;
            this.hashReferenceInsertSQL = hashReferenceInsertSQL;
        }

        /**
         * inserts reference into storage
         * @param connection
         * @param objectHash
         * @param path
         * @param groupName
         * @param hashRef
         * @throws DotDataException
         */
        void pushObjectReference(final Connection connection, final String objectHash, final String path, final String groupName, final String hashRef)
                throws DotDataException {
            final int rows = dotConnect.executeUpdate(connection,
                    storageInsertSQL.get(),
                    objectHash, path, groupName, hashRef);
            Logger.debug(DataBaseStoragePersistenceAPIImpl.class,"pushObjectReference inserted rows "+rows);
        }

        /**
         * inserts data chunks into storage_data and have them associated with a  chunk hash
         * @param connection
         * @param chunkHash
         * @param data
         * @throws DotDataException
         */
        void pushDataChunk(final Connection connection, final String chunkHash, final byte [] data)
                throws DotDataException {
            final int rows = dotConnect.executeUpdate(connection,
                    dataInsertSQL.get(), chunkHash, data);
            Logger.debug(DataBaseStoragePersistenceAPIImpl.class,"pushDataChunk inserted rows "+rows);
        }

        /**
         * inserts references into storage_x_data
         * @param connection
         * @param objectHash
         * @param chunkHashes
         * @throws DotDataException
         */
        void pushHashReference(final Connection connection, final String objectHash, final List<String> chunkHashes)
                throws DotDataException {
            final String sql = hashReferenceInsertSQL.get();
            int order = 1;
            for (final String chunkHash : chunkHashes) {
                final int rows = dotConnect.executeUpdate(connection, sql, objectHash, chunkHash, order++);
                Logger.debug(DataBaseStoragePersistenceAPIImpl.class,"pushHashReference inserted rows "+rows);
            }
        }

        /**
         * Factory method
         * @return
         */
        static UpsertDelegate newInstance() {
            if(DbConnectionFactory.isPostgres()){
               return new PostgresUpsertDelegate(new DotConnect());
            }
            if(DbConnectionFactory.isMySql()){
                return new MySQLUpsertDelegate(new DotConnect());
            }
            if(DbConnectionFactory.isMsSql()){
                return new MSSQLUpsertDelegate(new DotConnect());
            }
            if(DbConnectionFactory.isOracle()){
                return new OracleUpsertDelegate(new DotConnect());
            }
            throw new DotRuntimeException("Oh snap! dunno what database I'm running on.");
        }

    }

    /**
     * Postgres SQL Upsert
     */
    static class PostgresUpsertDelegate extends UpsertDelegate {

        static final String STORAGE_INSERT = "INSERT INTO storage(hash, path, group_name, hash_ref) VALUES (?, ?, ?, ?) ON CONFLICT (path, group_name) DO NOTHING";

        static final String STORAGE_DATA_INSERT ="INSERT INTO storage_data(hash_id, data) VALUES (?, ?) ON CONFLICT (hash_id) DO NOTHING";

        static final String HASH_REFERENCE_INSERT = "INSERT INTO storage_x_data(storage_hash, data_hash, data_order) VALUES (?, ?, ?) ON CONFLICT (storage_hash,data_hash) DO NOTHING";

        /**
         * Constructor
         * @param dotConnect
         */
        PostgresUpsertDelegate(final DotConnect dotConnect) {
            super(dotConnect, ()->STORAGE_INSERT, ()->STORAGE_DATA_INSERT, ()->HASH_REFERENCE_INSERT);
        }

    }

    /**
     * My SQL Upsert
     */
    static class MySQLUpsertDelegate extends UpsertDelegate {

        static final String STORAGE_INSERT = "INSERT INTO storage(hash, path, group_name, hash_ref) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE path = path, group_name = group_name ";

        static final String STORAGE_DATA_INSERT ="INSERT INTO storage_data(hash_id, data) VALUES (?, ?) ON DUPLICATE KEY UPDATE hash_id = hash_id, mod_date = CURRENT_TIMESTAMP ";

        static final String HASH_REFERENCE_INSERT = "INSERT INTO storage_x_data(storage_hash, data_hash, data_order) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE storage_hash = storage_hash, data_hash = data_hash";

        /**
         * Constructor
         * @param dotConnect
         */
        MySQLUpsertDelegate(final DotConnect dotConnect) {
            super(dotConnect, ()->STORAGE_INSERT, ()->STORAGE_DATA_INSERT, ()->HASH_REFERENCE_INSERT);
        }

    }

    /**
     * MS-SQL Upsert specific
     */
    static class MSSQLUpsertDelegate extends UpsertDelegate {

        static final String STORAGE_INSERT =
                "MERGE storage WITH (HOLDLOCK) AS [Target] \n"
                + "USING ( VALUES (?, ?, ?, ?) ) AS [Source] (hash, path, group_name, hash_ref) ON ([Target].path = [Source].path AND [Target].group_name = [Source].group_name)  \n"
                + "WHEN MATCHED THEN \n"
                + "  UPDATE SET [Target].path = [Source].path, [Target].group_name = [Source].group_name \n"
                + "WHEN NOT MATCHED THEN \n"
                + "  INSERT (hash, path, group_name, hash_ref) VALUES ([Source].hash, [Source].path, [Source].group_name, [Source].hash_ref);";

        static final String STORAGE_DATA_INSERT =
                "MERGE storage_data WITH (HOLDLOCK) AS [Target] \n"
                + "USING (VALUES (?, ?) ) AS [Source] (hash_id, data) ON ([Target].hash_id = [Source].hash_id)  \n"
                + "WHEN MATCHED THEN \n"
                + "  UPDATE SET [Target].hash_id = [Source].hash_id, [Target].data = [Source].data, [Target].mod_date = GETDATE() \n"
                + "WHEN NOT MATCHED THEN \n"
                + "  INSERT (hash_id, data) VALUES ([Source].hash_id, [Source].data);";

        static final String HASH_REFERENCE_INSERT =
                "MERGE storage_x_data WITH (HOLDLOCK) AS [Target] \n"
                + "USING ( VALUES(?, ?, ?) ) AS [Source] (storage_hash, data_hash, data_order) ON ([Target].storage_hash = [Source].storage_hash  AND  [Target].data_hash = [Source].data_hash )  \n"
                + "WHEN MATCHED THEN \n"
                + "  UPDATE SET [Target].storage_hash = [Source].storage_hash, [Target].data_hash = [Source].data_hash, [Target].data_order = [Source].data_order \n"
                + "WHEN NOT MATCHED THEN \n"
                + "  INSERT (storage_hash, data_hash, data_order) VALUES ([Source].storage_hash,[Source].data_hash,[Source].data_order);\n";

        /**
         * Constructor
         * @param dotConnect
         */
        MSSQLUpsertDelegate(final DotConnect dotConnect) {
            super(dotConnect, ()->STORAGE_INSERT, ()->STORAGE_DATA_INSERT, ()->HASH_REFERENCE_INSERT);
        }

    }

    /**
     * Oracle Upsert SQL Specific
     */
    static class OracleUpsertDelegate extends UpsertDelegate {

        static final String STORAGE_INSERT =
               "MERGE INTO storage Target  \n"
                + "USING (SELECT ? PATH, ? GROUP_NAME, ? HASH FROM DUAL) Source \n"
                + "ON (Target.PATH = Source.PATH AND Target.GROUP_NAME = Source.GROUP_NAME) WHEN MATCHED THEN \n"
                + "  UPDATE SET Target.hash = Source.hash \n"
                + "WHEN NOT MATCHED THEN \n"
                + "  INSERT (hash, path, group_name, hash_ref) \n"
                + "  VALUES (?, ?, ?, ?)\n";

        static final String STORAGE_DATA_INSERT =

               "MERGE INTO storage_data Target \n"
                + "USING (SELECT ? HASH_ID, ? DATA FROM DUAL) Source \n"
                + "ON (Target.HASH_ID = Source.HASH_ID ) WHEN MATCHED THEN \n"
                + " UPDATE SET Target.DATA = Source.DATA, Target.MOD_DATE = CURRENT_TIMESTAMP  \n"
                + "WHEN NOT MATCHED THEN  \n"
                + " INSERT (hash_id, data) VALUES (?, ?)\n";

        static final String HASH_REFERENCE_INSERT =
               "MERGE INTO storage_x_data Target  \n"
                + "USING (SELECT ? STORAGE_HASH, ? DATA_HASH, ? DATA_ORDER FROM DUAL) Source \n"
                + "ON (Target.STORAGE_HASH = Source.STORAGE_HASH AND Target.DATA_HASH = Source.DATA_HASH ) WHEN MATCHED THEN \n"
                + " UPDATE SET Target.DATA_ORDER = Source.DATA_ORDER, Target.MOD_DATE = CURRENT_TIMESTAMP \n"
                + "WHEN NOT MATCHED THEN  \n"
                + " INSERT (storage_hash, data_hash, data_order) VALUES (?, ?, ?)\n";

        /**
         * Constructor
         * @param dotConnect
         */
        OracleUpsertDelegate(final DotConnect dotConnect) {
            super(dotConnect, ()->STORAGE_INSERT, ()->STORAGE_DATA_INSERT, ()->HASH_REFERENCE_INSERT);
        }

        /**
         * As the SQL is a bit more complex and requires more params to be replaced we need to adjust
         * @param connection
         * @param objectHash
         * @param path
         * @param groupName
         * @param hashRef
         * @throws DotDataException
         */
        @Override
        void pushObjectReference(final Connection connection, final String objectHash, final String path, final String groupName, final String hashRef)
                throws DotDataException {
            final int rows = dotConnect.executeUpdate(connection,
                    storageInsertSQL.get(),
                    path, groupName, objectHash,
                    objectHash, path, groupName, hashRef);
            Logger.debug(DataBaseStoragePersistenceAPIImpl.class,"pushObjectReference inserted rows "+rows);
        }

        /**
         * As the SQL is a bit more complex and requires more params to be replaced we need to adjust
         * @param connection
         * @param chunkHash
         * @param data
         * @throws DotDataException
         */
        @Override
        void pushDataChunk(final Connection connection, final String chunkHash, final byte [] data)
                throws DotDataException {
            final int rows = dotConnect.executeUpdate(connection,
                    dataInsertSQL.get(),
                    chunkHash, data,
                    chunkHash, data);
            Logger.debug(DataBaseStoragePersistenceAPIImpl.class,"pushDataChunk inserted rows "+rows);
        }

        /**
         * As the SQL is a bit more complex and requires more params to be replaced we need to adjust
         * @param connection
         * @param objectHash
         * @param chunkHashes
         * @throws DotDataException
         */
        @Override
        void pushHashReference(final Connection connection, final String objectHash, final List<String> chunkHashes)
                throws DotDataException {
            final String sql = hashReferenceInsertSQL.get();
            int order = 1;
            for (final String chunkHash : chunkHashes) {
                final int rows = dotConnect.executeUpdate(connection, sql,
                        objectHash, chunkHash, order,
                        objectHash, chunkHash, order++);
                Logger.debug(DataBaseStoragePersistenceAPIImpl.class,"pushHashReference inserted rows "+rows);
            }
        }

    }


}