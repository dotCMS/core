package com.dotmarketing.fixtask.tasks;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.quartz.JobExecutionException;
import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.beans.FixAudit;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.fixtask.FixTask;
import com.dotmarketing.portlets.cmsmaintenance.ajax.FixAssetsProcessStatus;
import com.dotmarketing.util.Logger;
import com.google.common.collect.Lists;
import com.liferay.util.FileUtil;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import io.vavr.Tuple2;

public class FixTask00100DeleteUnlinkedContentletAssets implements FixTask {

    final String rootPath;
    final Pattern assetMatchPattern;
    final FileFilter inodeFolderFilter;

    public FixTask00100DeleteUnlinkedContentletAssets(final String rootPath) {
        this.rootPath = rootPath;
        this.assetMatchPattern = Pattern.compile(ASSET_UUID_REGEX);
        this.inodeFolderFilter = new FileFilter() {
            final File assetFolder = new File(rootPath);

            @Override
            public boolean accept(File pathname) {
                if (!pathname.isDirectory()) {
                    return false;
                }
                final String folderName = pathname.getAbsolutePath().replace(assetFolder.getAbsolutePath(), "");
                return assetMatchPattern.matcher(folderName).find();
            }

        };

    }

    public FixTask00100DeleteUnlinkedContentletAssets() {
        this(APILocator.getFileAssetAPI().getRealAssetsRootPath());
    }


    private static final String ASSET_UUID_REGEX =
                    "\\/[A-Fa-f0-9]{1}\\/[A-Fa-f0-9]{1}\\/([A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12})$";


    /**
     * This returns a list of inode folders that do not have a coorsponding entry in the inode table
     * 
     * @param inodesToTest
     * @return
     * @throws DotDataException
     */
    private final List<String> filterForDeadInodes(final List<String> testTheseInode) throws DotDataException {

        final String sql = "select inode.inode as test from inode, contentlet, identifier "
                        + "where identifier.id = contentlet.identifier and inode.inode = contentlet.inode and "
                        + "inode.type='contentlet' and inode.inode in ('" + String.join("','", testTheseInode) + "')";

        final List<String> existingInodes = new DotConnect().setSQL(sql).loadObjectResults().stream()
                        .map(r -> String.valueOf(r.get("test"))).collect(Collectors.toList());



        List<String> toReturn = new ArrayList<>(testTheseInode);
        toReturn.removeAll(existingInodes);

        return toReturn;
    }


    public Tuple2<Integer,List<String>> run() throws JobExecutionException {

        final File assetFolder = new File(this.rootPath);


        List<File> folders = FileUtil.listFilesRecursively(assetFolder, new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });


        folders.removeIf(f -> !inodeFolderFilter.accept(f));

        Collections.shuffle(folders);
        Logger.info(this.getClass(), "Found " + folders.size() + " contentlet folders under " + assetFolder);

        final List<String> inodesToTest = new ArrayList<>();

        for (final File folder : folders) {
            Matcher matcher = assetMatchPattern.matcher(folder.getPath());
            matcher.find();
            inodesToTest.add(matcher.group(1));
        }
        folders = null;
        List<String> deletedInodes = new ArrayList<>();
        for (final List<String> partition : Lists.partition(inodesToTest, 100)) {
            final List<String> deadInodes = Sneaky.sneak(() -> filterForDeadInodes(partition));
            deletedInodes.addAll(deadInodes);
            deleteDeadInodeFolders(deadInodes);
            Sneaky.sneaked(() -> Thread.sleep(50));

        }

        return new Tuple2<Integer, List<String>>(inodesToTest.size(), deletedInodes);

    }

    /**
     * this takes a list of inodes and deletes them and their folders
     * 
     * @param deadOnes
     */
    private void deleteDeadInodeFolders(List<String> deadOnes) {
        for (String id : deadOnes) {
            File deleteMe = new File(APILocator.getFileAssetAPI().getRealAssetPath(id)).getParentFile();
            Logger.info(this.getClass(), "  - deleting unlinked contentlet " + deleteMe);
            FileUtil.deltree(deleteMe);
        }
    }

    @Override
    public boolean shouldRun() {
        return true;
    }

    @CloseDBIfOpened
    @Override
    public List<Map<String, Object>> executeFix() throws DotDataException, DotRuntimeException {


        Logger.info(this.getClass(), "-----------------------------");
        Logger.info(this.getClass(), "Unlinked contentlet asset scan starting on : " + rootPath);
        Logger.info(this.getClass(), "-----------------------------");
        final List<Map<String, Object>> returnValue = new ArrayList<>();

        if (!FixAssetsProcessStatus.getRunning()) {

            try {
                FixAssetsProcessStatus.startProgress();
                FixAssetsProcessStatus.setDescription("task 100: DeleteUnlinkedContentletAssets");

                Tuple2<Integer, List<String>> inodesAffected = run();

                FixAssetsProcessStatus.setTotal(inodesAffected._1);
                FixAssetsProcessStatus.setErrorsFixed(inodesAffected._2.size());

                saveFixAuditRecord(inodesAffected._2.size());
                returnValue.add(FixAssetsProcessStatus.getFixAssetsMap());
                Logger.debug(this.getClass(), "Ending DeleteUnlinkedContentletAssets");

            } catch (DotHibernateException e1) {
                Logger.debug(this.getClass(), "An issue happened during execution of DeleteUnlinkedContentletAssets task", e1);
                FixAssetsProcessStatus.setActual(-1);
            } catch (Exception e2) {
                Logger.debug(this.getClass(),
                                "There was an unexpected problem during execution of DeleteUnlinkedContentletAssets task", e2);
                FixAssetsProcessStatus.setActual(-1);
            } finally {
                FixAssetsProcessStatus.stopProgress();
            }
        }
        return returnValue;
    }



    @Override
    public List<Map<String, String>> getModifiedData() {
        // TODO Auto-generated method stub
        return null;
    }

    private void saveFixAuditRecord(final int total) throws DotHibernateException {
        final FixAudit auditObj = new FixAudit();
        auditObj.setTableName("filesystem");
        auditObj.setDatetime(new Date());
        auditObj.setRecordsAltered(total);
        auditObj.setAction("task 100: DeleteUnlinkedContentletAssets");
        HibernateUtil.save(auditObj);
    }

}
