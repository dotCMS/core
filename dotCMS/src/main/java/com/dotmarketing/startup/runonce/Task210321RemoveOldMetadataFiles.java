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
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
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
        try {
             /*
               +serverDir
               +
               +- a  (First level 1 char length )
                  +
                  +- 5 (First level 1 char length )
                     +
                     +- a568fd-dff45fg- (identifier like folder name)
             */

            final Set<Path> firstLevelPaths = listDirectories(serverDir).stream()
                    .filter(path -> path.getFileName().toString().length() == 1).collect(Collectors.toSet());

            for (final Path dir : firstLevelPaths) {
                final Set<Path> secondLevelPaths = listDirectories(dir).stream()
                        .filter(path -> path.getFileName().toString().length() == 1).collect(Collectors.toSet());

                for (final Path secondLevelPath : secondLevelPaths) {
                    final Set<Path> paths = listDirectories(secondLevelPath).stream()
                            .filter(path -> UUIDUtil.isUUID(path.getFileName().toString()))
                            .collect(Collectors.toSet());
                    for (final Path current:paths) {

                        Logger.info(Task210321RemoveOldMetadataFiles.class,"current dir: "+current);

                        final Set<Path> metadataFiles = Files.list(current)
                        .filter(path -> !Files.isDirectory(path))
                        .filter(path -> path.getFileName().toString().toLowerCase().endsWith("-metadata.json")).collect(Collectors.toSet());

                        for (final Path metadataFile : metadataFiles) {
                            Logger.info(Task210321RemoveOldMetadataFiles.class,"Removing metadata file: " + metadataFile.toString());
                            if (metadataFile.toFile().delete()) {
                                filesCount.increment();
                            }
                        }

                        final Optional<Path> metaDataDir = Files.list(current)
                                .filter(path -> Files.isDirectory(path))
                                .filter(path -> path.toString().equals("metaData"))
                                .findFirst();
                        metaDataDir.ifPresent(path -> {
                            if(path.toFile().delete()){
                                dirsCount.increment();
                                Logger.info(Task210321RemoveOldMetadataFiles.class,"Removed metaData dir: " + path);
                            }
                        });
                    }
                }
            }

        } catch (IOException e) {
            throw new DotRuntimeException(e);
        }

        return Tuple.of(filesCount.getValue(),dirsCount.getValue());

    }

    private Set<Path> listDirectories(final Path dir) throws IOException {
        final Set<Path> fileList = new HashSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (final Path path : stream) {
                if (Files.isDirectory(path)) {
                    fileList.add(path);
                }
            }
        }
        return fileList;
    }

    public Future<Tuple2<Integer, Integer>> getFuture() {
        return future;
    }

}
