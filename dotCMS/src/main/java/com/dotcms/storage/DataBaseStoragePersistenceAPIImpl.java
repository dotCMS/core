package com.dotcms.storage;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.util.CloseUtils;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.FileByteSplitter;
import com.dotcms.util.FileJoiner;
import com.dotcms.util.ReturnableDelegate;
import com.dotcms.util.VoidDelegate;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotCorruptedDataException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.liferay.util.Encryptor;
import com.liferay.util.HashBuilder;
import com.liferay.util.StringPool;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;

/**
 * Represents a Storage on the database It supports big files since provides the ability to split
 * fat object in smaller pieces, see {@link FileByteSplitter} and {@link com.dotcms.util.FileJoiner}
 * to get more details about the process
 *
 * @author jsanca
 */
public class DataBaseStoragePersistenceAPIImpl implements StoragePersistenceAPI {

    private final ObjectMapper objectMapper = new ObjectMapper();

    protected Connection getConnection() {

        final String jdbcPool = Config
                .getStringProperty("DATABASE_STORAGE_JDBC_POOL_NAME", StringPool.BLANK);
        final boolean isExternalPool = UtilMethods.isSet(jdbcPool);

        return isExternalPool ?
                DbConnectionFactory.getConnection(jdbcPool) :
                DbConnectionFactory.getConnection();
    }

    protected void wrapCloseConnection(final VoidDelegate voidDelegate) {

        final String jdbcPool = Config
                .getStringProperty("DATABASE_STORAGE_JDBC_POOL_NAME", StringPool.BLANK);
        final boolean isExternalPool = UtilMethods.isSet(jdbcPool);

        if (isExternalPool) {

            wrapExternalCloseConnection(voidDelegate, jdbcPool);
        } else {

            wrapLocalCloseConnection(voidDelegate);
        }
    }

    private void wrapLocalCloseConnection(final VoidDelegate voidDelegate) {

        final boolean isNewConnection = !DbConnectionFactory.connectionExists();

        try {

            voidDelegate.execute();
        } catch (DotSecurityException | DotDataException e) {

            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e);
        } finally {

            if (isNewConnection) {

                DbConnectionFactory.closeSilently();
            }
        }
    }

    private void wrapExternalCloseConnection(final VoidDelegate voidDelegate,
            final String jdbcPool) {

        Connection connection = null;
        try {

            connection = DbConnectionFactory.getConnection(jdbcPool);
            voidDelegate.execute();
        } catch (DotSecurityException | DotDataException e) {

            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e);
        } finally {

            CloseUtils.closeQuietly(connection);
        }
    }

    protected <T> T wrapInTransaction(final ReturnableDelegate<T> delegate) {

        final String jdbcPool = Config
                .getStringProperty("DATABASE_STORAGE_JDBC_POOL_NAME", StringPool.BLANK);
        final boolean isExternalPool = UtilMethods.isSet(jdbcPool);

        if (isExternalPool) {

            return this.wrapInExternalTransaction(delegate, jdbcPool);
        } else {

            try {

                return LocalTransaction.wrapReturnWithListeners(delegate);
            } catch (Exception e) {

                Logger.error(this, e.getMessage(), e);
                throw new DotRuntimeException(e);
            }
        }
    }

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

    @Override
    public boolean existsGroup(final String groupName) {
        final String groupNameLC = groupName.toLowerCase();
        final MutableBoolean result = new MutableBoolean(false);

        this.wrapCloseConnection(() -> {

            final List results = Try.of(() -> new DotConnect()
                    .setSQL("SELECT * FROM storage_group WHERE group_name = ?")
                    .addParam(groupNameLC).loadObjectResults(this.getConnection())).getOrElse(
                    Collections::emptyList);
            result.setValue(!results.isEmpty());
        });

        return result.booleanValue();
    }

    @Override
    public boolean existsObject(final String groupName, final String objectPath) {
        final String groupNameLC = groupName.toLowerCase();
        final String objectPathLC = objectPath.toLowerCase();
        final MutableBoolean result = new MutableBoolean(false);

        this.wrapCloseConnection(() -> {

            final List results = Try.of(() -> new DotConnect()
                    .setSQL("SELECT * FROM storage WHERE group_name = ? AND path = ?")
                    .addParam(groupNameLC).addParam(objectPathLC)
                    .loadObjectResults(this.getConnection())).getOrElse(Collections::emptyList);
            result.setValue(!results.isEmpty());
        });

        return result.booleanValue();
    }

    @Override
    public boolean createGroup(final String groupName) {
        return createGroup(groupName, ImmutableMap.of());
    }

    @Override
    public boolean createGroup(final String groupName, final Map<String, Object> extraOptions) {
        final String groupNameLC = groupName.toLowerCase();
        return this.wrapInTransaction(() ->
                Try.of(() -> {
                    new DotConnect().setSQL(" INSERT INTO storage_group (group_name) VALUES (?) ")
                            .addParam(groupNameLC).loadResult(
                            this.getConnection());
                    return true;
                }).getOrElseGet(throwable -> false)
        );
    }

    @Override
    public int deleteGroup(final String groupName) {
        final String groupNameLC = groupName.toLowerCase();
        return this.wrapInTransaction(() -> {
            final Connection connection = this.getConnection();
            final DotConnect dotConnect = new DotConnect();

            final String hash = dotConnect
                    .setSQL("SELECT hash FROM storage WHERE group_name = ?").addParam(groupNameLC)
                    .getString("hash");

            final int dataPiecesCount = dotConnect.executeUpdate(connection,
                    "DELETE FROM storage_x_data WHERE data_hash = ?", hash);

            final int storageDataCount = dotConnect.executeUpdate(connection,
                    "DELETE FROM storage_data WHERE hash_id = ?", hash);

            final int storageEntriesCount = dotConnect.executeUpdate(connection,
                    "DELETE FROM storage WHERE group_name = ?", groupNameLC);

            final int groupsCount = dotConnect.executeUpdate(connection,
                    "DELETE FROM storage_group WHERE group_name = ?", groupNameLC);

            Logger.info(this, () -> String
                    .format("total of `%d` objects allocated in `%d` and split in `%d` removed for `%d` group. ",
                            storageEntriesCount, storageDataCount, dataPiecesCount, groupsCount));
            return storageEntriesCount;
        });

    }

    @Override
    public boolean deleteObject(final String groupName, final String path) {
        final String groupNameLC = groupName.toLowerCase();
        final String pathLC = path.toLowerCase();
        //TODO: Delete other objects in cascade
        return this.wrapInTransaction(() -> {
            final Connection connection = this.getConnection();
            final DotConnect dotConnect = new DotConnect();
            final int count = dotConnect
                    .executeUpdate(connection,
                    "DELETE FROM storage WHERE group_name = ? AND path = ?",
                            groupNameLC, pathLC);
            return count > 0;
        });
    }

    @Override
    public List<String> listGroups() {

        final MutableObject<List<String>> result = new MutableObject<>(Collections.emptyList());

        this.wrapCloseConnection(() -> {

            final List<Map<String, Object>> results = Try
                    .of(() -> new DotConnect().setSQL("SELECT group_name FROM storage_group")
                            .loadObjectResults(this.getConnection())).getOrElse(Collections::emptyList);

            result.setValue(results.stream().map(map -> map.get("group_name").toString())
                    .collect(CollectionsUtils.toImmutableList()));
        });

        return result.getValue();
    }

    @Override
    public Object pushFile(final String groupName, final String path,
            final File file, final Map<String, Object> extraMeta) {

        // 1. generate metadata
        // 2. see if the sha-256 exists
        // 2.1 if exists only insert on the reference
        // 2.2 if does not exists, insert a new one
        final Map<String, Object> metaData = processMetadata(file, extraMeta);
        final String fileHash = (String) metaData.get(FileStorageAPI.SHA226_META_KEY);

        return this.wrapInTransaction(
                () -> {
                    if (!existsGroup(groupName)) {
                        throw new IllegalArgumentException("The groupName: " + groupName +
                                ", does not exist.");
                    }
                    if (this.existsObject(groupName, path)) {
                        Logger.warn(DataBaseStoragePersistenceAPIImpl.class,
                                String.format("Attempt to override entry `%s/%s` ", groupName,
                                        path));
                        return false;
                    }
                    return this.existsHash(fileHash) ?
                            this.pushFileReference(groupName, path, metaData, fileHash) :
                            this.pushNewFile(groupName, path, file, metaData);
                });
    }

    /**
     * Selects directly on storage-data since hash is the primary key there.
     * @param fileHash
     * @return
     */
    private boolean existsHash(final String fileHash) {
        final MutableBoolean exists = new MutableBoolean(false);
        this.wrapCloseConnection(() -> {
            final Number results = Try
                    .of(() -> {
                                final List<Map<String, Object>> result = new DotConnect()
                                        .setSQL("SELECT count(*) as x FROM storage_data WHERE hash_id = ?")
                                        .addParam(fileHash)
                                        .loadObjectResults(this.getConnection());
                                return (Number) result.get(0).get("x");
                            }
                    ).getOrElse(0);
            exists.setValue(results.intValue() > 0);
        });

        return exists.getValue();
    }

    private Map<String, Object> processMetadata(final File file,
            final Map<String, Object> extraMeta) {

        if (UtilMethods.isSet(extraMeta) && extraMeta.containsKey(FileStorageAPI.SHA226_META_KEY)) {

            return extraMeta;
        }

        final ImmutableMap.Builder<String, Object> metaData = new ImmutableMap.Builder<>();

        if (UtilMethods.isSet(extraMeta)) {

            metaData.putAll(extraMeta);
        }

        metaData.putAll(APILocator.getFileStorageAPI().generateRawBasicMetaData(file));

        return metaData.build();
    }

    private Object pushFileReference(final String groupName, final String path,
            final Map<String, Object> extraMeta, final String objectHash) {
        final String groupNameLC = groupName.toLowerCase();
        final String pathLC = path.toLowerCase();
        try {
            final StringWriter metaDataJsonWriter = new StringWriter();
            this.objectMapper.writeValue(metaDataJsonWriter, extraMeta);
            new DotConnect().executeUpdate(this.getConnection(),
                    "INSERT INTO storage(hash, path, group_name, metadata) VALUES (?, ?, ?, ?)",
                    objectHash, pathLC, groupNameLC, metaDataJsonWriter.toString());
            return true;
        } catch (DotDataException | IOException e) {

            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    private Object pushNewFile(final String groupName, final String path, final File file,
            final Map<String, Object> extraMeta) {
        final String groupNameLC = groupName.toLowerCase();
        final String pathLC = path.toLowerCase();
        try (final FileByteSplitter fileSplitter = new FileByteSplitter(file)) {

            final HashBuilder objectHashBuilder = Encryptor.Hashing.sha256();
            final List<String> chunkHashes = new ArrayList<>();

            for (final Tuple2<byte[], Integer> bytesRead : fileSplitter) {

                objectHashBuilder.append(bytesRead._1(), bytesRead._2());
                final String chunkHash = Encryptor.Hashing.sha256().append
                        (bytesRead._1(), bytesRead._2()).buildUnixHash();
                chunkHashes.add(chunkHash);
                new DotConnect().executeUpdate(this.getConnection(),
                        "INSERT INTO storage_data(hash_id, data) VALUES (?, ?)",
                        chunkHash,
                        bytesRead._1().length == bytesRead._2() ?
                                bytesRead._1() : this.chunkBytes(bytesRead._2(), bytesRead._1()));
            }

            final String objectHash = objectHashBuilder.buildUnixHash();
            final StringWriter metaDataJsonWriter = new StringWriter();
            this.objectMapper.writeValue(metaDataJsonWriter, extraMeta);
            new DotConnect().executeUpdate(this.getConnection(),
                    "INSERT INTO storage(hash, path, group_name, metadata) VALUES (?, ?, ?, ?)",
                    objectHash, pathLC, groupNameLC, metaDataJsonWriter.toString());

            int order = 1;
            for (final String chunkHash : chunkHashes) {

                new DotConnect().executeUpdate(this.getConnection(),
                        "INSERT INTO storage_x_data(storage_hash, data_hash, data_order) VALUES (?, ?, ?)",
                        objectHash, chunkHash, order++);
            }

            return true;
        } catch (DotDataException | NoSuchAlgorithmException | IOException e) {

            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    private byte[] chunkBytes(final int bytesLength, final byte[] bytes) {

        final byte[] chunkedArray = new byte[bytesLength];

        System.arraycopy(bytes, 0, chunkedArray, 0, bytesLength);

        return chunkedArray;
    }

    @Override
    public Object pushObject(final String groupName, final String path,
            final ObjectWriterDelegate writerDelegate,
            final Serializable object, final Map<String, Object> extraMeta) {

        try {

            final File file = FileUtil.createTemporalFile("object-storage", ".tmp");
            final byte[] objectBytes = this.objectToBytes(writerDelegate, object);
            FileUtils.writeByteArrayToFile(file, objectBytes);

            return this.pushFile(groupName, path, file, extraMeta);
        } catch (IOException e) {

            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    private byte[] objectToBytes(final ObjectWriterDelegate writerDelegate,
            final Serializable object) {

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        writerDelegate.write(byteArrayOutputStream, object);
        return byteArrayOutputStream.toByteArray();
    }

    @Override
    public Future<Object> pushFileAsync(final String groupName, final String path, final File file,
            final Map<String, Object> extraMeta) {

        return DotConcurrentFactory.getInstance().getSubmitter(STORAGE_POOL).submit(
                () -> this.pushFile(groupName, path, file, extraMeta)
        );
    }

    @Override
    public Future<Object> pushObjectAsync(final String bucketName, final String path,
            final ObjectWriterDelegate writerDelegate,
            final Serializable object, final Map<String, Object> extraMeta) {

        return DotConcurrentFactory.getInstance().getSubmitter(STORAGE_POOL).submit(
                () -> this.pushObject(bucketName, path, writerDelegate, object, extraMeta)
        );
    }

    @Override
    public File pullFile(final String groupName, final String path) {
        final String groupNameLC = groupName.toLowerCase();
        final String pathLC = path.toLowerCase();
        final MutableObject<File> file = new MutableObject<>(null);

        this.wrapCloseConnection(() -> {
            try {

                final List<Map<String, Object>> storageResult = Try.of(() ->
                        new DotConnect()
                                .setSQL("SELECT hash FROM storage WHERE group_name = ? AND path = ?")
                                .addParam(groupNameLC).addParam(pathLC).loadObjectResults()).getOrElse(
                        Collections::emptyList);

                if (!storageResult.isEmpty()) {

                    final Optional<Object> objectOpt = storageResult.stream()
                            .map(map -> map.get("hash")).findFirst();
                    if (objectOpt.isPresent()) {

                        final String objectHash = (String) objectOpt.get();
                        if (UtilMethods.isSet(objectHash)) {

                            file.setValue(this.createJoinFile(objectHash));
                        }
                    }
                } else {

                    throw new DoesNotExistException(
                            "The storage, group: " + groupName + ", path: " + path
                                    + " does not exists");
                }
            } catch (IOException e) {

                Logger.error(this, e.getMessage(), e);
                throw new DotRuntimeException(e);
            }
        });

        return file.getValue();
    }

    private File createJoinFile(final String hashId) throws IOException {

        final File file = FileUtil.createTemporalFile("dotdbstorage-recovery", ".tmp");
        try (final FileJoiner fileJoiner = new FileJoiner(file)) {

            final HashBuilder fileHashBuilder = Try.of(() -> Encryptor.Hashing.sha256())
                    .getOrElseThrow(DotRuntimeException::new);
            final List<Map<String, Object>> hashes = Try.of(() -> new DotConnect().setSQL(
                    "SELECT storage_data.hash_id AS hash, storage_data.data AS data FROM storage_data , storage_x_data "
                            +
                            "WHERE storage_x_data.data_hash  =  storage_data.hash_id " +
                            "AND storage_x_data.storage_hash = ? order by data_order ASC")
                    .setFetchSize(1).addParam(hashId).loadObjectResults(this.getConnection()))
                    .getOrElse(() -> Collections.emptyList());

            for (final Map<String, Object> hashMap : hashes) {

                final String hash = (String) hashMap.get("hash");
                final byte[] bytes = (byte[]) hashMap
                        .get("data"); // todo: this could be a getInputStream: must
                final String recoverHash = Try
                        .of(() -> Encryptor.Hashing.sha256().append(bytes).buildUnixHash())
                        .getOrElseThrow(DotRuntimeException::new);

                if (hash.equals(recoverHash)) {

                    fileJoiner.join(bytes, 0, bytes.length);
                    fileJoiner.flush();
                    fileHashBuilder.append(bytes);
                } else {

                    throw new DotCorruptedDataException("The chunks hash is not valid");
                }
            }

            if (!hashId.equals(fileHashBuilder.buildUnixHash())) {

                throw new DotCorruptedDataException("The file hash is not valid");
            }
        }

        return file;
    }

    @Override
    public Object pullObject(final String groupName, final String path,
            final ObjectReaderDelegate readerDelegate) {

        Object object = null;
        final File file = pullFile(groupName, path);

        if (null != file) {

            object = Try.of(() -> readerDelegate.read(
                    new ByteArrayInputStream(FileUtils.readFileToByteArray(file)))).getOrNull();
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
}
