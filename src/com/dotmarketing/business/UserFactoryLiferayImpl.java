/**
 * 
 */
package com.dotmarketing.business;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotmarketing.cms.factories.PublicAddressFactory;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.counter.ejb.CounterManagerUtil;
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

/**
 * @author Jason Tesser
 *
 */
public class UserFactoryLiferayImpl extends UserFactory {

	private UserCache uc;
	
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
		boolean autoID = false;
		User defaultUser = null;
		if(!UtilMethods.isSet(userId)){
			autoID = true;
			try {
				boolean more = false; 
				do {
					userId = companyId + "." +
					Long.toString(CounterManagerUtil.increment(User.class.getName() + "." + companyId));
					try {
						User user = UserLocalManagerUtil.getUserById(companyId, userId);
						if(!user.isNew())
							more=true;
						else
							more=false;
						
					} catch (PortalException e) {
						more=false;
					}
					
				}while(more);
			} catch (Exception e) {
				throw new DotDataException("Can't get a counter");
			}
		}
		
		try {
			defaultUser = APILocator.getUserAPI().getDefaultUser();
		} catch (Exception e) {
			throw new DotDataException("Can't get default user");
		}		

		if(!UtilMethods.isSet(email)){
			email = userId + "@fakedotcms.org";
		}
		
		User user;
		try {
			user = UserLocalManagerUtil.addUser(companyId, autoID, userId, true, null, null, false, userId, null, userId, null, true, null, email, defaultUser.getLocale());
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
	
	/* (non-Javadoc)
	 * @see com.dotmarketing.business.UserFactory#loadUserById(java.lang.String, com.liferay.portal.model.User, boolean)
	 */
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
	public List<User> getUsersByName(String filter, int start, int limit) throws DotDataException {
		DotConnect dotConnect = new DotConnect();
		boolean isFilteredByName = UtilMethods.isSet(filter);
		filter = (isFilteredByName ? filter : "");
		StringBuffer baseSql = new StringBuffer("select user_.userId from user_ where companyid = ? and userid <> 'system' ");
		String userFullName = dotConnect.concat( new String[]{ "firstname", "' '", "lastname" } );

		if( isFilteredByName ) {
			baseSql.append(" and lower(");
			baseSql.append(userFullName);
			baseSql.append(") like ?");
		}
		baseSql.append(" order by ");
		baseSql.append(userFullName);

		String sql = baseSql.toString();
		dotConnect.setSQL(sql);
		Logger.debug( UserFactoryLiferayImpl.class,"::getUsersByName -> query: " + dotConnect.getSQL() );

		dotConnect.addParam(PublicCompanyFactory.getDefaultCompanyId());
		if(isFilteredByName) {
			dotConnect.addParam("%"+filter.toLowerCase()+"%");
		}
		
		if(start > -1)
			dotConnect.setStartRow(start);
		if(limit > -1)
			dotConnect.setMaxRows(limit);

		ArrayList<Map<String, Object>> results = dotConnect.loadResults();
		
		// Since limit is a small number, convert each row to appropriate entity
		ArrayList<User> users = new ArrayList<User>();

		int length = results.size();
		for(int i = 0;i < length; i++ ) {
			Map<String, Object> hash = (Map<String, Object>) results.get(i);
			String userId = (String) hash.get("userid");
			User u = loadUserById(userId);
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
//			uc.add(u.getUserId(), u);
			return u;
		} catch (PortalException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException("saving a user failed", e);
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
		String sql = "select count(*) as count from user_ where " +
				"lower(firstName) like '%" + filter + "%' or lower(lastName) like '%" + filter +"%' or " +
				"lower(emailAddress) like '%" + filter + "%' or " + 
				DotConnect.concat(new String[] { "lower(firstName)", "' '", "lower(lastName)" }) + " like '%" + filter +"%'";
		DotConnect dotConnect = new DotConnect();
		dotConnect.setSQL(sql);
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
		    		
		String sql = "select userid from user_ where (lower(firstName) like '%" + filter + "%' or lower(lastName) like '%" + filter +"%' or lower(emailAddress) like '%" + filter + "%' " +
				" or " + DotConnect.concat(new String[] { "lower(firstName)", "' '", "lower(lastName)" }) + " like '%" + filter +"%') AND userid <> 'system' " +
				"order by firstName asc,lastname asc";
		DotConnect dotConnect = new DotConnect();
		dotConnect.setSQL(sql);
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
	public Map<String, Object> getUsersAnRolesByName(String filter, int start, int limit) throws DotDataException {
		filter = (UtilMethods.isSet(filter) ? filter : "");
		DotConnect dotConnect = new DotConnect();
	
		String baseSql = " (select distinct 0 as isuser, 'role' as type, " + dotConnect.concat( new String[]{"role_.roleId", "''"}) + " as id, role_.name as name, 'role' as emailaddress " +
		"from role_ where companyid = '" + PublicCompanyFactory.getDefaultCompanyId() + "' " +
		"union " +
		"select distinct 1 as isuser, 'user' as type, user_.userId as id, " + dotConnect.concat( new String[]{"user_.firstName", "' '", "user_.lastName" } ) + " as name, user_.emailaddress as emailaddress " +
		"from user_ where companyid = '" + PublicCompanyFactory.getDefaultCompanyId() + "' " +
		"order by isuser, name) ";
		
		String sql = "select isuser, id, name, type, emailaddress from " + baseSql + " uar ";
		if(UtilMethods.isSet(filter))
				sql += "where lower(name) like ?";
		dotConnect.setSQL(sql);
		if(UtilMethods.isSet(filter))
			dotConnect.addParam("%"+filter.toLowerCase()+"%");
		
		if(start > -1)
			dotConnect.setStartRow(start);
		if(limit > -1)
			dotConnect.setMaxRows(limit);
		ArrayList<Map<String, Object>> results = dotConnect.getResults();
		
		sql = "select count(*) as total from " + baseSql + " uar ";
		if(UtilMethods.isSet(filter))
				sql += "where lower(name) like ?";
		dotConnect = new DotConnect();
		dotConnect.setSQL(sql);
		if(UtilMethods.isSet(filter))
			dotConnect.addParam("%"+filter.toLowerCase()+"%");
		int total = dotConnect.getInt("total");
		
		HashMap<String, Object> ret = new HashMap<String, Object>();
		ret.put("total", total);
		ret.put("data", results);
		return ret;    			
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
