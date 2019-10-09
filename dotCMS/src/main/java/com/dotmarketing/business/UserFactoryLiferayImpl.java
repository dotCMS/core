package com.dotmarketing.business;


import com.dotmarketing.cms.factories.PublicAddressFactory;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.UserFirstNameException;
import com.dotmarketing.exception.UserLastNameException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.DuplicateUserEmailAddressException;
import com.liferay.portal.DuplicateUserIdException;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.AddressLocalManagerUtil;
import com.liferay.portal.ejb.CompanyLocalManagerUtil;
import com.liferay.portal.ejb.UserLocalManagerUtil;
import com.liferay.portal.model.Address;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.logging.log4j.util.Strings;

/**
 * @author Jason Tesser
 *
 */
public class UserFactoryLiferayImpl extends UserFactory {

	private UserCache uc;

	/**
	 * Default class constructor.
	 */
	public UserFactoryLiferayImpl() {
		uc = CacheLocator.getUserCache();
	}
	
	@Override
	protected User loadDefaultUser() throws DotDataException, NoSuchUserException {
		Company company = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany();
		try {
			return loadUserById(User.getDefaultUserId(company.getCompanyId()));
		} catch (NoSuchUserException e) {
			Logger.debug(this, "Default user not found attempting to create by updating company");
			try {
				CompanyLocalManagerUtil.createDefaultUser(company);
			} catch (Exception e1) {
				throw new DotDataException("Unable to create deafult user from company", e1);
			}
		}
		return loadUserById(User.getDefaultUserId(company.getCompanyId()));
	}
	
	@Override
	public User createUser(String userId, String email) throws DotDataException, DuplicateUserException {
        Company comp = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany();
		String companyId = comp.getCompanyId();

		User defaultUser = APILocator.getUserAPI().getDefaultUser();
	
		if(!UtilMethods.isSet(userId)){
	        userId = "user-" + UUIDUtil.uuid();
		}


		if(!UtilMethods.isSet(email)){
			email = userId + "@fakedotcms.com";
		}
		
		User user;
		try {
			user = UserLocalManagerUtil.addUser(companyId, false, userId, true, null, null, false, userId, null, userId, null, true, null, email, defaultUser.getLocale());
		}catch (DuplicateUserEmailAddressException e) {
			Logger.info(this, "User already exists with this email");
			throw new DuplicateUserException(e.getMessage(), e);
		}catch (DuplicateUserIdException e) {
			Logger.info(this, "User already exists with this ID");
			throw new DuplicateUserException(e.getMessage(), e);
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		}
		user.setLanguageId(defaultUser.getLocale().toString());
		user.setTimeZoneId(defaultUser.getTimeZoneId());
		user.setSkinId(defaultUser.getSkinId());
		user.setDottedSkins(defaultUser.isDottedSkins());
		user.setRoundedSkins(defaultUser.isRoundedSkins());
		user.setResolution(defaultUser.getResolution());
		user.setRefreshRate(defaultUser.getRefreshRate());
		user.setLayoutIds("");
		user.setNew(false);
		user.setCompanyId(companyId);

		return user;
	}
	
	@Override
	public User loadUserById(String userId) throws DotDataException, NoSuchUserException {
		User u = uc.get(userId);
		if(!UtilMethods.isSet(u)){
			try{
				u = UserLocalManagerUtil.getUserById(userId);
			}catch (com.liferay.portal.NoSuchUserException e) {
				throw new NoSuchUserException(e.getMessage(), e);
			}catch (Exception e) {
				throw new DotDataException(e.getMessage(), e);
			}
			uc.add(userId, u);
		}
		return u;
	}

	@Override
	public User loadByUserByEmail(String email) throws DotDataException, DotSecurityException, NoSuchUserException {
		String companyId = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany().getCompanyId();
		User u;
		try {
			u = UserLocalManagerUtil.getUserByEmailAddress(companyId, email);
		}catch (com.liferay.portal.NoSuchUserException e) {
			throw new NoSuchUserException(e.getMessage(), e);
		} catch (Exception e) {
			throw new DotDataException(e.getMessage(), e);
		}
		return u;
	}
	
	@Override
	public List<User> findAllUsers(int begin, int end) throws DotDataException {
		try {
			return CompanyLocalManagerUtil.getUsers(PublicCompanyFactory.getDefaultCompany().getCompanyId(), begin, end);
		} catch (SystemException e) {
			Logger.error(this, "getAllUsers: error", e);
			throw new DotDataException(e.getMessage(), e);
		}
	}
	
	@Override
	public List<User> findAllUsers() throws DotDataException {
		try {
			return CompanyLocalManagerUtil.getUsers(PublicCompanyFactory.getDefaultCompany().getCompanyId());
		} catch (SystemException e) {
			Logger.error(this, "getAllUsers: error", e);
			throw new DotDataException(e.getMessage(), e);
		}
	}

	@Override
	public long getCountUsersByName(final String filter) throws DotDataException {
		return getCountUsersByName(filter, ImmutableList.of());
	}

    @Override
	protected long getCountUsersByName(String filter, final List<Role> roles) throws DotDataException{
		filter = SQLUtil.sanitizeParameter(filter);
		DotConnect dotConnect = new DotConnect();
		boolean isFilteredByName = UtilMethods.isSet(filter);
		filter = (isFilteredByName ? filter : Strings.EMPTY);
		StringBuilder baseSql = new StringBuilder("select count(*) as count from user_ where companyid = ? and userid <> 'system' ");
		if(UtilMethods.isSet(roles)){
			final String joinedRoleKeys = roles.stream().map(Role::getRoleKey).map(s -> String.format("'%s'",s)).collect(Collectors.joining(","));
			final String backendRoleFilter = String.format(" and exists ( select ur.user_id from users_cms_roles ur join cms_role r on ur.role_id = r.id where r.role_key in (%s) and ur.user_id = user_.userId )", joinedRoleKeys);
			baseSql.append(backendRoleFilter);
		}
		String userFullName = DotConnect.concat( new String[]{ "firstname", "' '", "lastname" } );

		if( isFilteredByName ) {
			baseSql.append(" and lower(");
			baseSql.append(userFullName);
			baseSql.append(") like ?");
		}

		baseSql.append(" and delete_in_progress = ");
		baseSql.append(DbConnectionFactory.getDBFalse());

		String sql = baseSql.toString();
		dotConnect.setSQL(sql);
		Logger.debug( UserFactoryLiferayImpl.class,"::getCountUsersByName -> query: " + dotConnect.getSQL() );

		dotConnect.addParam(PublicCompanyFactory.getDefaultCompanyId());
		if(isFilteredByName) {
			dotConnect.addParam("%"+filter.toLowerCase()+"%");
		}

		return dotConnect.getInt("count");
	}

	@Override
	public List<User> getUsersByName(final String filter, final int start, final int limit) throws DotDataException {
		return getUsersByName(filter, ImmutableList.of(), start,limit);
	}

	@Override
	public List<User> getUsersByName(final String filter, final List<Role> roles ,final int start, final int limit) throws DotDataException {
		String sanitizeFilter = SQLUtil.sanitizeParameter(filter);
		DotConnect dotConnect = new DotConnect();
		boolean isFilteredByName = UtilMethods.isSet(sanitizeFilter);
		sanitizeFilter = (isFilteredByName ? sanitizeFilter : Strings.EMPTY);
		final StringBuilder baseSql = new StringBuilder("select user_.userId from user_ where companyid = ? and userid <> 'system' ");
		if(UtilMethods.isSet(roles)){
			final String joinedRoleKeys = roles.stream().map(Role::getRoleKey).map(s -> String.format("'%s'",s)).collect(Collectors.joining(","));
			final String backendRoleFilter = String.format(" and exists ( select ur.user_id from users_cms_roles ur join cms_role r on ur.role_id = r.id where r.role_key in (%s) and ur.user_id = user_.userId )", joinedRoleKeys);
			baseSql.append(backendRoleFilter);
		}
		final String userFullName = DotConnect.concat( new String[]{ "firstname", "' '", "lastname" } );

		if( isFilteredByName ) {
			baseSql.append(" and lower(");
			baseSql.append(userFullName);
			baseSql.append(") like ?");
		}

		baseSql.append(" and delete_in_progress = ");
		baseSql.append(DbConnectionFactory.getDBFalse());

		baseSql.append(" order by ");
		baseSql.append(userFullName);

		final String sql = baseSql.toString();
		dotConnect.setSQL(sql);
		Logger.debug( UserFactoryLiferayImpl.class,"::getUsersByName -> query: " + dotConnect.getSQL() );

		dotConnect.addParam(PublicCompanyFactory.getDefaultCompanyId());
		if(isFilteredByName) {
			dotConnect.addParam("%"+sanitizeFilter.toLowerCase()+"%");
		}

		if(start > -1){
			dotConnect.setStartRow(start);
		}
		if(limit > -1) {
			dotConnect.setMaxRows(limit);
		}
		final List<Map<String, Object>> results = dotConnect.loadResults();

		// Since limit is a small number, convert each row to appropriate entity
		final List<User> users = new ArrayList<User>();

		for (final Map<String, Object> hash : results) {
			final String userId = (String) hash.get("userid");
			final User u = loadUserById(userId);
			users.add(u);
			uc.add(u.getUserId(), u);
		}

		return users;
	}

	@Override
	public User saveUser(User user) throws DotDataException,DuplicateUserException {
		if (user.getUserId() == null) {
			throw new DotRuntimeException("Can't save a user without a userId");
		}
		
		try {
			User oldUser = UserLocalManagerUtil.getUserById(user.getUserId());
			if(!oldUser.getEmailAddress().equals(user.getEmailAddress())){
				User emailUser = null;
				try{ 
					emailUser = UserLocalManagerUtil.getUserByEmailAddress(user.getCompanyId(), user.getEmailAddress());
				}catch(Exception e){}
				if(emailUser!=null){
					throw new com.dotmarketing.business.DuplicateUserException("User already exists with this email");
				}
			}
			user.setModified(true);
			String emailAddress = user.getEmailAddress();
			if(UtilMethods.isSet(emailAddress))
			{
				user.setEmailAddress(emailAddress.trim().toLowerCase());
			}
			User u =  UserLocalManagerUtil.updateUser(user);
			return u;
		} catch (com.liferay.portal.UserFirstNameException e) {
			Logger.error(this, e.getMessage(), e);
			throw new UserFirstNameException(e);
		} catch (com.liferay.portal.UserLastNameException e) {
			Logger.error(this, e.getMessage(), e);
			throw new UserLastNameException(e);
		} catch (PortalException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		} catch (SystemException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException("saving a user failed", e);
		}
	}
	
	@Override
	public boolean userExistsWithEmail(String email) throws DotDataException {
		String companyId = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany().getCompanyId();
		User u;
		try {
			u = UserLocalManagerUtil.getUserByEmailAddress(companyId, email);
		} catch (Exception e) {
			throw new DotDataException(e.getMessage(), e);
		}
		if(UtilMethods.isSet(u)){
			uc.add(u.getUserId(), u);
			return true;
		}
		return false;
	}
	
	@Override
	public long getCountUsersByNameOrEmail(String filter) throws DotDataException {
		filter = (UtilMethods.isSet(filter) ? filter.toLowerCase() : "");
		filter = SQLUtil.sanitizeParameter(filter);    	
		StringBuilder sql = new StringBuilder("select count(*) as count from user_ where ");
		sql.append("(lower(firstName) like '%");
		sql.append(filter);
		sql.append("%' or lower(lastName) like '%");
		sql.append(filter);
		sql.append("%' or ");
		sql.append("lower(emailAddress) like '%");
		sql.append(filter);
		sql.append("%' or ");
		sql.append(DotConnect.concat(new String[] { "lower(firstName)", "' '", "lower(lastName)" }));
		sql.append(" like '%");
		sql.append(filter);
		sql.append("%')");
		sql.append(" and delete_in_progress = ");
		sql.append(DbConnectionFactory.getDBFalse());
	
		DotConnect dotConnect = new DotConnect();
		dotConnect.setSQL(sql.toString());
		return dotConnect.getInt("count");
	}
	
	@Override
	public List<User> getUsersByNameOrEmail(String filter, int page, int pageSize) throws DotDataException {
		List users = new ArrayList(pageSize);
		if(page==0){
			page = 1;
		}
		int bottom = ((page - 1) * pageSize);
		int top = (page * pageSize);
		filter = (UtilMethods.isSet(filter) ? filter.toLowerCase() : "");
		filter = SQLUtil.sanitizeParameter(filter);
		StringBuilder sql = new StringBuilder("select userid from user_ where (lower(userid) like '%");
		sql.append(filter);
		sql.append("%' or lower(firstName) like '%");
		sql.append(filter);
		sql.append("%' or lower(lastName) like '%");
		sql.append(filter);
		sql.append("%' or lower(emailAddress) like '%");
		sql.append(filter);
		sql.append("%' ");
		sql.append(" or ");
		sql.append(DotConnect.concat(new String[]{"lower(firstName)", "' '", "lower(lastName)"}));
		sql.append(" like '%");
		sql.append(filter);
		sql.append("%') AND userid <> 'system' ");
		sql.append(" and delete_in_progress = ");
		sql.append(DbConnectionFactory.getDBFalse());
		sql.append(" order by firstName asc,lastname asc");


		DotConnect dotConnect = new DotConnect();
		dotConnect.setSQL(sql.toString());
		dotConnect.setMaxRows(top);
		List results = dotConnect.getResults();
		
		int lenght = results.size();
		for(int i = 0;i < lenght;i++)
		{
			if(i >= bottom)
			{
				if(i < top)
				{
					HashMap hash = (HashMap) results.get(i);
					String userId = (String) hash.get("userid");
					users.add(loadUserById(userId));
				}
				else
				{
					break;
				}
			}    			
		}
		return users;    		
	}
	
	@Override
	public long getCountUsersByNameOrEmailOrUserID(String filter) throws DotDataException {
		return getCountUsersByNameOrEmailOrUserID(filter, true);
	}

	@Override
	public long getCountUsersByNameOrEmailOrUserID(String filter, boolean includeAnonymous) throws DotDataException {
		return getCountUsersByNameOrEmailOrUserID(filter, true, true);
	}

	
	 @Override
	  public long getCountUsersByNameOrEmailOrUserID(String filter, boolean includeAnonymous, boolean includeDefault)  throws DotDataException {
	   return getCountUsersByNameOrEmailOrUserID(filter, true, true, null);
	 }
	
	
	@Override
	public long getCountUsersByNameOrEmailOrUserID(String filter, boolean includeAnonymous, boolean includeDefault, String roleId)
			throws DotDataException {
    filter = (UtilMethods.isSet(filter) ? "%" + filter.toLowerCase() + "%" :null);

		
    DotConnect dotConnect = new DotConnect();
    StringBuilder sql = new StringBuilder("select count(*) as count from user_ ");
    if(roleId!=null) {
      sql.append(", users_cms_roles ");
    }
    
    sql.append(" where 1=1 ");
    if(filter!=null) {
      sql.append(" AND (lower(userid) like ? or lower(firstName) like ? or lower(lastName) like ? or lower(emailAddress) like ?) ");
    }
    sql.append(" AND userid <> 'system' ");
    sql.append(((!includeAnonymous) ? "AND userid <> 'anonymous'" : " "));
    sql.append(((!includeDefault) ? "AND userid <> 'dotcms.org.default'" : " "));
    sql.append(" AND delete_in_progress = ");
    sql.append(DbConnectionFactory.getDBFalse());
    if(roleId!=null) {
      sql.append(" AND role_id = ? " );
      sql.append(" AND users_cms_roles.user_id=user_.userid " );
      
    }


    dotConnect.setSQL(sql.toString());
    if(filter!=null) {
      dotConnect.addParam(filter);
      dotConnect.addParam(filter);
      dotConnect.addParam(filter);
      dotConnect.addParam(filter);
    }
    if(roleId!=null) {
      dotConnect.addParam(roleId);
    }
    
		return dotConnect.getInt("count");
	}

	@Override
	public List<User> getUsersByNameOrEmailOrUserID(String filter, int page, int pageSize) throws DotDataException {
		return getUsersByNameOrEmailOrUserID(filter, page, pageSize, true);
	}

	@Override
	public List<User> getUsersByNameOrEmailOrUserID(String filter, int page, int pageSize, boolean includeAnonymous) throws DotDataException {
		return getUsersByNameOrEmailOrUserID(filter, page, pageSize, includeAnonymous, true);
	}
  @Override
  protected List<User> getUsersByNameOrEmailOrUserID(String filter, int page,
      int pageSize, boolean includeAnonymous, boolean includeDefault) throws DotDataException {
    return getUsersByNameOrEmailOrUserID(filter, page, pageSize, includeAnonymous, includeDefault, null);
    
  }
	@Override
	protected List<User> getUsersByNameOrEmailOrUserID(String filter, int page,
			int pageSize, boolean includeAnonymous, boolean includeDefault, String roleId) throws DotDataException {
	  

		List<User> users = new ArrayList<>(pageSize);
		
		if (page == 0) {
			page = 1;
		}
		int bottom = ((page - 1) * pageSize);
		int top = (page * pageSize);
		filter = (UtilMethods.isSet(filter) ? "%" + filter.toLowerCase() + "%" :null);
		
		DotConnect dotConnect = new DotConnect();
		StringBuilder sql = new StringBuilder("select userid from user_ ");
		if(roleId!=null) {
		  sql.append(", users_cms_roles ");
		}
		
		sql.append(" where 1=1 ");
		if(filter!=null) {
		  sql.append(" AND (lower(userid) like ? or lower(firstName) like ? or lower(lastName) like ? or lower(emailAddress) like ?) ");
		}
		sql.append(" AND userid <> 'system' ");
		sql.append(((!includeAnonymous) ? " AND userid <> 'anonymous'" : " "));
		sql.append(((!includeDefault) ? " AND userid <> 'dotcms.org.default'" : " "));
		sql.append(" AND delete_in_progress = ");
		sql.append(DbConnectionFactory.getDBFalse());
		if(roleId!=null) {
		  sql.append(" AND role_id = ? " );
		  sql.append(" AND users_cms_roles.user_id=user_.userid " );
		}

		sql.append(" order by firstName asc,lastname asc");
		dotConnect.setSQL(sql.toString());
		if(filter!=null) {
		  dotConnect.addParam(filter);
		  dotConnect.addParam(filter);
		  dotConnect.addParam(filter);
		  dotConnect.addParam(filter);
		}
		if(roleId!=null) {
		  dotConnect.addParam(roleId);
		}
		
		
		dotConnect.setMaxRows(top);
		List<?> results = dotConnect.getResults();
		int lenght = results.size();
		for (int i = 0; i < lenght; i++) {
			if (i >= bottom) {
				if (i < top) {
					Map<String, Object> hash = (HashMap) results.get(i);
					String userId = (String) hash.get("userid");
					users.add(loadUserById(userId));
				} else {
					break;
				}
			}
		}
		return users;
	}
	

	
	
	
	

	@Override
	protected List<User> getUnDeletedUsers() throws DotDataException {

		List<User> users = new ArrayList();
		String date;
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		//By default, the filter searches by registers whose delete_date value is less equals than 24 hours
		calendar.add(Calendar.HOUR, Config.getIntProperty("CLEAN_UNDELETED_USERS_INTERVAL", -25));

		StringBuilder sql = new StringBuilder("select userid from user_ where ");

		sql.append("delete_in_progress = ");
		sql.append(DbConnectionFactory.getDBTrue());

		if (DbConnectionFactory.isOracle()) {
			sql.append(" and delete_date<=to_date('");
			sql.append(format.format(calendar.getTime()));
			sql.append("', 'YYYY-MM-DD HH24:MI:SS')");
		} else {
			sql.append(" and delete_date<='");
			sql.append(format.format(calendar.getTime()));
			sql.append("'");
		}

		DotConnect dotConnect = new DotConnect();
		dotConnect.setSQL(sql.toString());

		List results = dotConnect.loadResults();

		for (int i = 0; i < results.size(); i++) {
			HashMap hash = (HashMap) results.get(i);
			String userId = (String) hash.get("userid");
			users.add(loadUserById(userId));
		}
		return users;
	}

    @Override
    public List<String> getUsersIdsByCreationDate ( Date filterDate, int start, int limit ) throws DotDataException {

        DotConnect dotConnect = new DotConnect();
        //Build the sql query
		StringBuffer
			query =
			new StringBuffer("SELECT user_.userId FROM user_ WHERE companyid = ? AND userid <> 'system' ");
		if (UtilMethods.isSet(filterDate)) {
			query.append(" AND createdate >= ?");
		}

		query.append(" AND delete_in_progress = ");
		query.append(DbConnectionFactory.getDBFalse());

		query.append(" ORDER BY firstName ASC, lastname ASC");
		dotConnect.setSQL( query.toString() );
        Logger.debug( UserFactoryLiferayImpl.class, "::getUsersByCreationDate -> query: " + dotConnect.getSQL() );

        //Add the required params
        dotConnect.addParam( PublicCompanyFactory.getDefaultCompanyId() );
        if ( UtilMethods.isSet( filterDate ) ) {
            dotConnect.addParam( filterDate );
        }

        //Load the results
        if ( start > -1 ) {
            dotConnect.setStartRow( start );
        }
        if ( limit > -1 ) {
            dotConnect.setMaxRows( limit );
        }
        ArrayList<Map<String, Object>> results = dotConnect.loadResults();

        ArrayList<String> ids = new ArrayList<String>();
        for ( Map<String, Object> hash : results ) {
            String userId = (String) hash.get( "userid" );
            ids.add( userId );
        }
        return ids;
    }

	@Override
	public void delete(User userToDelete) throws DotDataException {
		uc.remove(userToDelete.getUserId());
		try {
			UserLocalManagerUtil.deleteUser(userToDelete.getUserId());
		} catch (Exception e) {
			throw new DotDataException(e.getMessage(), e);
		}
	}

	@Override
	protected void saveAddress(User user, Address ad)
			throws DotDataException {
		try 
		{
			if(UtilMethods.isSet(ad.getAddressId())) {
				AddressLocalManagerUtil.updateAddress(ad.getAddressId(), ad.getDescription(), ad.getStreet1(), ad.getStreet2(), ad.getCity(), ad.getState(), 
						ad.getZip(), ad.getCountry(), ad.getPhone(), ad.getFax(), ad.getCell());
			} else {
				Address newAddress = AddressLocalManagerUtil.addAddress(user.getUserId(), user.getClass().getName(), user.getUserId(), ad.getDescription(), ad.getStreet1(), 
						ad.getStreet2(), ad.getCity(), ad.getState(), ad.getZip(), ad.getCountry(), ad.getPhone(), ad.getFax(), ad.getCell());
				ad.setAddressId(newAddress.getAddressId());
			}
			ad.setNew(false);
			
		} catch (PortalException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		} catch (SystemException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		}
	}

	@Override
	protected Address loadAddressById(String addressId) throws DotDataException {
		try {
			return PublicAddressFactory.getAddressById(addressId);
		} catch (SystemException e) {
			throw new DotDataException(e.getMessage(), e);
		}
	}

	@Override
	protected void deleteAddress(Address ad) {
		PublicAddressFactory.delete(ad);
	}

	@Override
	protected List<Address> loadUserAddresses(User user)
			throws DotDataException {
		try {
			return PublicAddressFactory.getAddressesByUserId(user.getUserId());
		} catch (SystemException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		}
	}

}
