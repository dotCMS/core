package com.dotmarketing.viewtools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class PictureGalleryWebAPI implements ViewTool {

    private FolderAPI fapi = APILocator.getFolderAPI();

    public void init(Object obj) {


    }

	//Pictures Gallery Methods
	public List<Hashtable<String, Object>> getPictureGalleryIndexImages (String indexFolderPath, Host host) throws DotDataException, DotSecurityException {
	    return getPictureGalleryIndexImages (indexFolderPath, host.getIdentifier());
	}
    @Deprecated
	public List<Hashtable<String, Object>> getPictureGalleryIndexImages (String indexFolderPath, long hostId) throws DotDataException, DotSecurityException {
		return getPictureGalleryIndexImages (indexFolderPath, String.valueOf(hostId));
	}

	public List<Hashtable<String, Object>> getPictureGalleryIndexImages (String indexFolderPath, String hostId) throws DotDataException, DotSecurityException {
		ArrayList<Hashtable<String, Object>> indexImages = new ArrayList<Hashtable<String, Object>> ();
		indexFolderPath = indexFolderPath.trim().endsWith("/")?indexFolderPath.trim():indexFolderPath.trim() + "/";
		Folder folder = fapi.findFolderByPath(indexFolderPath, hostId,APILocator.getUserAPI().getSystemUser(), false);
		Host host = APILocator.getHostAPI().find(hostId, APILocator.getUserAPI().getSystemUser(), false);
		List<Folder> subFolders = fapi.findSubFolders(folder, APILocator.getUserAPI().getSystemUser(), false);
		Iterator<Folder> sFoldersIterator = subFolders.iterator();
		while (sFoldersIterator.hasNext()) {
			Folder subFolder = (Folder) sFoldersIterator.next();
			String subFolderPath = APILocator.getIdentifierAPI().find(subFolder).getPath();
            List<IFileAsset> imagesList = getPictureGalleryFolderImages(subFolderPath, host);
            if (imagesList.size() > 0) {
            	IFileAsset indexImg = null;
                for (IFileAsset img : imagesList) {
                	String ext = img.getExtension();
        			if(ext.toLowerCase().endsWith(".jpg") || ext.toLowerCase().endsWith(".gif")) {
        				if(indexImg == null)
        					indexImg = img;
	                    if (img.getFileName().toLowerCase().equals("index.jpg") || img.getFileName().toLowerCase().equals("index.gif")) {
	                        indexImg = img;
	                        break;
	                    }
        			}
                }
                if(indexImg != null) {
	                Hashtable<String, Object> folderProperties = new Hashtable<String, Object> ();
	                folderProperties.put("totalImages", imagesList.size());
	                folderProperties.put("indexImageInode", indexImg.getInode());
	                folderProperties.put("indexImageExtension", indexImg.getExtension());
	                folderProperties.put("galleryName", subFolder.getTitle());
	                folderProperties.put("galleryDate", indexImg.getFriendlyName());
	                folderProperties.put("galleryDescription", indexImg.getTitle());
	                folderProperties.put("galleryPath", subFolderPath);
	                indexImages.add(folderProperties);
                }
            }
		}
		return indexImages;
	}


	public List<IFileAsset> getPictureGalleryFolderImages (String folderPath, Host host) {
	    return getPictureGalleryFolderImages (folderPath, host.getIdentifier());
	}
    @Deprecated
	public List<IFileAsset> getPictureGalleryFolderImages (String folderPath, long hostId) {
    	return getPictureGalleryFolderImages (folderPath, String.valueOf(hostId));
	}
	public List<IFileAsset> getPictureGalleryFolderImages (String folderPath, String hostId) {
		folderPath = (folderPath == null)?"":folderPath;
		folderPath = folderPath.trim().endsWith("/")?folderPath.trim():folderPath.trim() + "/";
		Folder folder;
		try {
			folder = fapi.findFolderByPath(folderPath, hostId,APILocator.getUserAPI().getSystemUser(),false);
		} catch (Exception e) {
			Logger.error(this,e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(),e);
		}
		List<IFileAsset> filesList = new ArrayList<IFileAsset>();
		try {
			filesList.addAll(APILocator.getFolderAPI().getWorkingFiles(folder, APILocator.getUserAPI().getSystemUser(),false));
			filesList.addAll(APILocator.getFolderAPI().getLiveFiles(folder, APILocator.getUserAPI().getSystemUser(),false));
			filesList.addAll(APILocator.getFileAssetAPI().findFileAssetsByFolder(folder, APILocator.getUserAPI().getSystemUser(),false));
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
		}
		List<IFileAsset> imagesList = new ArrayList<IFileAsset> ();
		for(IFileAsset file : filesList) {
			String ext = file.getExtension();
			if(ext.toLowerCase().endsWith(".jpg") || ext.toLowerCase().endsWith(".gif"))
				imagesList.add(file);
		}
		return imagesList;
	}

	public List<IFileAsset> getShufflePictureGalleryFolderImages (String folderPath, Host host) {
		List<IFileAsset> result = getPictureGalleryFolderImages (folderPath, host.getIdentifier());

		java.util.Collections.shuffle(result, new java.util.Random(new java.util.GregorianCalendar().getTimeInMillis()));

	    return result;
	}
    @Deprecated
	public String getFirstSubFolder(String fromPath, long hostId) throws DotStateException, DotDataException{
		return getFirstSubFolder(fromPath, String.valueOf(hostId));

	}

	public String getFirstSubFolder(String fromPath, String hostId) throws DotStateException, DotDataException{

		fromPath = fromPath.trim().endsWith("/")?fromPath.trim():fromPath.trim() + "/";
		List<Folder> subFolders;
		try {
			Folder folder = fapi.findFolderByPath(fromPath, hostId,APILocator.getUserAPI().getSystemUser(),false);
			subFolders = fapi.findSubFolders(folder, APILocator.getUserAPI().getSystemUser(),false);
		} catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
			throw new DotRuntimeException(e.getMessage(),e);
		}
		Folder subFolder = (Folder) subFolders.get(0);
		return	APILocator.getIdentifierAPI().find(subFolder).getPath();

	}

	@SuppressWarnings("unchecked")
	public List getPhotoGalleryFolderImages(String folderPath, Host host) {
		return getPhotoGalleryFolderImages(folderPath, host.getIdentifier());
	}
    @SuppressWarnings("unchecked")
	@Deprecated
	public List getPhotoGalleryFolderImages(String folderPath, long hostId) {
		return getPhotoGalleryFolderImages(folderPath, String.valueOf(hostId));
	}
	@SuppressWarnings("unchecked")
	public List getPhotoGalleryFolderImages(String folderPath, String hostId) {
		folderPath = (folderPath == null) ? "" : folderPath;
		folderPath = folderPath.trim().endsWith("/") ? folderPath.trim() : folderPath.trim() + "/";
		Folder folder;
		try {
			folder = fapi.findFolderByPath(folderPath, hostId,APILocator.getUserAPI().getSystemUser(),false);
		} catch (Exception e) {
			Logger.error(this, e.getMessage());
			throw new DotRuntimeException(e.getMessage(),e);
		}
		List imagesList = new ArrayList();
		try {
			imagesList.addAll(APILocator.getFolderAPI().getLiveFilesSortOrder(folder, null, true));
			imagesList.addAll(APILocator.getFileAssetAPI().findFileAssetsByFolder(folder, "", true, APILocator.getUserAPI().getSystemUser(),false));
		} catch (Exception e) {
		Logger.error(this,e.getMessage(), e);
		}
		return imagesList;
	}

	// Pictures Gallery Methods
    public List<Map<String, Object>> getPhotoGalleryIndexImages(String indexFolderPath, Host host, String indexPicture) {
        return getPhotoGalleryIndexImages(indexFolderPath, host.getIdentifier(), indexPicture);
    }

	public List<Map<String, Object>> getPhotoGalleryIndexImages(String indexFolderPath, Host host) {
        return getPhotoGalleryIndexImages(indexFolderPath, host.getIdentifier());
    }
	@Deprecated
	public List<Map<String, Object>> getPhotoGalleryIndexImages(String indexFolderPath, long hostId) {
    	return getPhotoGalleryIndexImages(indexFolderPath, String.valueOf(hostId), null);
    }
	public List<Map<String, Object>> getPhotoGalleryIndexImages(String indexFolderPath, String hostId) {
    	return getPhotoGalleryIndexImages(indexFolderPath, hostId, null);
    }

	@Deprecated
	public List<Map<String, Object>> getPhotoGalleryIndexImages(String indexFolderPath, long hostId, String indexPicture) {
        return getPhotoGalleryIndexImages(indexFolderPath, String.valueOf(hostId), indexPicture);
    }

	public List<Map<String, Object>> getPhotoGalleryIndexImages(String indexFolderPath, String hostId, String indexPicture) {

    	if(!UtilMethods.isSet(indexPicture)) {
    		indexPicture =  "index.jpg";
    	}

        List<Map<String, Object>> indexImages = new ArrayList<Map<String, Object>>();
        indexFolderPath = indexFolderPath.trim().endsWith("/") ? indexFolderPath.trim() : indexFolderPath.trim() + "/";
        List<Folder> subFolders = new ArrayList<Folder>();
		try {
			Folder folder = fapi.findFolderByPath(indexFolderPath, hostId,APILocator.getUserAPI().getSystemUser(),false);
			subFolders = fapi.findSubFolders(folder, APILocator.getUserAPI().getSystemUser(),false);
		} catch (Exception e) {
			Logger.error(this,e.getMessage());
			throw new DotRuntimeException(e.getMessage(),e);
		}
        Iterator<Folder> sFoldersIterator = subFolders.iterator();
        while (sFoldersIterator.hasNext()) {
            Folder subFolder = (Folder) sFoldersIterator.next();
            List<IFileAsset> imagesList = new ArrayList<IFileAsset>();
			try {
				imagesList.addAll(APILocator.getFolderAPI().getLiveFiles(subFolder, APILocator.getUserAPI().getSystemUser(),false));
				imagesList.addAll(APILocator.getFileAssetAPI().findFileAssetsByFolder(subFolder, "", true, APILocator.getUserAPI().getSystemUser(),false));
			} catch (Exception e1) {
				Logger.error(this, e1.getMessage(), e1);
			}
            if (imagesList.size() > 0) {
            	IFileAsset indexImg = imagesList.get(0);
                for (IFileAsset img : imagesList) {
                    if (img.getFileName().toLowerCase().equals(indexPicture.toLowerCase())) {
                        indexImg = img;
                        break;
                    }
                }
                Map<String, Object> folderProperties = new HashMap<String, Object>();
                folderProperties.put("totalImages", imagesList.size());
                folderProperties.put("indexImageInode", indexImg.getInode());
                folderProperties.put("indexImageExtension", indexImg.getExtension());
                folderProperties.put("galleryName", subFolder.getTitle());
                folderProperties.put("galleryDate", indexImg.getFriendlyName());
                folderProperties.put("galleryDescription", indexImg.getTitle());
                folderProperties.put("galleryLongDescription", indexImg.getFriendlyName());
                Identifier identifier = new Identifier();
				try {
					identifier = APILocator.getIdentifierAPI().find(subFolder);
				} catch (Exception e) {
					Logger.error(this, e.getMessage(),e);
					throw new DotRuntimeException(e.getMessage(),e);
				}
                folderProperties.put("galleryPath",identifier.getPath());
                indexImages.add(folderProperties);
            }
        }
        return indexImages;
    }

}