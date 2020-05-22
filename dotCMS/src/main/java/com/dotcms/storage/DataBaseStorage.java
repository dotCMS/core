package com.dotcms.storage;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.FileSplitter;
import com.dotcms.util.user.FileByteSplitter;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.liferay.util.Encryptor;
import com.liferay.util.HashBuilder;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.commons.io.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;

public class DataBaseStorage implements Storage {

    @CloseDBIfOpened
    @Override
    public boolean existsGroup(final String groupName) {

        final List results = Try.of(() -> new DotConnect().setSQL("select * from storage where storage_group = ?")
                .addParam(groupName).loadObjectResults()).getOrElse(() -> Collections.emptyList());
        return !results.isEmpty();
    }

    @CloseDBIfOpened
    @Override
    public boolean existsObject(final String groupName, final String objectPath) {

        final List results = Try.of(()->new DotConnect().setSQL("select * from storage where storage_group = ? and key = ?")
                .addParam(groupName).addParam(objectPath)
                .loadObjectResults()).getOrElse(()-> Collections.emptyList());
        return !results.isEmpty();
    }

    @Override
    public boolean createGroup(final String groupName) {

        return true; // bucket here are dynamic
    }

    @Override
    public boolean createGroup(final  String groupName, final Map<String, Object> extraOptions) {

        return  true; // bucket here are dynamic;
    }

    @WrapInTransaction
    @Override
    public boolean deleteGroup(final String groupName) {

        return Try.of(()-> new DotConnect() // todo: this should remove all dependencies
                    .executeUpdate("delete from storage where storage_group = ?", groupName) > 0
                ).getOrElse(false);
    }

    @WrapInTransaction
    @Override
    public boolean deleteObject(final String groupName, final String path) {

        return Try.of(()-> new DotConnect() // todo: this should remove all dependencies
                    .executeUpdate("delete from storage where storage_group = ? and key = ?", groupName, path) > 0
                ).getOrElse(false);
    }

    @CloseDBIfOpened
    @Override
    public List<Object> listGroups() {

        final List<Map<String, Object>> results = Try.of(()->new DotConnect().setSQL("select storage_group from storage")
                .loadObjectResults()).getOrElse(()-> Collections.emptyList());

        return results.stream().map(map -> map.get("storage_group")).collect(CollectionsUtils.toImmutableList());
    }

    @WrapInTransaction
    @Override
    public Object pushFile(final String groupName, final String path,
                           final File file, final Map<String, Object> extraMeta) {

        try (final FileByteSplitter fileSplitter = new FileByteSplitter(file)) {

            final HashBuilder objectHashBuilder = Encryptor.Hashing.sha256();
            final List<String> chuckHashes      = new ArrayList<>();

            for (final Tuple2<byte[], Integer> bytesRead : fileSplitter) {

                objectHashBuilder.append(bytesRead._1(), bytesRead._2());
                final String chuckHash = Encryptor.Hashing.sha256().append
                        (bytesRead._1(), bytesRead._2()).buildUnixHash();
                chuckHashes.add(chuckHash);
                new DotConnect().executeUpdate("insert into storage_objects_data(hash_id, object) values (?, ?)",
                        chuckHash,
                                    bytesRead._1().length == bytesRead._2()?
                                        bytesRead._1(): this.chuckBytes (bytesRead._2(), bytesRead._1()));
            }

            final String objectHash = objectHashBuilder.buildUnixHash();
            new DotConnect().executeUpdate("insert into storage(hash_id, key, storage_group) values (?, ?, ?)",
                    objectHash, path, groupName);

            int order = 1;
            for (final String chuckHash : chuckHashes) {

                new DotConnect().executeUpdate("insert into storage_objects(storage_hash, data_hash, data_order) values (?, ?, ?)",
                        objectHash, chuckHash, order++);
            }

            return true;
        } catch (DotDataException | NoSuchAlgorithmException | IOException e) {

            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    private  byte[]  chuckBytes(final int bytesLength, final byte[] bytes) {

        return bytes; // todo: create a bytes array with the bytesLength
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

    @WrapInTransaction
    @Override
    public Object pushObject(final String bucketName, final String path,
                             final ObjectWriterDelegate writerDelegate,
                             final Object object, final Map<String, Object> extraMeta) {
        try {

            // todo: continue here
            return new DotConnect().executeUpdate("insert into storage(bucket, path, object) values (?, ?, ?)",
                    bucketName, path, this.objectToBytes (writerDelegate, object)) > 0;
        } catch (DotDataException e) {

            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    private byte[] objectToBytes(final ObjectWriterDelegate writerDelegate,
                                 final Object object) {

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();  // todo not sure if I should compress this
        writerDelegate.write(byteArrayOutputStream, object);
        return  byteArrayOutputStream.toByteArray();
    }

    @Override
    public Future<Object> pushFileAsync(final String bucketName, final String path, final File file, final Map<String, Object> extraMeta) {
        return null; // todo: implement me
    }

    @Override
    public Future<Object> pushObjectAsync(final String bucketName, final String path, final ObjectWriterDelegate writerDelegate, final Object object, final Map<String, Object> extraMeta) {
        return null; // todo: implement me
    }

    @CloseDBIfOpened
    @Override
    public File pullFile(final String bucketName, final String path) {

        File file = null;

        try {

            final List<Map<String, Object>> results = Try.of(()->new DotConnect().setSQL("select object from storage where bucket = ? and path = ?")
                    .addParam(bucketName).addParam(path).loadObjectResults()).getOrElse(()-> Collections.emptyList());

            if (!results.isEmpty()) {

                final Optional<Object> objectOpt = results.stream().map(map-> map.get("object")).findFirst();
                if (objectOpt.isPresent()) {

                    final byte[] bytes = (byte[]) objectOpt.get();
                    if (null != bytes && bytes.length > 0) {

                        file = FileUtil.createTemporalFile("recovery", "tmp");
                        FileUtils.writeByteArrayToFile(file, bytes);
                    }
                }
            }
        } catch (IOException e) {

            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }

        return file;
    }

    @CloseDBIfOpened
    @Override
    public Object pullObject(final String bucketName, final String path, final ObjectReaderDelegate readerDelegate) {

        Object object = null;

        final List<Map<String, Object>> results = Try.of(()->new DotConnect().setSQL("select object from storage where bucket = ? and path = ?")
                .addParam(bucketName).addParam(path).loadObjectResults()).getOrElse(()-> Collections.emptyList());

        if (!results.isEmpty()) {

            final Optional<Object> objectOpt = results.stream().map(map-> map.get("object")).findFirst();
            if (objectOpt.isPresent()) {

                final byte[] bytes = (byte[]) objectOpt.get();
                if (null != bytes && bytes.length > 0) {

                    object = readerDelegate.read(new ByteArrayInputStream(bytes));
                }
            }
        }

        return object;
    }

    @Override
    public Future<File> pullFileAsync(String bucketName, String path) {
        return null; // todo: implement me
    }

    @Override
    public Future<Object> pullObjectAsync(String bucketName, String path, ObjectReaderDelegate readerDelegate) {
        return null; // todo: implement me
    }
}
