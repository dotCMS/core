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
 * Represents a Storage on the database
 * It supports big files since provides the ability to split fat object in smaller pieces,
 * see {@link FileByteSplitter} and {@link com.dotcms.util.FileJoiner} to get more details about the process
 * @author jsanca
 */
public class DataBaseStorage implements Storage {

    private final ObjectMapper objectMapper = new ObjectMapper();

    protected Connection getConnection () {

        final String jdbcPool        = Config.getStringProperty("DATABASE_STORAGE_JDBC_POOL_NAME", StringPool.BLANK);
        final boolean isExternalPool = UtilMethods.isSet(jdbcPool);

        return isExternalPool?
                DbConnectionFactory.getConnection(jdbcPool):
                DbConnectionFactory.getConnection();
    }

    protected void wrapCloseConnection (final VoidDelegate voidDelegate) {

        final String  jdbcPool       = Config.getStringProperty("DATABASE_STORAGE_JDBC_POOL_NAME", StringPool.BLANK);
        final boolean isExternalPool = UtilMethods.isSet(jdbcPool);

        if(isExternalPool) {

            wrapExternalCloseConnection(voidDelegate, jdbcPool);
        } else {

            wrapLocalCloseConnection(voidDelegate);
        }
    }

    private void wrapLocalCloseConnection(final VoidDelegate voidDelegate) {

        final boolean isNewConnection = !DbConnectionFactory.connectionExists();

        try {

            voidDelegate.execute();
        }  catch (DotSecurityException | DotDataException e) {

            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e);
        } finally {

            if (isNewConnection) {

                DbConnectionFactory.closeSilently();
            }
        }
    }

    private void wrapExternalCloseConnection(final VoidDelegate voidDelegate, final String jdbcPool) {

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

    protected  <T> T wrapInTransaction (final ReturnableDelegate<T> delegate) {

        final String  jdbcPool       = Config.getStringProperty("DATABASE_STORAGE_JDBC_POOL_NAME", StringPool.BLANK);
        final boolean isExternalPool = UtilMethods.isSet(jdbcPool);

        if(isExternalPool) {

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

    private <T> T wrapInExternalTransaction(final ReturnableDelegate<T> delegate, final String jdbcPool) {

        T result = null;
        Connection connection = null;
        boolean    autocommit = false;

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

        final MutableBoolean result = new MutableBoolean(false);

        this.wrapCloseConnection(()-> {

            final List results = Try.of(() -> new DotConnect().setSQL("select * from storage where storage_group = ?")
                    .addParam(groupName).loadObjectResults(this.getConnection())).getOrElse(() -> Collections.emptyList());
            result.setValue(!results.isEmpty());
        });

        return result.booleanValue();
    }

    @Override
    public boolean existsObject(final String groupName, final String objectPath) {

        final MutableBoolean result = new MutableBoolean(false);

        this.wrapCloseConnection(()-> {

            final List results = Try.of(()->new DotConnect().setSQL("select * from storage where storage_group = ? and key = ?")
                    .addParam(groupName).addParam(objectPath)
                    .loadObjectResults(this.getConnection())).getOrElse(()-> Collections.emptyList());
            result.setValue(!results.isEmpty());
        });

        return result.booleanValue();
    }

    @Override
    public boolean createGroup(final String groupName) {

        return true; // bucket here are dynamic
    }

    @Override
    public boolean createGroup(final  String groupName, final Map<String, Object> extraOptions) {

        return  true; // bucket here are dynamic;
    }

    @Override
    public int deleteGroup(final String groupName) {

        return this.wrapInTransaction(()->
                new DotConnect()
                        .executeUpdate(this.getConnection(),
                                "delete from storage where storage_group = ?", groupName));

    }

    @Override
    public boolean deleteObject(final String groupName, final String path) {

        return this.wrapInTransaction(()->
                new DotConnect()
                        .executeUpdate(this.getConnection(),
                                "delete from storage where storage_group = ? and key = ?", groupName, path) > 0);
    }

    @Override
    public List<Object> listGroups() {

        final MutableObject<List<Object>> result = new MutableObject<>(Collections.emptyList());

        this.wrapCloseConnection(()-> {

            final List<Map<String, Object>> results = Try.of(()->new DotConnect().setSQL("select storage_group from storage")
                    .loadObjectResults()).getOrElse(()-> Collections.emptyList());

            result.setValue(results.stream().map(map -> map.get("storage_group")).collect(CollectionsUtils.toImmutableList()));
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
        final Map<String, Object> metaData = processMetadata (file, extraMeta);
        final String fileHash              = (String)metaData.get(FileStorageAPI.SHA226_META_KEY);

        return this.wrapInTransaction(
                ()->this.existsHash(fileHash)?
                    this.pushFileReference(groupName, path, metaData, fileHash):
                    this.pushNewFile(groupName, path, file, metaData));
    }

    private boolean existsHash(final String fileHash) {

        final MutableBoolean exists = new MutableBoolean(false);

        this.wrapCloseConnection(()-> {

            final List results = Try.of(()->new DotConnect().setSQL("select * from storage where hash = ?")
                    .addParam(fileHash)
                    .loadObjectResults(this.getConnection())).getOrElse(()-> Collections.emptyList());
            exists.setValue(!results.isEmpty());
        });

        return exists.getValue();
    }

    private Map<String, Object> processMetadata(final File file, final Map<String, Object> extraMeta) {

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

        try {
            final StringWriter metaDataJsonWriter = new StringWriter();
            this.objectMapper.writeValue(metaDataJsonWriter, extraMeta);
            new DotConnect().executeUpdate(this.getConnection(),
                    "insert into storage(hash, key, storage_group, metadata) values (?, ?, ?, ?)",
                    objectHash, path, groupName, metaDataJsonWriter.toString());
            return true;
        } catch (DotDataException | IOException e) {

            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    private Object pushNewFile(final String groupName, final String path, final File file, final Map<String, Object> extraMeta) {

        try (final FileByteSplitter fileSplitter = new FileByteSplitter(file)) {

            final HashBuilder objectHashBuilder = Encryptor.Hashing.sha256();
            final List<String> chuckHashes      = new ArrayList<>();

            for (final Tuple2<byte[], Integer> bytesRead : fileSplitter) {

                objectHashBuilder.append(bytesRead._1(), bytesRead._2());
                final String chuckHash = Encryptor.Hashing.sha256().append
                        (bytesRead._1(), bytesRead._2()).buildUnixHash();
                chuckHashes.add(chuckHash);
                new DotConnect().executeUpdate(this.getConnection(), "insert into storage_data(hash_id, data) values (?, ?)", // todo: this could be an upsert
                        chuckHash,
                                    bytesRead._1().length == bytesRead._2()?
                                        bytesRead._1(): this.chuckBytes (bytesRead._2(), bytesRead._1()));
            }

            final String objectHash               = objectHashBuilder.buildUnixHash();
            final StringWriter metaDataJsonWriter = new StringWriter();
            this.objectMapper.writeValue(metaDataJsonWriter, extraMeta);
            new DotConnect().executeUpdate(this.getConnection(),
                    "insert into storage(hash, key, storage_group, metadata) values (?, ?, ?, ?)",
                    objectHash, path, groupName, metaDataJsonWriter.toString());

            int order = 1;
            for (final String chuckHash : chuckHashes) {

                new DotConnect().executeUpdate(this.getConnection(),"insert into storage_x_data(storage_hash, data_hash, data_order) values (?, ?, ?)",
                        objectHash, chuckHash, order++);
            }

            return true;
        } catch (DotDataException | NoSuchAlgorithmException | IOException e) {

            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    private  byte[]  chuckBytes(final int bytesLength, final byte[] bytes) {

        final byte[] chunkedArray = new byte[bytesLength];

        System.arraycopy(bytes, 0, chunkedArray, 0, bytesLength);

        return chunkedArray;
    }

    private byte[] fileToBytes(final File file) {

        try (InputStream input = com.liferay.util.FileUtil.createInputStream(file.toPath(), "none")) {

            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();  // todo not sure if I should compress this
            final byte[] buffer = new byte[1024];
            for (int readNum; (readNum = input.read(buffer)) != -1;) {

                byteArrayOutputStream.write(buffer, 0, readNum);
            }

            return  byteArrayOutputStream.toByteArray();
        } catch (IOException e) {

            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    @Override
    public Object pushObject(final String groupName, final String path,
                             final ObjectWriterDelegate writerDelegate,
                             final Serializable object, final Map<String, Object> extraMeta) {

        try {

            final File file                     = FileUtil.createTemporalFile("object-storage", ".tmp");
            final byte[] objectBytes            = this.objectToBytes (writerDelegate, object);
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
        return  byteArrayOutputStream.toByteArray();
    }

    @Override
    public Future<Object> pushFileAsync(final String bucketName, final String path, final File file, final Map<String, Object> extraMeta) {

        return DotConcurrentFactory.getInstance().getSubmitter(STORAGE_POOL).submit(
                ()-> this.pushFile(bucketName, path, file, extraMeta)
        );
    }

    @Override
    public Future<Object> pushObjectAsync(final String bucketName, final String path, final ObjectWriterDelegate writerDelegate,
                                          final Serializable object, final Map<String, Object> extraMeta) {

        return DotConcurrentFactory.getInstance().getSubmitter(STORAGE_POOL).submit(
                ()-> this.pushObject(bucketName, path, writerDelegate, object, extraMeta)
        );
    }

    @Override
    public File pullFile(final String groupName, final String path) {

        final MutableObject<File> file = new MutableObject<>(null);

        this.wrapCloseConnection( ()-> {
             try {

                final List<Map<String, Object>> storageResult = Try.of(() ->
                        new DotConnect().setSQL("select hash from storage where storage_group = ? and key = ?")
                            .addParam(groupName).addParam(path).loadObjectResults()).getOrElse(() -> Collections.emptyList());

                if (!storageResult.isEmpty()) {

                    final Optional<Object> objectOpt = storageResult.stream().map(map -> map.get("hash")).findFirst();
                    if (objectOpt.isPresent()) {

                        final String objectHash = (String) objectOpt.get();
                        if (UtilMethods.isSet(objectHash)) {

                            file.setValue(this.createJoinFile(objectHash));
                        }
                    }
                } else {

                    throw new DoesNotExistException("The storage, group: " + groupName + ", path: " + path + " does not exists");
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

            final HashBuilder fileHashBuilder = Try.of(()-> Encryptor.Hashing.sha256()).getOrElseThrow(DotRuntimeException::new);
            final List<Map<String, Object>> hashes = Try.of(() -> new DotConnect().setSQL(
                    "select storage_data.hash_id as hash, storage_data.data as data from storage_data , storage_x_data " +
                            "where storage_x_data.data_hash  =  storage_data.hash_id " +
                            "and storage_x_data.storage_hash = ? order by data_order asc")
                    .setFetchSize(1).addParam(hashId).loadObjectResults(this.getConnection())).getOrElse(() -> Collections.emptyList());

            for (final Map<String, Object> hashMap : hashes) {

                final String hash        = (String) hashMap.get("hash");
                final byte[] bytes       = (byte[]) hashMap.get("data"); // todo: this could be a getInputStream
                final String recoverHash = Try.of(() -> Encryptor.Hashing.sha256().append(bytes).buildUnixHash()).getOrElseThrow(DotRuntimeException::new);

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
    public Object pullObject(final String groupName, final String path, final ObjectReaderDelegate readerDelegate) {

        Object object   = null;
        final File file = pullFile(groupName, path);

        if (null != file) {

            object = Try.of(()->readerDelegate.read(
                    new ByteArrayInputStream(FileUtils.readFileToByteArray(file)))).getOrNull();
        }

        return object;
    }

    @Override
    public Future<File> pullFileAsync(final String groupName, final String path) {

        return DotConcurrentFactory.getInstance().getSubmitter(STORAGE_POOL).submit(
                ()-> this.pullFile(groupName, path)
        );
    }

    @Override
    public Future<Object> pullObjectAsync(final String groupName, final String path,
                                          final ObjectReaderDelegate readerDelegate) {

        return DotConcurrentFactory.getInstance().getSubmitter(STORAGE_POOL).submit(
                ()-> this.pullObject(groupName, path, readerDelegate)
        );
    }
}
