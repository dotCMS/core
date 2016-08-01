package com.dotmarketing.util;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import com.liferay.util.FileUtil;

public class TrashUtils {

	private final String trashFolder;
	public TrashUtils(final String trashFolder){
		this.trashFolder=trashFolder;
	}
	public TrashUtils(){
		this(ConfigUtils.getDynamicContentPath() + File.separator+ "trash");
	}
	
	public File getTrashFolder () {
		return new File(trashFolder);
	}
	
	public boolean moveFileToTrash(File[] filesToMove, String trashCategory) throws IOException {
		
		trashCategory = trashCategory == null?File.separator:File.separator + RegEX.replaceAll(trashCategory, "_", "[^A-Za-z0-9]");
		String trashSubFolder = getTrashFolder().getCanonicalPath() + trashCategory + File.separator + DateUtil.formatDate(new Date(), "yyyyMMddHHmmssSSS");
		File f = new File(trashSubFolder);
		if(!f.exists() && !f.mkdirs()) {
			Logger.error(TrashUtils.class, "Unable to create trash folder " + trashSubFolder);
			return false;
		}
		for(File file : filesToMove) {
			File newDest = new File(f, file.getName());
			FileUtil.move(file, newDest);
		}
		return true;
	}
	
	public boolean moveFileToTrash(String[] filePaths, String trashCategory) throws IOException {
		File[]filesToMove = new File[filePaths.length];
		int i = 0;
		for(String filePath : filePaths) {
			File fileToMove = new File(filePath);
			filesToMove[i++] = fileToMove;
		}
		return moveFileToTrash(filesToMove, trashCategory);
	}

	public boolean moveFileToTrash(File fileToMove, String trashCategory) throws IOException {
		return moveFileToTrash(new File[] { fileToMove }, trashCategory);
	}
	
	public boolean moveFileToTrash(String filePath, String trashCategory) throws IOException {
		File fileToMove = new File(filePath);
		return moveFileToTrash(fileToMove, trashCategory);
	}
	
	public boolean emptyTrash(){
	
		File trashFolder = getTrashFolder();
		if(!trashFolder.exists()){
			Logger.debug(TrashUtils.class,"Trash folder "+ trashFolder.getPath() + " not found exiting job");
			return false;
		}
		File[] files = trashFolder.listFiles();
		for (File file : files) {
			if(file.isDirectory()){
				FileUtil.deltree(file);
			}else{
				file.delete();
			}
		}
		return true;
	}
	

	/**
	 * Will delete folders from the configured bundle directory
	 * older than what is configed here : trash.delete.bundles.older.than.milliseconds  (2 days by default)
	 * @return
	 */
	public boolean deleteOldBundles(){
		
		long olderThan = System.currentTimeMillis() - Config.getIntProperty("trash.delete.bundles.older.than.milliseconds", 1000*60*60*24*2); // two days
		File bundleFolder = new File(ConfigUtils.getDynamicContentPath() );
		if(bundleFolder.exists()){
			for(File file : bundleFolder.listFiles()){
				if(file.lastModified()< olderThan){
					FileUtil.deltree(file);
				}
			}
		}
		return true;

	}
}
