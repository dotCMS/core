package com.dotmarketing.business;

import static com.dotcms.util.CollectionsUtils.set;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.util.transform.TransformerLocator;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.beanutils.BeanUtils;

/**
 * Implementation class for the {@link VersionableFactory} class.
 *
 * @author Will Ezell
 * @version 1.0
 * @since Mar 22, 2012
 *
 */
public class VersionableFactoryImpl extends VersionableFactory {

	private final String fourOhFour = "NOTFOUND";

	IdentifierAPI iapi = null;
	IdentifierCache icache = null;
	UserAPI userApi = null;
	ContainerAPI containerApi = null;
	TemplateAPI templateApi = null;

	private static final String CREATE_CONTENTLET_VERSION_INFO_SQL = "INSERT INTO contentlet_version_info (identifier, lang, working_inode, deleted, locked_by, locked_on, version_ts) VALUES (?,?,?,?,?,?,?)";
	private static final String INSERT_CONTENTLET_VERSION_INFO_SQL = "INSERT INTO contentlet_version_info (identifier, lang, working_inode, live_inode, deleted, locked_by, locked_on, version_ts) VALUES (?,?,?,?,?,?,?,?)";
	private static final String UPDATE_CONTENTLET_VERSION_INFO_SQL = "UPDATE contentlet_version_info SET working_inode=?, live_inode=?, deleted=?, locked_by=?, locked_on=?, version_ts=? WHERE identifier=? AND lang=?";

	/**
	 * Default class constructor.
	 */
	public VersionableFactoryImpl() {
		this(APILocator.getIdentifierAPI(), CacheLocator.getIdentifierCache(), APILocator.getUserAPI(),
				APILocator.getContainerAPI(), APILocator.getTemplateAPI());
	}

	@VisibleForTesting
	public VersionableFactoryImpl(IdentifierAPI identifierApi, IdentifierCache identifierCache, UserAPI userApi,
			ContainerAPI containerApi, TemplateAPI templateApi) {
		this.iapi = identifierApi;
		this.icache = identifierCache;
		this.userApi = userApi;
		this.containerApi = containerApi;
		this.templateApi = templateApi;
	}

	@Override
	protected Versionable findWorkingVersion(String id) throws DotDataException, DotStateException {
		Identifier identifier = this.iapi.find(id);
		if(identifier==null || !InodeUtils.isSet(identifier.getId())){
			throw new DotDataException("Identifier: " + id + " not found.");
		}
		if(identifier.getAssetType().equals("contentlet")) {
		    throw new DotDataException("Contentlets could have working versions for each language.");
		}
		VersionInfo vinfo = getVersionInfo(identifier.getId());
		if(!UtilMethods.isSet(vinfo)){
			throw new DotDataException("No version info for identifier : " + id);
		}
		Class<?> clazz = InodeUtils.getClassByDBType(identifier.getAssetType());
		Versionable ver = null;
		User user = this.userApi.getSystemUser();
		String workingInode = vinfo.getWorkingInode();
		Set<Class<?>> versionableWhitelist = getVersionableWhitelist();
		// Ignore Links, WorkflowMessages and Inode
		if (versionableWhitelist.contains(clazz)) {
			try {
				if (Container.class.equals(clazz)) {
					ver = this.containerApi.find(workingInode, user, true);
				} else if (Template.class.equals(clazz)) {
					ver = this.templateApi.find(workingInode, user, true);
				}
				if (ver == null) {
					Logger.warn(this.getClass(), "Versionable object is null when finding working version '" + clazz.getName()
							+ "'. Trying old method.");
				}
			} catch (Exception e) {
				Logger.error(this.getClass(), "Error finding the working version of '" + clazz + "', with Identifier: " + id);
			}
		}
		if(ver == null){
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
		Identifier identifier = this.iapi.find(id);
		if(identifier==null || !InodeUtils.isSet(identifier.getId())){
			throw new DotDataException("identifier: " + id + " not found.");
		}
		if(identifier.getAssetType().equals("contentlet")) {
            throw new DotDataException("Contentlets could have live versions for each language.");
		}
		VersionInfo vinfo = getVersionInfo(identifier.getId());
		Versionable ver = null;
		User user = this.userApi.getSystemUser();
		Class<?> clazz = InodeUtils.getClassByDBType(identifier.getAssetType());
		if(UtilMethods.isSet(vinfo)) {
			if(UtilMethods.isSet(vinfo.getLiveInode())){
				String liveInode = vinfo.getLiveInode();
				Set<Class<?>> versionableWhitelist = getVersionableWhitelist();
				// Ignore Links, WorkflowMessages and Inode
				if (versionableWhitelist.contains(clazz)) {
					try {
						if (Container.class.equals(clazz)) {
							ver = this.containerApi.find(liveInode, user, true);
						} else if (Template.class.equals(clazz)) {
							ver = this.templateApi.find(liveInode, user, true);
						}
						if (ver == null) {
							Logger.warn(this.getClass(), "Versionable object is null when finding working version '" + clazz.getName()
									+ "'. Trying old method.");
						}
					} catch (Exception e) {
						Logger.error(this.getClass(),
								"Error finding the live version of '" + clazz + "', with Identifier: " + id);
					}
				}
				if(ver==null){
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
		Class<?> clazz = InodeUtils.getClassByDBType(identifier.getAssetType());
		HibernateUtil dh = new HibernateUtil(clazz);
		dh.setQuery("from inode in class " + clazz.getName() + " where identifier = ? and inode.type='" + identifier.getAssetType() + "' and deleted="
				+ DbConnectionFactory.getDBTrue());
		dh.setParam(id);
		Logger.debug(this.getClass(), "findDeletedVersion query: " + dh.getQuery());
		return (Versionable) dh.load();
	}

	@Override
	protected List<Versionable> findAllVersions(String id) throws DotDataException, DotStateException {

		return findAllVersions(id, Optional.empty());
	}

	protected  List<Versionable> findAllVersions(final String id, final Optional<Integer> maxResults) throws DotDataException, DotStateException {

		final Identifier identifier = this.iapi.find(id);

		if(identifier ==null) {

			throw new DotDataException("identifier:" + identifier +" not found");
		}

		final Class<?> clazz = InodeUtils.getClassByDBType(identifier.getAssetType());
		if(clazz.equals(Inode.class)) {

			return new ArrayList<Versionable>(1);
		}

		final HibernateUtil dh = new HibernateUtil(clazz);
		dh.setQuery("from inode in class " + clazz.getName() + " where inode.identifier = ? and inode.type='" + identifier.getAssetType() + "' order by mod_date desc");
		dh.setParam(id);

		if (maxResults.isPresent()) {
			dh.setMaxResults(maxResults.get());
		}

		Logger.debug(this.getClass(), "findAllVersions query: " + dh.getQuery());
		return (List<Versionable>) dh.list();
	}

    @Override
    protected VersionInfo getVersionInfo(String identifier) throws DotDataException,
            DotStateException {
        VersionInfo vi = this.icache.getVersionInfo(identifier);
        if(vi==null || vi.getWorkingInode().equals(fourOhFour)) {
            Identifier ident = this.iapi.find(identifier);
            if(ident==null || !UtilMethods.isSet(ident.getId()))
                return null;
            Class<?> clazz = UtilMethods.getVersionInfoType(ident.getAssetType());
            HibernateUtil dh = new HibernateUtil(clazz);
            dh.setQuery("from "+clazz.getName()+" where identifier=?");
            dh.setParam(identifier);
            Logger.debug(this.getClass(), "getVersionInfo query: "+dh.getQuery());
            vi=(VersionInfo)dh.load();
            if(!UtilMethods.isSet(vi.getIdentifier())) {
            	vi.setIdentifier(identifier);
            	vi.setWorkingInode("NOTFOUND");
            }
            this.icache.addVersionInfoToCache(vi);
        }
        if(vi.getWorkingInode().equals("NOTFOUND")) {
            return null;
        } else {
        	return vi;
        }
    }

    @Override
    protected VersionInfo findVersionInfoFromDb(Identifier identifer) throws DotDataException,
            DotStateException {
            Class<?> clazz = UtilMethods.getVersionInfoType(identifer.getAssetType());
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
        Identifier ident = this.iapi.find(info.getIdentifier());
        VersionInfo vi=(VersionInfo) findVersionInfoFromDb(ident);
        boolean isNew = vi==null || !InodeUtils.isSet(vi.getIdentifier());
        try {
			BeanUtils.copyProperties(vi, info);
		} catch (Exception e) {
			throw new DotDataException(e.getMessage(),e);
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
        this.icache.removeVersionInfoFromCache(vi.getIdentifier());
    }

    @Override
    protected Optional<ContentletVersionInfo> getContentletVersionInfo(String identifier, long lang) throws DotDataException, DotStateException {
        if (DbConnectionFactory.inTransaction()) {
            return findContentletVersionInfoInDB(identifier, lang);
        }
        ContentletVersionInfo contentVersionInfo = this.icache.getContentVersionInfo(identifier, lang);
        if(contentVersionInfo!=null && fourOhFour.equals(contentVersionInfo.getWorkingInode())) {
        	return Optional.empty();
        }else if(contentVersionInfo!=null ){
        	return Optional.of(contentVersionInfo);
        }

    	final Optional<ContentletVersionInfo> optionalInfo = findContentletVersionInfoInDB(identifier, lang);
        if(optionalInfo.isPresent()){
        	this.icache.addContentletVersionInfoToCache(optionalInfo.get());
        }else{
        	contentVersionInfo = new ContentletVersionInfo();
        	contentVersionInfo.setIdentifier(identifier);
        	contentVersionInfo.setLang(lang);
        	contentVersionInfo.setWorkingInode(fourOhFour);
        	this.icache.addContentletVersionInfoToCache(contentVersionInfo);
			return Optional.empty();
        }

        return optionalInfo;
    }

    @Override
    protected Optional<ContentletVersionInfo> findContentletVersionInfoInDB(String identifier, long lang)throws DotDataException, DotStateException {
		final DotConnect dotConnect = new DotConnect()
				.setSQL("SELECT * FROM contentlet_version_info WHERE identifier=? AND lang=?")
				.addParam(identifier)
				.addParam(lang);

		List<ContentletVersionInfo> versionInfos = TransformerLocator
				.createContentletVersionInfoTransformer(dotConnect.loadObjectResults()).asList();

		return !versionInfos.isEmpty() ? Optional.of(versionInfos.get(0)) : Optional.empty();
    }
    @Override
    protected List<ContentletVersionInfo> findAllContentletVersionInfos(final String identifier)throws DotDataException, DotStateException {

         HibernateUtil dh = new HibernateUtil(ContentletVersionInfo.class);
         dh.setQuery("from "+ContentletVersionInfo.class.getName()+" where identifier=? ");
         dh.setParam(identifier);

         Logger.debug(this.getClass(), "getContentletVersionInfo query: "+dh.getQuery());
         return (List<ContentletVersionInfo>)dh.list();

    }
    @Override
    protected void saveContentletVersionInfo(ContentletVersionInfo cvInfo, boolean updateVersionTS) throws DotDataException, DotStateException {
		boolean isNew = true;
		if (UtilMethods.isSet(cvInfo.getIdentifier())) {
			try {
				final Optional<ContentletVersionInfo> fromDB =
						findContentletVersionInfoInDB(cvInfo.getIdentifier(), cvInfo.getLang());
				if (fromDB.isPresent()) {
					isNew = false;
				}
			} catch (final Exception e) {
				Logger.debug(this.getClass(), e.getMessage(), e);
			}
		} else {
			cvInfo.setIdentifier(UUIDGenerator.generateUuid());
		}

		if(updateVersionTS){
			cvInfo.setVersionTs(new Date());
		}

		final DotConnect dotConnect = new DotConnect();

    	if(isNew) {
			dotConnect.setSQL(INSERT_CONTENTLET_VERSION_INFO_SQL);
			dotConnect.addParam(cvInfo.getIdentifier());
			dotConnect.addParam(cvInfo.getLang());
			dotConnect.addParam(cvInfo.getWorkingInode());
			dotConnect.addParam(cvInfo.getLiveInode());
			dotConnect.addParam(cvInfo.isDeleted());
			dotConnect.addParam(cvInfo.getLockedBy());
			dotConnect.addParam(cvInfo.getLockedOn());
			dotConnect.addParam(cvInfo.getVersionTs());
			dotConnect.loadResult();
        } else {
			dotConnect.setSQL(UPDATE_CONTENTLET_VERSION_INFO_SQL);
			dotConnect.addParam(cvInfo.getWorkingInode());
			dotConnect.addParam(cvInfo.getLiveInode());
			dotConnect.addParam(cvInfo.isDeleted());
			dotConnect.addParam(cvInfo.getLockedBy());
			dotConnect.addParam(cvInfo.getLockedOn());
			dotConnect.addParam(cvInfo.getVersionTs());
			dotConnect.addParam(cvInfo.getIdentifier());
			dotConnect.addParam(cvInfo.getLang());
			dotConnect.loadResult();
        }
    	this.icache.removeContentletVersionInfoToCache(cvInfo.getIdentifier(),cvInfo.getLang());
    }

    @Override
    protected ContentletVersionInfo createContentletVersionInfo(Identifier identifier, long lang, String workingInode) throws DotStateException, DotDataException {
		DotConnect dotConnect = new DotConnect();
		dotConnect.setSQL(CREATE_CONTENTLET_VERSION_INFO_SQL);
		dotConnect.addParam(identifier.getId());
		dotConnect.addParam(lang);
		dotConnect.addParam(workingInode);
		dotConnect.addParam(false);
		dotConnect.addParam((String) null);
		dotConnect.addParam(new Date());
		dotConnect.addParam(new Date());
		dotConnect.loadResult();

		return findContentletVersionInfoInDB(identifier.getId(), lang).get();
    }

    @Override
    protected VersionInfo createVersionInfo(Identifier identifier, String workingInode) throws DotStateException, DotDataException {
        Class<?> clazz=UtilMethods.getVersionInfoType(identifier.getAssetType());
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
		this.icache.removeVersionInfoFromCache(id);
	    VersionInfo info = getVersionInfo(id);
		if(info!=null && UtilMethods.isSet(info.getIdentifier())) {
			String ident = info.getIdentifier();
			HibernateUtil.delete(info);
			this.icache.removeFromCacheByIdentifier(ident);
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
        	this.icache.removeContentletVersionInfoToCache(id, lang);
        }
	}

	/**
	 * Returns the set of dotCMS objects whose information can be looked up via
	 * APIs before performing a database lookup. This approach allows the
	 * information to be obtained by checking the cache first instead of going
	 * directly to the database, which is more expensive.
	 *
	 * @return The set of classes representing the objects that need to be
	 *         looked up using their respective APIs before performing a
	 *         database query.
	 */
	private Set<Class<?>> getVersionableWhitelist() {
		return set(Container.class, Template.class);
	}

}
