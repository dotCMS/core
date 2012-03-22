package com.dotmarketing.util;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class TrashUtils {

	private static final String TRASH_FOLDER = "dottrash";
	
	public static File getTrashFolder () {
		return new File(ConfigUtils.getDynamicContentPath() + File.separator+TRASH_FOLDER);
	}
	
	public static boolean moveFileToThrash(File[] filesToMove, String trashCategory) throws IOException {
		
		trashCategory = trashCategory == null?File.separator:File.separator + RegEX.replaceAll(trashCategory, "_", "[^A-Za-z0-9]");
		String trashSubFolder = getTrashFolder().getCanonicalPath() + trashCategory + File.separator + DateUtil.formatDate(new Date(), "yyyyMMddHHmmssSSS");
		File f = new File(trashSubFolder);
		if(!f.exists() && !f.mkdirs()) {
			Logger.error(TrashUtils.class, "Unable to create trash folder " + trashSubFolder);
			return false;
		}
		for(File file : filesToMove) {
			File newDest = new File(f, file.getName());
			if (file.exists()) {
				if(!file.renameTo(newDest)) {
					return false;
				}
			}
		}
		return true;
	}
	
	public static boolean moveFileToThrash(String[] filePaths, String trashCategory) throws IOException {
		File[]filesToMove = new File[filePaths.length];
		int i = 0;
		for(String filePath : filePaths) {
			File fileToMove = new File(filePath);
			filesToMove[i++] = fileToMove;
		}
		return moveFileToThrash(filesToMove, trashCategory);
	}

	public static boolean moveFileToThrash(File fileToMove, String trashCategory) throws IOException {
		return moveFileToThrash(new File[] { fileToMove }, trashCategory);
	}
	
	public static boolean moveFileToThrash(String filePath, String trashCategory) throws IOException {
		File fileToMove = new File(filePath);
		return moveFileToThrash(fileToMove, trashCategory);
	}
}
