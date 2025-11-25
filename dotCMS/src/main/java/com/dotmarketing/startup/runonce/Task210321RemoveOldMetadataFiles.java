package com.dotmarketing.startup.runonce;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.mutable.MutableInt;

/**
 *  This task takes care of removing all the old metadata files.
 *  Because Now the reindex thread regenerates the new metadata and internally the heuristic used to determine
 *  if the metadata needs to be generated will skip it if it finds there's a file already in place.
 *  That's why all the old files must be removed.
 */
public class Task210321RemoveOldMetadataFiles implements StartupTask {

    private Future<Tuple2<Integer, Integer>> future;

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        final Path serverDir = Paths.get(APILocator.getFileAssetAPI().getRealAssetsRootPath()).normalize();
        Logger.info(Task210321RemoveOldMetadataFiles.class," executing metadata clean up under path "+serverDir);
        final DotSubmitter submitter = DotConcurrentFactory.getInstance()
                .getSubmitter(DotConcurrentFactory.DOT_SYSTEM_THREAD_POOL);
        this.future = submitter.submit(() -> deleteMetadataFiles(serverDir));
    }


    private Tuple2<Integer,Integer> deleteMetadataFiles(Path serverDir) {
        final MutableInt filesCount = new MutableInt(0);
        final MutableInt dirsCount = new MutableInt(0);

        try (Stream<Path> firstLevelStream = Files.list(serverDir)) {
            firstLevelStream
                    .filter(path -> path.getFileName().toString().length() == 1)
                    .forEach(firstLevelPath -> processFirstLevelPath(firstLevelPath, filesCount, dirsCount));
        } catch (IOException e) {
            throw new DotRuntimeException(e);
        }

        return Tuple.of(filesCount.getValue(), dirsCount.getValue());
    }

    private void processFirstLevelPath(Path firstLevelPath, MutableInt filesCount, MutableInt dirsCount) {
        try (Stream<Path> secondLevelStream = Files.list(firstLevelPath)) {
            secondLevelStream
                    .filter(path -> path.getFileName().toString().length() == 1)
                    .forEach(secondLevelPath -> processSecondLevelPath(secondLevelPath, filesCount, dirsCount));
        } catch (IOException e) {
            throw new DotRuntimeException(e);
        }
    }

    private void processSecondLevelPath(Path secondLevelPath, MutableInt filesCount, MutableInt dirsCount) {
        try (Stream<Path> pathsStream = Files.list(secondLevelPath)) {
            pathsStream
                    .filter(path -> UUIDUtil.isUUID(path.getFileName().toString()))
                    .forEach(current -> processCurrentPath(current, filesCount, dirsCount));
        } catch (IOException e) {
            throw new DotRuntimeException(e);
        }
    }

    private void processCurrentPath(Path current, MutableInt filesCount, MutableInt dirsCount) {
        try (Stream<Path> metadataStream = Files.list(current)) {
            Set<Path> metadataFiles = metadataStream
                    .filter(path -> !Files.isDirectory(path))
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith("-metadata.json"))
                    .collect(Collectors.toSet());
            deleteMetadataFiles(metadataFiles, filesCount);
            processMetaDataDirectory(current, dirsCount);
        } catch (IOException e) {
            throw new DotRuntimeException(e);
        }
    }

    private void deleteMetadataFiles(Set<Path> metadataFiles, MutableInt filesCount) {
        for (Path metadataFile : metadataFiles) {
            Logger.debug(Task210321RemoveOldMetadataFiles.class, "Removing metadata file: " + metadataFile.toString());
            if (metadataFile.toFile().delete()) {
                filesCount.increment();
            }
        }
    }

    private void processMetaDataDirectory(Path current, MutableInt dirsCount) {
        try (Stream<Path> metaDataDirStream = Files.list(current)) {
            Optional<Path> metaDataDir = metaDataDirStream
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().equals("metaData"))
                    .findFirst();
            metaDataDir.ifPresent(metaDir -> deleteMetaDataDirectory(metaDir, dirsCount));
        } catch (IOException e) {
            throw new DotRuntimeException(e);
        }
    }

    private void deleteMetaDataDirectory(Path metaDataDir, MutableInt dirsCount) {
        try (Stream<Path> walk = Files.walk(metaDataDir)) {
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(file -> {
                        if (file.delete()) {
                            if (file.getName().equals("metaData")) {
                                dirsCount.increment();
                                Logger.debug(Task210321RemoveOldMetadataFiles.class, "Removed metaData dir: " + file);
                            }
                        }
                    });
        } catch (IOException e) {
            throw new DotRuntimeException(e);
        }
    }

    public Future<Tuple2<Integer, Integer>> getFuture() {
        return future;
    }

}
