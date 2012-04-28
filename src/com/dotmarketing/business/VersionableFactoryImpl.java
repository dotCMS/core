package com.dotmarketing.business;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

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
		HibernateUtil dh = new HibernateUtil(clazz);
		dh.setQuery("from inode in class " + clazz.getName() + " where inode.inode=?");
		dh.setParam(vinfo.getWorkingInode());
		Logger.debug(this.getClass(), "findWorkingVersion query: " + dh.getQuery());

		Versionable v =(Versionable) dh.load();
		if(v.getVersionId() ==null){
			throw new DotStateException("Invalid working version for identifier : " +id + " / working inode : " + vinfo.getWorkingInode());
		}
		return v;


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

		Class clazz = InodeUtils.getClassByDBType(identifier.getAssetType());
		if(UtilMethods.isSet(vinfo.getLiveInode())) {
    		HibernateUtil dh = new HibernateUtil(clazz);
    		dh.setQuery("from inode in class " + clazz.getName() + " where inode.inode=?");
    		dh.setParam(vinfo.getLiveInode());
    		Logger.debug(this.getClass(), "findLiveVersion query: " + dh.getQuery());
    		return (Versionable) dh.load();
		}
		else {
		    // hey! there is no live version for this versionable
		    return null;
		}
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
        if(vi==null || vi.getWorkingInode().equals("NOTFOUND")) {
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
    private VersionInfo refreshVersionInfoFromDb(VersionInfo info) throws DotDataException,
            DotStateException {

            Identifier ident = APILocator.getIdentifierAPI().find(info.getIdentifier());
            Class clazz = UtilMethods.getVersionInfoType(ident.getAssetType());
            HibernateUtil dh = new HibernateUtil(clazz);
            dh.setQuery("from "+clazz.getName()+" where identifier=?");
            dh.setParam(info.getIdentifier());
            Logger.debug(this.getClass(), "getVersionInfo query: "+dh.getQuery());
            VersionInfo vi=(VersionInfo)dh.load();
            if(vi ==null || !UtilMethods.isSet(vi.getIdentifier())) {
            	vi = new VersionInfo();
            }

            return vi;

    }



    @Override
    protected void saveVersionInfo(VersionInfo info) throws DotDataException, DotStateException {

    	//reload versionInfo from db (JIRA-7203)
        VersionInfo vi=(VersionInfo) refreshVersionInfoFromDb(info);
        try {
			BeanUtils.copyProperties(vi, info);
		} catch (Exception e) {
			throw new DotDataException(e.getMessage());
		}

        vi.setVersionTs(new Timestamp(System.currentTimeMillis()));
        HibernateUtil.saveOrUpdate(vi);
        icache.removeVersionInfoFromCache(info.getIdentifier());
        icache.addVersionInfoToCache(info);
    }

    @Override
    protected ContentletVersionInfo getContentletVersionInfo(String identifier, long lang) throws DotDataException, DotStateException {
        ContentletVersionInfo contv = icache.getContentVersionInfo(identifier, lang);
        if(contv==null) {
            HibernateUtil dh = new HibernateUtil(ContentletVersionInfo.class);
            dh.setQuery("from "+ContentletVersionInfo.class.getName()+" where identifier=? and lang=?");
            dh.setParam(identifier);
            dh.setParam(lang);
            Logger.debug(this.getClass(), "getContentletVersionInfo query: "+dh.getQuery());
            contv = (ContentletVersionInfo)dh.load();
            if(UtilMethods.isSet(contv.getIdentifier()))
                icache.addContentletVersionInfoToCache(contv);
        }
        return contv;
    }

    @Override
    protected void saveContentletVersionInfo(ContentletVersionInfo cvInfo) throws DotDataException, DotStateException {
    	cvInfo.setVersionTs(new Timestamp(System.currentTimeMillis()));
    	HibernateUtil.saveOrUpdate(cvInfo);
        icache.removeContentletVersionInfoToCache(cvInfo.getIdentifier(),cvInfo.getLang());
    }

    @Override
    protected ContentletVersionInfo createContentletVersionInfo(Identifier identifier, long lang, String workingInode) throws DotStateException, DotDataException {
        ContentletVersionInfo cVer=new ContentletVersionInfo();
        cVer.setDeleted(false);
        cVer.setLockedBy(null);
        cVer.setLockedOn(new Timestamp(System.currentTimeMillis()));
        cVer.setIdentifier(identifier.getId());
        cVer.setLang(lang);
        cVer.setWorkingInode(workingInode);
        cVer.setVersionTs(new Timestamp(System.currentTimeMillis()));
        
        HibernateUtil.save(cVer);
        icache.addContentletVersionInfoToCache(cVer);
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
        ver.setVersionTs(new Timestamp(System.currentTimeMillis()));
        HibernateUtil.save(ver);
        icache.addVersionInfoToCache(ver);
        return ver;
    }

	@Override
	protected void deleteVersionInfo(String id) throws DotDataException {
		icache.removeVersionInfoFromCache(id);
	    VersionInfo info = getVersionInfo(id);
		if(info!=null && UtilMethods.isSet(info.getIdentifier())) {
			HibernateUtil.delete(info);
		}
	}

	@Override
	protected void deleteContentletVersionInfo(String id, long lang) throws DotDataException {
	    ContentletVersionInfo vinfo=getContentletVersionInfo(id, lang);
	    if(UtilMethods.isSet(vinfo.getIdentifier())) {
	        HibernateUtil.delete(vinfo);
	        icache.removeContentletVersionInfoToCache(id, lang);
	    }
	}
}
