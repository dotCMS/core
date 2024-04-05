package com.dotmarketing.business;

import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.util.transform.TransformerLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.StringPool;
import org.apache.logging.log4j.util.Strings;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Implementation class for the {@link UserFactory} class.
 *
 * @author Nollymar Longa
 * @since Mar 16th, 2021
 */
public class UserFactoryImpl implements UserFactory {

    private static final String USERID_COLUMN = "userid";
    private static final String AND_DELETE_IN_PROGRESS = " AND delete_in_progress = ";

    private final UserCache userCache;

    private static final ObjectMapper mapper = DotObjectMapperProvider.getInstance()
            .getDefaultObjectMapper();

    /**
     * Default class constructor.
     */
    public UserFactoryImpl() {
        userCache = CacheLocator.getUserCache();
    }

    @Override
    public User loadDefaultUser(final Company company) throws DotDataException, NoSuchUserException {
        try {
            return loadUserById(company.getCompanyId() + "." + User.DEFAULT);
        } catch (NoSuchUserException e) {
            Logger.debug(this, "Default user not found attempting to create by updating company");
            try {
                return save(getDefaultUser(company));
            } catch (Exception e1) {
                throw new DotDataException("Unable to create default user from company", e1);
            }
        }
    }

    /**
     * Creates a new instance of the default User
     * @param company
     * @return
     */
    private User getDefaultUser(Company company) {
        final User defaultUser = new User();
        final Date now = new Date();
        defaultUser.setUserId(company.getCompanyId() + "." + User.DEFAULT);
        defaultUser.setCompanyId(User.DEFAULT);
        defaultUser.setCreateDate(now);
        defaultUser.setPassword("password");
        defaultUser.setFirstName(StringPool.BLANK);
        defaultUser.setMiddleName(StringPool.BLANK);
        defaultUser.setLastName(StringPool.BLANK);
        defaultUser.setMale(true);
        defaultUser.setBirthday(now);
        defaultUser.setEmailAddress(User.DEFAULT + "@" + company.getMx());

        defaultUser.setLanguageId(null);
        defaultUser.setTimeZoneId(DateUtil.UTC);
        defaultUser.setDottedSkins(false);
        defaultUser.setRoundedSkins(false);
        defaultUser.setGreeting("Welcome!");
        defaultUser.setResolution(
                PropsUtil.get(PropsUtil.DEFAULT_GUEST_LAYOUT_RESOLUTION));
        defaultUser.setRefreshRate(
                PropsUtil.get(PropsUtil.DEFAULT_USER_LAYOUT_REFRESH_RATE));
        defaultUser.setLoginDate(now);
        defaultUser.setFailedLoginAttempts(0);
        defaultUser.setAgreedToTermsOfUse(false);
        defaultUser.setActive(true);
        return defaultUser;
    }

    @Override
    public User loadUserById(final String userId) throws DotDataException, NoSuchUserException {
        User user = userCache.get(userId);
        if (!UtilMethods.isSet(user)) {

            if (user == null) {
                final DotConnect dc = new DotConnect();
                dc.setSQL("select * from user_ where userid=?");
                dc.addParam(userId.trim().toLowerCase());
                List<Map<String, Object>> list = dc.loadObjectResults();
                if(list.isEmpty()) {
                    throw new NoSuchUserException(userId);
                }else{
                    user = TransformerLocator.createUserTransformer(list).findFirst();
                    userCache.add(userId, user);
                }
            }
        }
        return user;
    }

    @Override
    public User loadByUserEmail(final String email)
            throws DotDataException {
        final DotConnect dotConnect = new DotConnect();

        final StringBuffer query = new StringBuffer();
        query.append(
                "select * FROM user_ WHERE ");
        query.append("emailAddress = ?");
        query.append(AND_DELETE_IN_PROGRESS);
        query.append(DbConnectionFactory.getDBFalse());
        query.append(" ORDER BY ");
        query.append("firstName ASC").append(", ");
        query.append("middleName ASC").append(", ");
        query.append("lastName ASC");

        dotConnect.setSQL(query.toString());
        dotConnect.addParam(email.trim().toLowerCase());

        return UtilMethods.isSet(dotConnect.loadObjectResults()) ? TransformerLocator
                .createUserTransformer(dotConnect.loadObjectResults()).findFirst() : null;

    }

    @Override
    public List<User> findAllUsers(final int begin, final int end) throws DotDataException {
        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL("select * from user_ where companyid <> '" + User.DEFAULT
                + "' " + AND_DELETE_IN_PROGRESS + DbConnectionFactory.getDBFalse()
                + " order by firstname, lastname");
        dotConnect.setStartRow(begin);
        dotConnect.setMaxRows(end -  begin);
        return TransformerLocator.createUserTransformer(dotConnect.loadObjectResults()).asList();

    }

    @Override
    public long getCountUsersByName(String filter, final List<Role> roles) {
        filter = SQLUtil.sanitizeParameter(filter);
        final DotConnect dotConnect = new DotConnect();
        final boolean isFilteredByName = UtilMethods.isSet(filter);
        filter = (isFilteredByName ? filter : Strings.EMPTY);
        final StringBuilder baseSql = new StringBuilder(
                "select count(*) as count from user_ where companyid <> ? and userid <> 'system' ");
        if (UtilMethods.isSet(roles)) {
            final String joinedRoleKeys = roles.stream().map(Role::getRoleKey)
                    .map(s -> String.format("'%s'", s)).collect(Collectors.joining(","));
            final String backendRoleFilter = String
                    .format(" and exists ( select ur.user_id from users_cms_roles ur join cms_role r on ur.role_id = r.id where r.role_key in (%s) and ur.user_id = user_.userId )",
                            joinedRoleKeys);
            baseSql.append(backendRoleFilter);
        }

        final String userFullName = DotConnect.concat(new String[]{"firstname", "' '", "lastname"});

        if (isFilteredByName) {
            baseSql.append(" and lower(");
            baseSql.append(userFullName);
            baseSql.append(") like ?");
        }

        baseSql.append(AND_DELETE_IN_PROGRESS);
        baseSql.append(DbConnectionFactory.getDBFalse());

        final String sql = baseSql.toString();
        dotConnect.setSQL(sql);
        Logger.debug(UserFactoryImpl.class,
                "::getCountUsersByName -> query: " + dotConnect.getSQL());

        dotConnect.addParam(User.DEFAULT);
        if (isFilteredByName) {
            dotConnect.addParam("%" + filter.toLowerCase() + "%");
        }

        return dotConnect.getInt("count");
    }

    @Override
    public List<User> getUsersByName(final String filter, final List<Role> roles, final int start,
            final int limit) throws DotDataException {
        return getUsersByName(filter, roles, start, limit, new UserAPI.FilteringParams.Builder().build());
    }

    @Override
    public List<User> getUsersByName(final String filter, final List<Role> roles, final int start,
                                     final int limit, final UserAPI.FilteringParams filteringParams) throws DotDataException {
        final StringBuilder baseSql = new StringBuilder("select user_.userId from user_ where ");
        baseSql.append(!filteringParams.includeDefaultUser() ? "companyid <> ? AND " : StringPool.BLANK);
        baseSql.append(" userid <> 'system' ");
        baseSql.append(!filteringParams.includeAnonymousUser() ? " AND userid <> 'anonymous' " : StringPool.BLANK);

        if (UtilMethods.isSet(roles)) {
            final String joinedRoleKeys =
                    roles.stream().map(Role::getRoleKey).map(s -> String.format("'%s'", s)).collect(Collectors.joining(StringPool.COMMA));
            final String backendRoleFilter = String.format(" and exists ( select ur.user_id from users_cms_roles ur " +
                                                                   "join cms_role r on ur.role_id = r.id where r" +
                                                                   ".role_key in (%s) and ur.user_id = user_.userId )"
                    , joinedRoleKeys);
            baseSql.append(backendRoleFilter);
        }
        final String userFullName = DotConnect.concat(new String[]{"firstname", "' '", "lastname"});
        final String sanitizeFilter = SQLUtil.sanitizeParameter(filter);
        boolean isFilteredByName = UtilMethods.isSet(sanitizeFilter);
        if (isFilteredByName) {
            baseSql.append(" and lower(").append(userFullName).append(") like ?");
        }
        baseSql.append(AND_DELETE_IN_PROGRESS).append(DbConnectionFactory.getDBFalse());

        baseSql.append(" order by ");
        final String sanitizedOrderBy = SQLUtil.sanitizeSortBy(filteringParams.orderBy());
        baseSql.append(UtilMethods.isSet(sanitizedOrderBy) ? sanitizedOrderBy : userFullName);
        baseSql.append(UtilMethods.isSet(filteringParams.orderDirection()) ? filteringParams.orderDirection() : SQLUtil._ASC);

        final String sql = baseSql.toString();
        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL(sql);
        Logger.debug(UserFactoryImpl.class, "::getUsersByName -> query: " + dotConnect.getSQL());
        if (!filteringParams.includeDefaultUser()) {
            dotConnect.addParam(User.DEFAULT);
        }
        if (isFilteredByName) {
            dotConnect.addParam("%" + sanitizeFilter.toLowerCase() + "%");
        }
        if (start > -1) {
            dotConnect.setStartRow(start);
        }
        if (limit > -1) {
            dotConnect.setMaxRows(limit);
        }
        final List<Map<String, Object>> results = dotConnect.loadObjectResults();
        final List<User> users = new ArrayList<>();
        for (final Map<String, Object> userData : results) {
            final String userId = userData.get(USERID_COLUMN).toString();
            final User user = this.loadUserById(userId);
            users.add(user);
            this.userCache.add(user.getUserId(), user);
        }
        return users;
    }

    @Override
    public User save(final User user) throws DotDataException, DuplicateUserException {
        if (user.getUserId() == null) {
            throw new DotRuntimeException("Can't save a user without a userId");
        } else{
            user.setModificationDate(new Date());
            final String emailAddress = user.getEmailAddress();
            if (UtilMethods.isSet(emailAddress)) {
                user.setEmailAddress(emailAddress.trim().toLowerCase());
            }

            try {
                final User oldUser = loadUserById(user.getUserId());
                if (UtilMethods.isSet(oldUser)) {
                    userCache.remove(user.getUserId());
                    return updateUser(user, oldUser);
                }
            }catch(NoSuchUserException e){
                return createUser(user);
            }
        }
        return user;
    }

    private User createUser(final User user) throws DotDataException {

        final DotConnect dotConnect = new DotConnect();

        final StringBuilder query = new StringBuilder();

        query.append("INSERT INTO user_ (")
                .append("userid, companyid, createdate, mod_date, password_, passwordencrypted, ")
                .append("passwordexpirationdate, passwordreset, firstname, middlename, lastname, ")
                .append("nickname, male, birthday, emailaddress, smsid, aimid, icqid, msnid, ymid, ")
                .append("favoriteactivity, favoritebibleverse, favoritefood, favoritemovie, ")
                .append("favoritemusic, languageid, timezoneid, skinid, dottedskins, roundedskins, ")
                .append("greeting, resolution, refreshrate, layoutids, comments, logindate, loginip, ")
                .append("lastlogindate, lastloginip, failedloginattempts, agreedtotermsofuse, active_, ")
                .append("delete_in_progress, delete_date, additional_info) ")
                .append("VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ")
                .append("?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ")
                .append(DbConnectionFactory.isPostgres()? "? ::jsonb)": "?)");;

        dotConnect.setSQL(query.toString());
        dotConnect.addParam(user.getUserId().trim().toLowerCase());

        try {
            setDotConnectParamsForSave(dotConnect, user);
        } catch (JsonProcessingException e) {
            throw new DotDataException(e.getMessage(), e);
        }
        try {
            dotConnect.loadResult();
        } catch (DotDataException e) {
            Logger.debug(this, "An exception occurred while creating a user");
            throw new DotDataException("Creating a user has failed while executing the SQL due to "+ e.getMessage(), e);
        }
        return user;
    }

    private User updateUser(final User user, final User oldUser) throws DotDataException {

        if (!oldUser.getEmailAddress().equals(user.getEmailAddress())) {
            User emailUser = null;
            try {
                emailUser = loadByUserEmail(user.getEmailAddress());
            } catch (Exception e) {
            }
            if (emailUser != null) {
                throw new DuplicateUserException("User already exists with this email");
            }
        }
        user.setModified(true);

        final DotConnect dotConnect = new DotConnect();

        final StringBuilder query = new StringBuilder();
        query.append("update user_ set companyid=?, createdate=?, mod_date=?, password_=?, ")
                .append("passwordencrypted=?, passwordexpirationdate=?, passwordreset=?, firstname=?, ")
                .append("middlename=?, lastname=?, nickname=?, male=?, birthday=?, emailaddress=?, ")
                .append("smsid=?, aimid=?, icqid=?, msnid=?, ymid=?, favoriteactivity=?, ")
                .append("favoritebibleverse=?, favoritefood=?, favoritemovie=?, favoritemusic=?, ")
                .append("languageid=?, timezoneid=?, skinid=?, dottedskins=?, roundedskins=?, ")
                .append("greeting=?, resolution=?, refreshrate=?, layoutids=?, comments=?, ")
                .append("logindate=?, loginip=?, lastlogindate=?, lastloginip=?, ")
                .append("failedloginattempts=?, agreedtotermsofuse=?, active_=?, ")
                .append("delete_in_progress=?, delete_date=?, ")
                .append(DbConnectionFactory.isPostgres()? "additional_info=? ::jsonb ": "additional_info=? ")
                .append("where userid = ?");

        dotConnect.setSQL(query.toString());

        try {
            setDotConnectParamsForSave(dotConnect, user);
        } catch (JsonProcessingException e) {
            throw new DotDataException(e.getMessage(), e);
        }

        dotConnect.addParam(user.getUserId().trim().toLowerCase());
        try {
            dotConnect.loadResult();
        } catch (DotDataException e) {
            Logger.debug(this, "An exception occurred while updating a user");
            throw new DotDataException("Updating a user has failed while executing the SQL due to "+ e.getMessage(), e);
        }

        return user;
    }

    private void setDotConnectParamsForSave(final DotConnect dotConnect, final User user)
            throws JsonProcessingException {

        dotConnect.addParam(user.getCompanyId()).addParam(user.getCreateDate())
                .addParam(user.getModificationDate()).addParam(user.getPassword()).addParam(user.getPasswordEncrypted())
                .addParam(user.getPasswordExpirationDate()).addParam(user.getPasswordReset())
                .addParam(user.getFirstName()).addParam(user.getMiddleName()).addParam(user.getLastName())
                .addParam(user.getNickName()).addParam(user.getMale()).addParam(user.getBirthday())
                .addParam(user.getEmailAddress()).addParam(user.getSmsId()).addParam(user.getAimId())
                .addParam(user.getIcqId()).addParam(user.getMsnId()).addParam(user.getYmId())
                .addParam(user.getFavoriteActivity()).addParam(user.getFavoriteBibleVerse())
                .addParam(user.getFavoriteFood()).addParam(user.getFavoriteMovie())
                .addParam(user.getFavoriteMusic()).addParam(user.getLanguageId()).addParam(user.getTimeZoneId())
                .addParam(user.getSkinId()).addParam(user.getDottedSkins()).addParam(user.getRoundedSkins())
                .addParam(user.getGreeting()).addParam(user.getResolution()).addParam(user.getRefreshRate())
                .addParam(user.getLayoutIds()).addParam(user.getComments()).addParam(user.getLoginDate())
                .addParam(user.getLoginIP()).addParam(user.getLastLoginDate()).addParam(user.getLastLoginIP())
                .addParam(user.getFailedLoginAttempts()).addParam(user.getAgreedToTermsOfUse())
                .addParam(user.getActive()).addParam(user.getDeleteInProgress()).addParam(user.getDeleteDate())
                .addParam(user.getAdditionalInfo()!=null ? mapper.writeValueAsString(user.getAdditionalInfo()): "{}");
    }

    @Override
    public boolean userExistsWithEmail(final String email) throws DotDataException {

        User user;
        try {
            user = loadByUserEmail(email);
        } catch (Exception e) {
            throw new DotDataException(e.getMessage(), e);
        }
        if (UtilMethods.isSet(user)) {
            userCache.add(user.getUserId(), user);
            return true;
        }
        return false;
    }

    @Override
    public long getCountUsersByNameOrEmail(String filter) {
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
        sql.append(DotConnect.concat(new String[]{"lower(firstName)", "' '", "lower(lastName)"}));
        sql.append(" like '%");
        sql.append(filter);
        sql.append("%')");
        sql.append(AND_DELETE_IN_PROGRESS);
        sql.append(DbConnectionFactory.getDBFalse());

        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL(sql.toString());
        return dotConnect.getInt("count");
    }

    @Override
    public List<User> getUsersByNameOrEmail(String filter, int page, final int pageSize)
            throws DotDataException {
        final List users = new ArrayList(pageSize);
        if (page == 0) {
            page = 1;
        }
        final int bottom = ((page - 1) * pageSize);
        final int top = (page * pageSize);
        filter = (UtilMethods.isSet(filter) ? filter.toLowerCase() : "");
        filter = SQLUtil.sanitizeParameter(filter);
        StringBuilder sql = new StringBuilder(
                "select userid from user_ where (lower(userid) like '%");
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
        sql.append(AND_DELETE_IN_PROGRESS);
        sql.append(DbConnectionFactory.getDBFalse());
        sql.append(" order by firstName asc,lastname asc");

        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL(sql.toString());
        dotConnect.setMaxRows(top);
        List results = dotConnect.getResults();

        final int lenght = results.size();
        for (int i = 0; i < lenght; i++) {
            if (i >= bottom) {
                if (i < top) {
                    final Map hash = (HashMap) results.get(i);
                    String userId = (String) hash.get(USERID_COLUMN);
                    users.add(loadUserById(userId));
                } else {
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
    public long getCountUsersByNameOrEmailOrUserID(String filter, boolean includeAnonymous)
            throws DotDataException {
        return getCountUsersByNameOrEmailOrUserID(filter, true, true);
    }


    @Override
    public long getCountUsersByNameOrEmailOrUserID(String filter, boolean includeAnonymous,
            boolean includeDefault) throws DotDataException {
        return getCountUsersByNameOrEmailOrUserID(filter, true, true, null);
    }


    @Override
    public long getCountUsersByNameOrEmailOrUserID(String filter, final boolean includeAnonymous,
            final boolean includeDefault, final String roleId)
            throws DotDataException {
        filter = (UtilMethods.isSet(filter) ? "%" + filter.toLowerCase() + "%" : null);

        final DotConnect dotConnect = new DotConnect();
        final StringBuilder sql = new StringBuilder("select count(*) as count from user_ ");
        if (roleId != null) {
            sql.append(", users_cms_roles ");
        }

        sql.append(" where 1=1 ");
        if (filter != null) {
            sql.append(
                    " AND (lower(userid) like ? or lower(firstName) like ? or lower(lastName) like ? or lower(emailAddress) like ?) ");
        }
        sql.append(" AND userid <> 'system' ");
        sql.append(((!includeAnonymous) ? "AND userid <> 'anonymous'" : " "));
        sql.append(((!includeDefault) ? "AND userid <> 'dotcms.org.default'" : " "));
        sql.append(AND_DELETE_IN_PROGRESS);
        sql.append(DbConnectionFactory.getDBFalse());
        if (roleId != null) {
            sql.append(" AND role_id = ? ");
            sql.append(" AND users_cms_roles.user_id=user_.userid ");

        }

        dotConnect.setSQL(sql.toString());
        if (filter != null) {
            dotConnect.addParam(filter);
            dotConnect.addParam(filter);
            dotConnect.addParam(filter);
            dotConnect.addParam(filter);
        }
        if (roleId != null) {
            dotConnect.addParam(roleId);
        }

        return dotConnect.getInt("count");
    }

    @Override
    public List<User> getUsersByNameOrEmailOrUserID(String filter, int page, int pageSize)
            throws DotDataException {
        return getUsersByNameOrEmailOrUserID(filter, page, pageSize, true);
    }

    @Override
    public List<User> getUsersByNameOrEmailOrUserID(String filter, int page, int pageSize,
            boolean includeAnonymous) throws DotDataException {
        return getUsersByNameOrEmailOrUserID(filter, page, pageSize, includeAnonymous, true);
    }

    @Override
    public List<User> getUsersByNameOrEmailOrUserID(String filter, int page,
            int pageSize, boolean includeAnonymous, boolean includeDefault)
            throws DotDataException {
        return getUsersByNameOrEmailOrUserID(filter, page, pageSize, includeAnonymous,
                includeDefault, null);

    }

    @Override
    public List<User> getUsersByNameOrEmailOrUserID(String filter, int page,
            final int pageSize, final boolean includeAnonymous, final boolean includeDefault, final String roleId)
            throws DotDataException {

        final List<User> users = new ArrayList<>(pageSize);

        if (page == 0) {
            page = 1;
        }
        final int bottom = ((page - 1) * pageSize);
        final int top = (page * pageSize);
        filter = (UtilMethods.isSet(filter) ? "%" + filter.toLowerCase() + "%" : null);

        final DotConnect dotConnect = new DotConnect();
        final StringBuilder sql = new StringBuilder("select userid from user_ ");
        if (roleId != null) {
            sql.append(", users_cms_roles ");
        }

        sql.append(" where 1=1 ");
        if (filter != null) {
            sql.append(
                    " AND (lower(userid) like ? or lower(firstName) like ? or lower(lastName) like ? or lower(emailAddress) like ?) ");
        }
        sql.append(" AND userid <> 'system' ");
        sql.append(((!includeAnonymous) ? " AND userid <> 'anonymous'" : " "));
        sql.append(((!includeDefault) ? " AND userid <> 'dotcms.org.default'" : " "));
        sql.append(AND_DELETE_IN_PROGRESS);
        sql.append(DbConnectionFactory.getDBFalse());
        if (roleId != null) {
            sql.append(" AND role_id = ? ");
            sql.append(" AND users_cms_roles.user_id=user_.userid ");
        }

        sql.append(" order by firstName asc,lastname asc");
        dotConnect.setSQL(sql.toString());
        if (filter != null) {
            dotConnect.addParam(filter);
            dotConnect.addParam(filter);
            dotConnect.addParam(filter);
            dotConnect.addParam(filter);
        }
        if (roleId != null) {
            dotConnect.addParam(roleId);
        }

        dotConnect.setMaxRows(top);

        final List<?> results = dotConnect.loadResults();
        final int lenght = results.size();
        for (int i = 0; i < lenght; i++) {
            if (i >= bottom) {
                if (i < top) {
                    final Map<String, Object> hash = (HashMap) results.get(i);
                    final String userId = (String) hash.get(USERID_COLUMN);
                    users.add(loadUserById(userId));
                } else {
                    break;
                }
            }
        }
        return users;
    }


    @Override
    public List<User> getUnDeletedUsers() throws DotDataException {

        final List<User> users = new ArrayList();
        final Calendar calendar = Calendar.getInstance();
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        //By default, the filter searches by registers whose delete_date value is less equals than 24 hours
        calendar.add(Calendar.HOUR, Config.getIntProperty("CLEAN_UNDELETED_USERS_INTERVAL", -25));

        final StringBuilder sql = new StringBuilder("select userid from user_ where ");

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

        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL(sql.toString());

        final List results = dotConnect.loadResults();

        for (int i = 0; i < results.size(); i++) {
            HashMap hash = (HashMap) results.get(i);
            String userId = (String) hash.get(USERID_COLUMN);
            users.add(loadUserById(userId));
        }
        return users;
    }

    @Override
    public List<String> getUsersIdsByCreationDate(final Date filterDate, int start, int limit)
            throws DotDataException {

        final DotConnect dotConnect = new DotConnect();
        //Build the sql query
        final StringBuffer
                query =
                new StringBuffer(
                        "SELECT user_.userId FROM user_ WHERE companyid <> ? AND userid <> 'system' ");
        if (UtilMethods.isSet(filterDate)) {
            query.append(" AND createdate >= ?");
        }

        query.append(AND_DELETE_IN_PROGRESS);
        query.append(DbConnectionFactory.getDBFalse());

        query.append(" ORDER BY firstName ASC, lastname ASC");
        dotConnect.setSQL(query.toString());
        Logger.debug(UserFactoryImpl.class,
                "::getUsersByCreationDate -> query: " + dotConnect.getSQL());

        //Add the required params
        dotConnect.addParam(User.DEFAULT);
        if (UtilMethods.isSet(filterDate)) {
            dotConnect.addParam(filterDate);
        }

        //Load the results
        if (start > -1) {
            dotConnect.setStartRow(start);
        }
        if (limit > -1) {
            dotConnect.setMaxRows(limit);
        }

        final List<Map<String, Object>> results = dotConnect.loadResults();

        final List<String> ids = new ArrayList<>();
        for (final Map<String, Object> hash : results) {
            final String userId = (String) hash.get(USERID_COLUMN);
            ids.add(userId);
        }
        return ids;
    }

    @Override
    public String getUserIdByToken(final String token) throws DotDataException {

        final DotConnect dotConnect = new DotConnect();
        //Build the sql query
        final StringBuffer
                query =
                new StringBuffer(
                        "SELECT user_.userId FROM user_ WHERE icqid = ? ");

        dotConnect.setSQL(query.toString());

        //Add the required params
        dotConnect.addParam(token);


        final List<Map<String, Object>> results = dotConnect.loadResults();

        if(results.isEmpty()) {
            return null;
        }else {
            return (String) results.get(0).get(USERID_COLUMN);
        }
    }

    @Override
    public void delete(final User userToDelete) throws DotDataException {
        userCache.remove(userToDelete.getUserId());
        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL("delete from user_ where userid = ?");
        dotConnect.addParam(userToDelete.getUserId());
        dotConnect.loadResult();
    }
}
