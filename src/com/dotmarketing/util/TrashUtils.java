package com.dotmarketing.util;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import com.liferay.util.FileUtil;

public class TrashUtils {

	private static final String TRASH_FOLDER = "trash";
	
	public static File getTrashFolder () {
		return new File(ConfigUtils.getDynamicContentPath() + File.separator+TRASH_FOLDER);
	}
	
	public static boolean moveFileToTrash(File[] filesToMove, String trashCategory) throws IOException {
		
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
	
	public static boolean moveFileToTrash(String[] filePaths, String trashCategory) throws IOException {
		File[]filesToMove = new File[filePaths.length];
		int i = 0;
		for(String filePath : filePaths) {
			File fileToMove = new File(filePath);
			filesToMove[i++] = fileToMove;
		}
		return moveFileToTrash(filesToMove, trashCategory);
	}

	public static boolean moveFileToTrash(File fileToMove, String trashCategory) throws IOException {
		return moveFileToTrash(new File[] { fileToMove }, trashCategory);
	}
	
	public static boolean moveFileToTrash(String filePath, String trashCategory) throws IOException {
		File fileToMove = new File(filePath);
		return moveFileToTrash(fileToMove, trashCategory);
	}
	
	public static boolean emptyTrash(){
	
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
}
