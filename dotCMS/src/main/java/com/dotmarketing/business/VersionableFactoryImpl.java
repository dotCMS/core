package com.dotmarketing.business;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.dotcms.repackage.org.apache.commons.beanutils.BeanUtils;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 *
 * @author will
 *
 */
public class VersionableFactoryImpl extends VersionableFactory {

	IdentifierAPI iapi = null;
	IdentifierCache icache = null;

	public VersionableFactoryImpl() {
		iapi = APILocator.getIdentifierAPI();
		icache = CacheLocator.getIdentifierCache();
	}
	
	
	private final String fourOhFour="NOTFOUND";

	@Override
	protected Versionable findWorkingVersion(String id) throws DotDataException, DotStateException {

		Identifier identifier = iapi.find(id);
		if(identifier==null || !InodeUtils.isSet(identifier.getInode())){
			throw new DotDataException("identifier:" + id +" not found");
		}
		if(identifier.getAssetType().equals("contentlet"))
		    throw new DotDataException("Contentlets could have working versions for each language");

		VersionInfo vinfo = getVersionInfo(identifier.getId());

		Class clazz = InodeUtils.getClassByDBType(identifier.getAssetType());
		
		Versionable ver = null;
		User user = APILocator.getUserAPI().getSystemUser();
		String workingInode = vinfo.getWorkingInode();
		
		try{
			if(HTMLPage.class.equals(clazz)){
				ver= APILocator.getHTMLPageAPI().loadLivePageById(id, user, true);
			}
			else if(Container.class.equals(clazz)){
				ver= APILocator.getContainerAPI().getWorkingContainerById(id, user, true);
			}
			else if(Template.class.equals(clazz)){
				ver= APILocator.getTemplateAPI().find(workingInode, user, true);
			}
			else if(File.class.equals(clazz)){
				ver= APILocator.getFileAPI().find(workingInode, user, true);
			}
			else if(Contentlet.class.equals(clazz)){
				ver= APILocator.getContentletAPI().find(workingInode, user, true);
			}
			// ignore Links, WorkflowMessages and Inode
			} catch(Exception e){
				Logger.error(this.getClass(), "Error finding the working version of " + clazz + ", with Identifier: " + id);
			}
		
		
		if(ver == null){
			Logger.error(this.getClass(), "Versionable NULL when finding working version " + clazz.getName() + " Trying old method.");
			HibernateUtil dh = new HibernateUtil(clazz);
			dh.setQuery("from inode in class " + clazz.getName() + " where inode.inode=?");
			dh.setParam(vinfo.getWorkingInode());
			Logger.debug(this.getClass(), "findWorkingVersion query: " + dh.getQuery());
			ver =(Versionable) dh.load();
		}

		if(ver.getVersionId() ==null){
			throw new DotStateException("Invalid working version for identifier : " +id + " / working inode : " + vinfo.getWorkingInode());
		}
		return ver;


	}

	@Override
	protected Versionable findLiveVersion(String id) throws DotDataException, DotStateException {
		Identifier identifier = iapi.find(id);
		if(identifier==null || !InodeUtils.isSet(identifier.getInode())){
			throw new DotDataException("identifier:" + id +" not found");
		}
		if(identifier.getAssetType().equals("contentlet"))
            throw new DotDataException("Contentlets could have live versions for each language");

		VersionInfo vinfo = getVersionInfo(identifier.getId());
		
		Versionable ver = null;
		User user = APILocator.getUserAPI().getSystemUser();
		
		Class clazz = InodeUtils.getClassByDBType(identifier.getAssetType());
		if(UtilMethods.isSet(vinfo)) {
			if(UtilMethods.isSet(vinfo.getLiveInode())){
				String liveInode = vinfo.getLiveInode();
				try{
				if(HTMLPage.class.equals(clazz)){
					ver= APILocator.getHTMLPageAPI().loadLivePageById(id, user, true);
				}
				else if(Container.class.equals(clazz)){
					ver= APILocator.getContainerAPI().getLiveContainerById(id, user, true);
				}
				else if(Template.class.equals(clazz)){
					ver= APILocator.getTemplateAPI().find(liveInode, user, true);
				}
				else if(File.class.equals(clazz)){
					ver= APILocator.getFileAPI().find(liveInode, user, true);
				}
				else if(Contentlet.class.equals(clazz)){
					ver= APILocator.getContentletAPI().find(liveInode, user, true);
				}
				// ignore Links, WorkflowMessages and Inode
				} catch(Exception e){
					Logger.error(this.getClass(), "Error finding the live version of " + clazz + ", with Identifier: " + id);
				}

				if(ver==null){
					Logger.error(this.getClass(), "Versionable NULL when finding live version " + clazz.getName() + " Trying old method.");
					HibernateUtil dh = new HibernateUtil(clazz);
					dh.setQuery("from inode in class " + clazz.getName() + " where inode.inode=?");
					dh.setParam(vinfo.getLiveInode());
					Logger.debug(this.getClass(), "findLiveVersion query: " + dh.getQuery());
					ver= (Versionable) dh.load();
				}
			}
		}
		return ver;
	}
	
	@Override
	protected Versionable findDeletedVersion(String id) throws DotDataException, DotStateException {
		Identifier identifier = iapi.find(id);
		if(identifier ==null){
			throw new DotDataException("identifier:" + id +" not found");
		}
		Class clazz = InodeUtils.getClassByDBType(identifier.getAssetType());
		HibernateUtil dh = new HibernateUtil(clazz);
		dh.setQuery("from inode in class " + clazz.getName() + " where identifier = ? and inode.type='" + identifier.getAssetType() + "' and deleted="
				+ DbConnectionFactory.getDBTrue());
		dh.setParam(id);
		Logger.debug(this.getClass(), "findDeletedVersion query: " + dh.getQuery());
		return (Versionable) dh.load();
	}
	@Override
	protected List<Versionable> findAllVersions(String id) throws DotDataException, DotStateException {
		Identifier identifier = iapi.find(id);
		if(identifier ==null){
			throw new DotDataException("identifier:" + id +" not found");
		}
		Class clazz = InodeUtils.getClassByDBType(identifier.getAssetType());
		if(clazz.equals(Inode.class))
		    return new ArrayList<Versionable>(1);
		HibernateUtil dh = new HibernateUtil(clazz);
		dh.setQuery("from inode in class " + clazz.getName() + " where inode.identifier = ? and inode.type='" + identifier.getAssetType() + "' order by mod_date desc");
		dh.setParam(id);
		Logger.debug(this.getClass(), "findAllVersions query: " + dh.getQuery());
		return (List<Versionable>) dh.list();
	}

    @Override
    protected VersionInfo getVersionInfo(String identifier) throws DotDataException,
            DotStateException {
        VersionInfo vi = icache.getVersionInfo(identifier);
        if(vi==null || vi.getWorkingInode().equals(fourOhFour)) {
            Identifier ident = APILocator.getIdentifierAPI().find(identifier);
            if(ident==null || !UtilMethods.isSet(ident.getId()))
                return null;
            Class clazz = UtilMethods.getVersionInfoType(ident.getAssetType());
            HibernateUtil dh = new HibernateUtil(clazz);
            dh.setQuery("from "+clazz.getName()+" where identifier=?");
            dh.setParam(identifier);
            Logger.debug(this.getClass(), "getVersionInfo query: "+dh.getQuery());
            vi=(VersionInfo)dh.load();
            if(!UtilMethods.isSet(vi.getIdentifier())) {
            	vi.setIdentifier(identifier);
            	vi.setWorkingInode("NOTFOUND");
            }
            icache.addVersionInfoToCache(vi);
        }
        if(vi.getWorkingInode().equals("NOTFOUND"))
            return null;
        else
        	return vi;
    }

	/**
	 * reload versionInfo from db (JIRA-7203)
	 * @param info
	 * @return
	 * @throws DotDataException
	 * @throws DotStateException
	 */
    @Override
    protected VersionInfo findVersionInfoFromDb(Identifier identifer) throws DotDataException,
            DotStateException {
            Class clazz = UtilMethods.getVersionInfoType(identifer.getAssetType());
            VersionInfo vi= null;
            if(clazz != null) {
	            HibernateUtil dh = new HibernateUtil(clazz);
	            dh.setQuery("from "+clazz.getName()+" where identifier=?");
	            dh.setParam(identifer.getId());
	            Logger.debug(this.getClass(), "getVersionInfo query: "+dh.getQuery());
	            vi=(VersionInfo)dh.load();
            }
            if(vi ==null || !UtilMethods.isSet(vi.getIdentifier())) {
            	try {
                    vi = (VersionInfo) clazz.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            return vi;

    }



    @Override
    protected void saveVersionInfo(VersionInfo info, boolean updateVersionTS) throws DotDataException, DotStateException {

    	//reload versionInfo from db (JIRA-7203)
        Identifier ident = APILocator.getIdentifierAPI().find(info.getIdentifier());
        VersionInfo vi=(VersionInfo) findVersionInfoFromDb(ident);
        boolean isNew = vi==null || !InodeUtils.isSet(vi.getIdentifier());
        try {
			BeanUtils.copyProperties(vi, info);
		} catch (Exception e) {
			throw new DotDataException(e.getMessage());
		}

        if(updateVersionTS) {
        	vi.setVersionTs(new Date());
        }

        if(isNew) {
            HibernateUtil.save(vi);
        }
        else {
            HibernateUtil.saveOrUpdate(vi);
        }
        HibernateUtil.flush();
        icache.removeVersionInfoFromCache(vi.getIdentifier());

    }

    @Override
    protected ContentletVersionInfo getContentletVersionInfo(String identifier, long lang) throws DotDataException, DotStateException {
        ContentletVersionInfo contv = icache.getContentVersionInfo(identifier, lang);
        if(contv!=null && fourOhFour.equals(contv.getWorkingInode())) {
        	return null;
        }else if(contv!=null ){
        	return contv;
        }
        
    	contv = findContentletVersionInfoInDB(identifier, lang);
        if(contv!=null && UtilMethods.isSet(contv.getIdentifier())){
            icache.addContentletVersionInfoToCache(contv);
        }else{
        	contv = new ContentletVersionInfo();
        	contv.setIdentifier(identifier);
        	contv.setLang(lang);
        	contv.setWorkingInode(fourOhFour);
        	icache.addContentletVersionInfoToCache(contv);
			return null;
        }
        
        return contv;
    }

    @Override
    protected ContentletVersionInfo findContentletVersionInfoInDB(String identifier, long lang)throws DotDataException, DotStateException {
    	ContentletVersionInfo contv = null;
    	 HibernateUtil dh = new HibernateUtil(ContentletVersionInfo.class);
         dh.setQuery("from "+ContentletVersionInfo.class.getName()+" where identifier=? and lang=?");
         dh.setParam(identifier);
         dh.setParam(lang);
         Logger.debug(this.getClass(), "getContentletVersionInfo query: "+dh.getQuery());
         contv = (ContentletVersionInfo)dh.load();
         return contv;
    }

    @Override
    protected void saveContentletVersionInfo(ContentletVersionInfo cvInfo, boolean updateVersionTS) throws DotDataException, DotStateException {
    	Identifier ident = APILocator.getIdentifierAPI().find(cvInfo.getIdentifier());
    	ContentletVersionInfo vi= null;
    	if(ident!=null && InodeUtils.isSet(ident.getId())){
    		vi= findContentletVersionInfoInDB(ident.getId(), cvInfo.getLang());
    	}
    	boolean isNew = vi==null || !InodeUtils.isSet(vi.getIdentifier());
        try {
			BeanUtils.copyProperties(vi, cvInfo);
		} catch (Exception e) {
			throw new DotDataException(e.getMessage());
		}
        if(updateVersionTS){
        	vi.setVersionTs(new Date());
        }
    	if(isNew) {
            HibernateUtil.save(vi);
        }
        else {
            HibernateUtil.saveOrUpdate(vi);
        }
    	HibernateUtil.saveOrUpdate(vi);
        icache.removeContentletVersionInfoToCache(cvInfo.getIdentifier(),cvInfo.getLang());

    }

    @Override
    protected ContentletVersionInfo createContentletVersionInfo(Identifier identifier, long lang, String workingInode) throws DotStateException, DotDataException {
        ContentletVersionInfo cVer=new ContentletVersionInfo();
        cVer.setDeleted(false);
        cVer.setLockedBy(null);
        cVer.setLockedOn(new Date());
        cVer.setIdentifier(identifier.getId());
        cVer.setLang(lang);
        cVer.setWorkingInode(workingInode);
        cVer.setVersionTs(new Date());

        HibernateUtil.save(cVer);
        return cVer;
    }

    @Override
    protected VersionInfo createVersionInfo(Identifier identifier, String workingInode) throws DotStateException, DotDataException {
        Class clazz=UtilMethods.getVersionInfoType(identifier.getAssetType());
        VersionInfo ver;
        try {
            ver = (VersionInfo)clazz.newInstance();
        } catch (Exception e) {
            throw new DotStateException("this shouln't happend");
        }
        ver.setIdentifier(identifier.getId());
        ver.setDeleted(false);
        ver.setLockedBy(null);
        ver.setLockedOn(new Date());
        ver.setWorkingInode(workingInode);
        ver.setVersionTs(new Date());
        HibernateUtil.save(ver);
        return ver;
    }

	@Override
	protected void deleteVersionInfo(String id) throws DotDataException {
		icache.removeVersionInfoFromCache(id);
	    VersionInfo info = getVersionInfo(id);
		if(info!=null && UtilMethods.isSet(info.getIdentifier())) {
			String ident = info.getIdentifier();
			HibernateUtil.delete(info);
			icache.removeFromCacheByIdentifier(ident);
		}

	}

	@Override
	protected void deleteContentletVersionInfo(String id, long lang) throws DotDataException {
		HibernateUtil dh = new HibernateUtil(ContentletVersionInfo.class);
        dh.setQuery("from "+ContentletVersionInfo.class.getName()+" where identifier=? and lang=?");
        dh.setParam(id);
        dh.setParam(lang);
        Logger.debug(this.getClass(), "getContentletVersionInfo query: "+dh.getQuery());
        ContentletVersionInfo contv = (ContentletVersionInfo)dh.load();

        if(UtilMethods.isSet(contv.getIdentifier())) {
        	HibernateUtil.delete(contv);
        	icache.removeContentletVersionInfoToCache(id, lang);
        }
	}
	
	

	
	
}
