package com.dotmarketing.business;

import static com.dotcms.util.CollectionsUtils.set;
import static com.dotcms.variant.VariantAPI.DEFAULT_VARIANT;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.lock.IdentifierStripedLock;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.util.transform.TransformerLocator;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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
	private IdentifierStripedLock lockManager;

	IdentifierAPI iapi = null;
	IdentifierCache icache = null;
	UserAPI userApi = null;
	ContainerAPI containerApi = null;
	TemplateAPI templateApi = null;

	private static final String CREATE_CONTENTLET_VERSION_INFO_SQL = "INSERT INTO contentlet_version_info (identifier, lang, working_inode, deleted, locked_by, locked_on, version_ts, variant_id) VALUES (?,?,?,?,?,?,?, ?)";
	private static final String INSERT_CONTENTLET_VERSION_INFO_SQL = "INSERT INTO contentlet_version_info (identifier, lang, working_inode, live_inode, deleted, locked_by, locked_on, version_ts, variant_id, publish_date) VALUES (?,?,?,?,?,?,?,?,?,?)";
	private static final String UPDATE_CONTENTLET_VERSION_INFO_SQL = "UPDATE contentlet_version_info SET working_inode=?, live_inode=?, deleted=?, locked_by=?, locked_on=?, version_ts=?, publish_date=? WHERE identifier=? AND lang=? AND variant_id = ?";

	/**
	 * Default class constructor.
	 */
	public VersionableFactoryImpl() {
		this(APILocator.getIdentifierAPI(), CacheLocator.getIdentifierCache(), APILocator.getUserAPI(),
				APILocator.getContainerAPI(), APILocator.getTemplateAPI());
		lockManager = DotConcurrentFactory.getInstance().getIdentifierStripedLock();
	}

	@VisibleForTesting
	public VersionableFactoryImpl(IdentifierAPI identifierApi, IdentifierCache identifierCache, UserAPI userApi,
			ContainerAPI containerApi, TemplateAPI templateApi) {
		this.iapi = identifierApi;
		this.icache = identifierCache;
		this.userApi = userApi;
		this.containerApi = containerApi;
		this.templateApi = templateApi;
		lockManager = DotConcurrentFactory.getInstance().getIdentifierStripedLock();
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

		if ("contentlet".equals(identifier.getAssetType())){
            try {
                return Collections.unmodifiableList(FactoryLocator.getContentletFactory()
                        .findAllVersions(identifier, true, maxResults.isPresent()?maxResults.get():null));
            } catch (DotSecurityException e) {
                throw new DotDataException("Cannot get versions for contentlet with identifier:" + identifier);
            }
        } else{
            final Class<?> clazz = InodeUtils.getClassByDBType(identifier.getAssetType());
            if(clazz.equals(Inode.class)) {

                return new ArrayList<>(1);
            }
            if(clazz.equals(Template.class)){
                final List<Versionable> templateAllVersions = new ArrayList<>();
                templateAllVersions.addAll(FactoryLocator.getTemplateFactory().findAllVersions(identifier,true));
                return templateAllVersions;
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
            if(Objects.equals(clazz, ContentletVersionInfo.class)) {
            	Optional<ContentletVersionInfo> info =
						getContentletVersionInfo(identifier,
								APILocator.getLanguageAPI().getDefaultLanguage().getId());

				if(info.isEmpty()) {
					throw new DotDataException("Can't find ContentletVersionInfo. Identifier: "
							+ identifier + ". Lang: " + APILocator.getLanguageAPI()
							.getDefaultLanguage().getId());
				}

				vi = info.get();
			} else {
				HibernateUtil dh = new HibernateUtil(clazz);
				dh.setQuery("from " + clazz.getName() + " where identifier=?");
				dh.setParam(identifier);
				Logger.debug(this.getClass(), "getVersionInfo query: " + dh.getQuery());
				vi = (VersionInfo) dh.load();
				if (!UtilMethods.isSet(vi.getIdentifier())) {
					vi.setIdentifier(identifier);
					vi.setWorkingInode("NOTFOUND");
				}
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
                    vi = (VersionInfo) clazz.getDeclaredConstructor().newInstance();
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
    protected Optional<ContentletVersionInfo> getContentletVersionInfo(final String identifier,
			final long lang) throws DotDataException, DotStateException {
        return getContentletVersionInfo(identifier, lang, DEFAULT_VARIANT.name());
    }

	@Override
	public Optional<ContentletVersionInfo> getContentletVersionInfo(
			final String identifier, final long lang, final String variantName) throws DotDataException, DotStateException{

		if (DbConnectionFactory.inTransaction()) {
			return findContentletVersionInfoInDB(identifier, lang, variantName);
		}

		ContentletVersionInfo contentVersionInfo = this.icache.getContentVersionInfo(identifier, lang, variantName);
		if(contentVersionInfo!=null && fourOhFour.equals(contentVersionInfo.getWorkingInode())) {
			Logger.debug(this, "404 ContentletVersionInfo found for id: " + identifier + " lang: " + lang + " variant: " + variantName);
			return Optional.empty();
		}else if(contentVersionInfo!=null ){
			return Optional.of(contentVersionInfo);
		}

		final Optional<ContentletVersionInfo> optionalInfo = findContentletVersionInfoInDB(identifier, lang, variantName);
		if(optionalInfo.isPresent()){
			this.icache.addContentletVersionInfoToCache(optionalInfo.get());
		}else{
			contentVersionInfo = new ContentletVersionInfo();
			contentVersionInfo.setIdentifier(identifier);
			contentVersionInfo.setLang(lang);
			contentVersionInfo.setWorkingInode(fourOhFour);
			contentVersionInfo.setVariant(variantName);

			this.icache.addContentletVersionInfoToCache(contentVersionInfo);
			return Optional.empty();
		}

		return optionalInfo;
	}

	@Override
	public Optional<ContentletVersionInfo> findContentletVersionInfoInDB(
			String identifier, long lang, String variantId) throws DotDataException, DotStateException {
		final DotConnect dotConnect = new DotConnect()
				.setSQL("SELECT * FROM contentlet_version_info WHERE identifier=? AND lang=? AND variant_id = ?")
				.addParam(identifier)
				.addParam(lang)
				.addParam(variantId);

		final List<ContentletVersionInfo> versionInfos = TransformerLocator
				.createContentletVersionInfoTransformer(dotConnect.loadObjectResults()).asList();

		return !versionInfos.isEmpty() ? Optional.of(versionInfos.get(0)) : Optional.empty();
	}

    @Override
    protected Optional<ContentletVersionInfo> findContentletVersionInfoInDB(String identifier, long lang)throws DotDataException, DotStateException {
		return findContentletVersionInfoInDB(identifier, lang, DEFAULT_VARIANT.name());
    }

	@Override
	public Optional<ContentletVersionInfo> findAnyContentletVersionInfo(final String identifier)
			throws DotDataException {
		final List<ContentletVersionInfo> result = findContentletVersionInfos(identifier, null, 1);
		return !result.isEmpty() ? Optional.of(result.get(0)) : Optional.empty();
	}

	@Override
	public Optional<ContentletVersionInfo> findAnyContentletVersionInfo(final String identifier, final boolean deleted)
			throws DotDataException {
		return findContentletVersionInfos(identifier, DEFAULT_VARIANT.name(), 0).stream().filter(cvi->!cvi.isDeleted() || deleted ).findAny();
	}

	@Override
	public Optional<ContentletVersionInfo> findAnyContentletVersionInfoAnyVariant(final String identifier, final boolean deleted)
			throws DotDataException {
		return findContentletVersionInfos(identifier, null, 0).stream().filter(cvi->!cvi.isDeleted() || deleted ).findAny();
	}

	@Override
	public Optional<ContentletVersionInfo> findAnyContentletVersionInfo(final String identifier,
			final String variant, final boolean deleted)
			throws DotDataException {
		return findContentletVersionInfos(identifier, variant, 0).stream().filter(cvi->!cvi.isDeleted() || deleted ).findAny();
	}

	private List<ContentletVersionInfo> findContentletVersionInfos(final String identifier,
			final int maxResults) throws DotDataException, DotStateException {
		return 	findContentletVersionInfos(identifier, null, maxResults);
	}

	private List<ContentletVersionInfo> findContentletVersionInfos(final String identifier, final String variantName,
			final int maxResults) throws DotDataException, DotStateException {
		List<ContentletVersionInfo> infos = findAllContentletVersionInfos(identifier);

		if (UtilMethods.isSet(variantName)) {
			infos = infos.stream().filter(i-> variantName.equals(i.getVariant())).collect(Collectors.toList());
		}

		if (maxResults > 0 && infos.size() > maxResults) {
			infos = infos.subList(0, maxResults);
		}
		return infos;

	}

	/**
	 * this will return ALL CVIs from the database
	 * @param identifier
	 * @return
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	private List<ContentletVersionInfo> findContentletVersionInfosInDB(final String identifier) throws DotDataException, DotStateException {

		DotConnect dotConnect = new DotConnect().setSQL(
						"SELECT * FROM contentlet_version_info WHERE identifier=?")
				.addParam(identifier);

		final List<ContentletVersionInfo> versionInfos = TransformerLocator
				.createContentletVersionInfoTransformer(dotConnect.loadObjectResults()).asList();

		return versionInfos == null || versionInfos.isEmpty()
				? Collections.emptyList()
				: versionInfos;
	}

	@Override
	protected List<ContentletVersionInfo> findAllContentletVersionInfos(final String identifier)
			throws DotDataException, DotStateException {
		if(identifier==null){
			throw new DotRuntimeException("identifier cannot be null");
		}

		final List<ContentletVersionInfo> infos = icache.getContentVersionInfos(identifier);
		if (infos != null) {
			return infos;
		}

		try{
		return lockManager.tryLock(identifier,()->{
			final List<ContentletVersionInfo> infos2 = icache.getContentVersionInfos(identifier);
			if (infos2 != null) {
				return infos2;
			}
			final List<ContentletVersionInfo> infos3 = findContentletVersionInfosInDB(identifier);
			icache.putContentVersionInfos(identifier, infos3);
			return infos3;
		});
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected List<ContentletVersionInfo> findAllContentletVersionInfos(final String identifier, final String variantName)
			throws DotDataException, DotStateException {
		return findContentletVersionInfos(identifier, variantName, -1);
	}

	public List<ContentletVersionInfo> findAllByVariant(Variant variant)
			throws DotDataException {

		final DotConnect dotConnect = new DotConnect()
				.setSQL("SELECT * FROM contentlet_version_info WHERE variant_id = ?")
				.addParam(variant.name());

		final List<ContentletVersionInfo> versionInfos = TransformerLocator
				.createContentletVersionInfoTransformer(dotConnect.loadObjectResults()).asList();

		return versionInfos == null || versionInfos.isEmpty()
				? Collections.emptyList()
				: versionInfos;
	}

    @Override
    protected void saveContentletVersionInfo(ContentletVersionInfo cvInfo, boolean updateVersionTS) throws DotDataException, DotStateException {
		boolean isNew = true;
		if (UtilMethods.isSet(cvInfo.getIdentifier())) {
			try {
				final Optional<ContentletVersionInfo> fromDB =
						findContentletVersionInfoInDB(cvInfo.getIdentifier(), cvInfo.getLang(), cvInfo.getVariant());
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
			dotConnect.addParam(cvInfo.getVariant());
			dotConnect.addParam(cvInfo.getPublishDate());
			dotConnect.loadResult();
        } else {
			dotConnect.setSQL(UPDATE_CONTENTLET_VERSION_INFO_SQL);
			dotConnect.addParam(cvInfo.getWorkingInode());
			dotConnect.addParam(cvInfo.getLiveInode());
			dotConnect.addParam(cvInfo.isDeleted());
			dotConnect.addParam(cvInfo.getLockedBy());
			dotConnect.addParam(cvInfo.getLockedOn());
			dotConnect.addParam(cvInfo.getVersionTs());
			dotConnect.addParam(cvInfo.getPublishDate());
			dotConnect.addParam(cvInfo.getIdentifier());
			dotConnect.addParam(cvInfo.getLang());
			dotConnect.addParam(cvInfo.getVariant());
			dotConnect.loadResult();
        }
    	this.icache.removeContentletVersionInfoToCache(cvInfo.getIdentifier(), cvInfo.getLang(), cvInfo.getVariant());
    }

    @Override
    protected ContentletVersionInfo createContentletVersionInfo(Identifier identifier, long lang, String workingInode) throws DotStateException, DotDataException {
		return createContentletVersionInfo(identifier, lang, workingInode, DEFAULT_VARIANT.name());
    }

	@Override
	protected ContentletVersionInfo createContentletVersionInfo(final Identifier identifier,
			final long lang, final String workingInode, final String variantId)
			throws DotStateException, DotDataException {

		DotConnect dotConnect = new DotConnect();
		dotConnect.setSQL(CREATE_CONTENTLET_VERSION_INFO_SQL);
		dotConnect.addParam(identifier.getId());
		dotConnect.addParam(lang);
		dotConnect.addParam(workingInode);
		dotConnect.addParam(false);
		dotConnect.addParam((String) null);
		dotConnect.addParam(new Date());
		dotConnect.addParam(new Date());
		dotConnect.addParam(variantId);
		dotConnect.loadResult();

		return findContentletVersionInfoInDB(identifier.getId(), lang, variantId).get();
	}

    @Override
    protected VersionInfo createVersionInfo(Identifier identifier, String workingInode) throws DotStateException, DotDataException {
        Class<?> clazz=UtilMethods.getVersionInfoType(identifier.getAssetType());
        VersionInfo ver;
        try {
            ver = (VersionInfo)clazz.getDeclaredConstructor().newInstance();
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
		new DotConnect().setSQL("DELETE FROM contentlet_version_info WHERE identifier=? AND lang=?")
				.addParam(id).addParam(lang).loadResult();
		this.icache.removeContentletVersionInfoToCache(id, lang);
	}

	@Override
	protected void deleteContentletVersionInfo(String id, final String variantId) throws DotDataException {
		new DotConnect().setSQL("DELETE FROM contentlet_version_info WHERE identifier=? AND variant_id=?")
				.addParam(id).addParam(variantId).loadResult();
		//this.icache.removeContentletVersionInfoToCache(id, lang);
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
