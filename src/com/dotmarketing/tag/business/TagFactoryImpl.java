package com.dotmarketing.tag.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.tag.model.TagInode;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.ActionException;

import java.util.*;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

/**
 * @author Jonathan Gamba
 *         Date: 1/28/16
 */
public class TagFactoryImpl implements TagFactory {

    private PermissionAPI permissionAPI = APILocator.getPermissionAPI();
    private TagCache tagCache;

    public TagFactoryImpl () {
        tagCache = CacheLocator.getTagCache();
    }

    /**
     * @param permissionAPIRef the permissionAPI to set
     */
    public void setPermissionAPI ( PermissionAPI permissionAPIRef ) {
        permissionAPI = permissionAPIRef;
    }

    /**
     * Get a list of all the tags created
     *
     * @return list of all tags created
     */
    public java.util.List<Tag> getAllTags () throws DotDataException, DotCacheException {

        HibernateUtil dh = new HibernateUtil(Tag.class);
        dh.setQuery("from tag in class com.dotmarketing.tag.model.Tag");

        //Search
        List<Tag> tags = dh.list();

        //And add the results to the cache
        for ( Tag tag : tags ) {
            if ( tagCache.get(tag.getTagId()) == null ) {
                tagCache.put(tag);
            }
        }

        return tags;
    }

    /**
     * Gets a Tag by name
     *
     * @param name name of the tag to get
     * @return tag
     */
    public java.util.List<Tag> getTagByName ( String name ) throws DotDataException, DotCacheException {

        List<Tag> tags = tagCache.getByName(name);
        if ( tags == null ) {

            name = escapeSingleQuote(name);

            HibernateUtil dh = new HibernateUtil(Tag.class);
            dh.setQuery("from tag in class com.dotmarketing.tag.model.Tag where tagname = ?");
            dh.setParam(name.toLowerCase());

            //Search
            tags = dh.list();

            //And add the results to the cache
            for ( Tag tag : tags ) {
                if ( tagCache.get(tag.getTagId()) == null ) {
                    tagCache.put(tag);
                }
            }
            tagCache.putForName(name, tags);
        }

        return tags;
    }

    public java.util.List<Tag> getTagByHost ( String hostId ) throws DotDataException, DotCacheException {

        List<Tag> tags = tagCache.getByHost(hostId);
        if ( tags == null ) {

            hostId = escapeSingleQuote(hostId);

            HibernateUtil dh = new HibernateUtil(Tag.class);
            dh.setQuery("from tag in class com.dotmarketing.tag.model.Tag where host_id = ?");
            dh.setParam(hostId);

            //Search
            tags = dh.list();

            //And add the results to the cache
            for ( Tag tag : tags ) {
                if ( tagCache.get(tag.getTagId()) == null ) {
                    tagCache.put(tag);
                }
            }
            tagCache.putForHost(hostId, tags);
        }

        return tags;
    }

    public List<Tag> getTagsLikeNameAndHostIncludingSystemHost ( String name, String hostId ) throws DotDataException, DotCacheException {

        name = escapeSingleQuote(name);

        HibernateUtil dh = new HibernateUtil(Tag.class);
        dh.setQuery("from tag in class com.dotmarketing.tag.model.Tag where tagname like ? and (host_id like ? OR host_id like ?)");
        dh.setParam(name.toLowerCase() + "%");

        //search global
        dh.setParam(hostId);
        dh.setParam(Host.SYSTEM_HOST);

        //Search
        List<Tag> tags = dh.list();

        //And add the results to the cache
        for ( Tag tag : tags ) {
            if ( tagCache.get(tag.getTagId()) == null ) {
                tagCache.put(tag);
            }
        }

        return tags;
    }

    public Tag getTagByNameAndHost ( String name, String hostId ) throws DotDataException, DotCacheException {

        Tag tag = tagCache.get(name, hostId);
        if ( tag == null ) {

            name = escapeSingleQuote(name);

            HibernateUtil dh = new HibernateUtil(Tag.class);
            dh.setQuery("from tag in class com.dotmarketing.tag.model.Tag where tagname = ? and host_id like ?");
            dh.setParam(name.toLowerCase());

            //search global
            dh.setParam(hostId);
            tag = (Tag) dh.load();

            //And add the result to the cache
            tagCache.put(tag);
        }

        return tag;
    }

    /**
     * Gets a Tag by a tagId retrieved from a TagInode.
     *
     * @param tagId the tag id to get
     * @return tag
     * @throws DotHibernateException
     */
    public Tag getTagByTagId ( String tagId ) throws DotDataException, DotCacheException {

        Tag tag = tagCache.get(tagId);
        if ( tag == null ) {

            HibernateUtil dh = new HibernateUtil(Tag.class);

            dh.setQuery("from tag in class com.dotmarketing.tag.model.Tag where tag_id = ?");
            dh.setParam(tagId);

            tag = (Tag) dh.load();

            //And add the result to the cache
            tagCache.put(tag);
        }

        return tag;
    }

    /**
     * Gets all the tag created by an user
     *
     * @param userId id of the user
     * @return a list of all the tags created
     * FIXME: Needs cache
     */
    public java.util.List<Tag> getTagByUser ( String userId ) {
        try {
            HibernateUtil dh = new HibernateUtil(Tag.class);
            dh.setQuery("from tag in class com.dotmarketing.tag.model.Tag where user_id = ?");
            dh.setParam(userId);

            return dh.list();
        } catch ( Exception e ) {
            Logger.warn(Tag.class, "getTagByUser failed:" + e, e);
        }
        return new java.util.ArrayList<>();
    }

    /**
     * Gets all tags filtered by tag name and/or host name
     *
     * @param tagName          tag name
     * @param hostFilter
     * @param globalTagsFilter
     * @param sort
     * @param start
     * @param count
     * @return a list of tags filtered by tag name or host name
     * FIXME: Needs cache???
     */
    public java.util.List<Tag> getFilteredTags ( String tagName, String hostFilter, boolean globalTagsFilter, String sort, int start, int count ) {
        try {

            sort = SQLUtil.sanitizeSortBy(sort);

            HibernateUtil dh = new HibernateUtil(Tag.class);
            List list;

            Host host = null;
            try {
                host = APILocator.getHostAPI().find(hostFilter, APILocator.getUserAPI().getSystemUser(), true);
            } catch ( Exception e ) {
                Logger.warn(Tag.class, "Unable to get host according to search criteria - hostId = " + hostFilter);
            }
            if ( UtilMethods.isSet(host) ) {

                String sortStr = "";
                if ( UtilMethods.isSet(sort) ) {
                    String sortDirection = sort.startsWith("-") ? "desc" : "asc";
                    sort = sort.startsWith("-") ? sort.substring(1, sort.length()) : sort;

                    if ( sort.equalsIgnoreCase("hostname") ) sort = "host_id";

                    sortStr = " order by " + sort + " " + sortDirection;
                } else {
                    sortStr = "order by tagname";
                }

                //if tag name and host name are set as filters
                //search by name, global tags and current host.
                if ( UtilMethods.isSet(tagName) && globalTagsFilter ) {
                    dh.setQuery("from tag in class com.dotmarketing.tag.model.Tag where tagname like ? and (host_id = ? or host_id = ?) " + sortStr);
                    dh.setParam("%" + tagName.toLowerCase() + "%");
                    try {
                        dh.setParam(host.getMap().get("tagStorage").toString());
                    } catch ( NullPointerException e ) {
                        dh.setParam(Host.SYSTEM_HOST);
                    }
                    dh.setParam(Host.SYSTEM_HOST);
                }
                //if global host is not set as unique filter but tag name is set, search in current host
                else if ( UtilMethods.isSet(tagName) && !globalTagsFilter ) {
                    dh.setQuery("from tag in class com.dotmarketing.tag.model.Tag where tagname like ? and (host_id = ?) " + sortStr);
                    dh.setParam("%" + tagName.toLowerCase() + "%");
                    try {
                        dh.setParam(host.getMap().get("tagStorage").toString());
                    } catch ( NullPointerException e ) {
                        dh.setParam(Host.SYSTEM_HOST);
                    }
                } else if ( !UtilMethods.isSet(tagName) && globalTagsFilter ) {
                    //check if tag name is not set and if should display global tags
                    //it will check all global tags and current host tags.
                    dh.setQuery("from tag in class com.dotmarketing.tag.model.Tag where " +
                            "(host_id = ? or host_id = ? ) " + sortStr);

                    try {
                        dh.setParam(host.getMap().get("tagStorage").toString());
                    } catch ( NullPointerException e ) {
                        dh.setParam(Host.SYSTEM_HOST);
                    }
                    dh.setParam(Host.SYSTEM_HOST);
                } else {
                    //check all current host tags.
                    String sql = "from tag in class com.dotmarketing.tag.model.Tag ";

                    Object tagStorage = host.getMap().get("tagStorage");

                    if ( UtilMethods.isSet(tagStorage) ) {
                        sql = sql + "where ( host_id = ? ) " + sortStr;
                        dh.setQuery(sql);
                        dh.setParam(tagStorage.toString());
                    } else {
                        dh.setQuery(sql);
                    }
                }

                dh.setFirstResult(start);

                list = dh.list();

                return list;
            }
        } catch ( Exception e ) {
            Logger.warn(Tag.class, "getFilteredTags failed:" + e, e);
        }
        return new java.util.ArrayList<>();
    }

    /**
     * FIXME: Needs cache
     * @param tagInode
     * @param tagId
     * @throws DotDataException
     * @throws DotCacheException
     */
    public void updateTagInode ( TagInode tagInode, String tagId ) throws DotDataException, DotCacheException {
        tagInode.setTagId(tagId);
        tagInode.setModDate(new Date());
        HibernateUtil.saveOrUpdate(tagInode);
    }

    /**
     * FIXME: Needs cache
     * @param tag
     * @return
     * @throws Exception
     */
    public Tag saveTag ( Tag tag ) throws Exception {

        //FIXME: Remove the lists of caches of find where to add it!!???
        //FIXME: Remove the lists of caches of find where to add it!!???
        //FIXME: Remove the lists of caches of find where to add it!!???
        //FIXME: Remove the lists of caches of find where to add it!!???

        HibernateUtil.save(tag);
        return tag;
    }

    /**
     * FIXME: Needs cache
     * @param tagInode
     * @return
     * @throws Exception
     */
    public TagInode saveTagInode ( TagInode tagInode ) throws Exception {
        HibernateUtil.save(tagInode);
        return tagInode;
    }

    public void updateTag ( Tag tag ) throws DotDataException, DotCacheException {

        //First lets clean up the cache
        tagCache.remove(tag);

        HibernateUtil.saveOrUpdate(tag);
    }

    public void deleteTag ( Tag tag ) throws DotDataException, DotCacheException {

        //First lets clean up the cache
        tagCache.remove(tag);

        HibernateUtil.delete(tag);
    }

    /**
     * Gets all the tags created, with the respective owner and permission information
     *
     * @param userId id of the user that searches the tag
     * @return a complete list of all the tags, with the owner information and the respective permission
     * information
     *
     * FIXME: Needs cache
     */
    public List getAllTag ( String userId ) {
        try {
            User searcherUser = APILocator.getUserAPI().loadUserById(userId, APILocator.getUserAPI().getSystemUser(), false);

            HibernateUtil dh = new HibernateUtil();
            StringBuffer sb = new StringBuffer();
            sb.append("select Tag.*, User_.firstName, User_.lastName from Tag, User_ ");
            sb.append("where Tag.user_id = User_.userid ");
            sb.append("order by Tag.user_id");
            dh.setQuery(sb.toString());

            List allTags = dh.list();

            java.util.List matchesArray = new ArrayList();
            Iterator it = allTags.iterator();
            for ( int i = 0; it.hasNext(); i++ ) {
                User user = null;

                Map map = (Map) it.next();

                String user_Id = (String) map.get("user_id");
                String tagName = (String) map.get("tagname");
                String firstName = (String) map.get("firstname");
                String lastName = (String) map.get("lastname");
                user = APILocator.getUserAPI().loadUserById(user_Id, APILocator.getUserAPI().getSystemUser(), false);
                UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user, APILocator.getUserAPI().getSystemUser(), false);

                String[] match = new String[6];
                match[0] = (user_Id == null) ? "" : user_Id;
                match[1] = (tagName == null) ? "" : tagName;
                match[2] = (firstName == null) ? "" : firstName;
                match[3] = (lastName == null) ? "" : lastName;

                // adding read permission
                try {
                    _checkUserPermissions(userProxy, searcherUser, PERMISSION_READ);
                    match[4] = "true";
                } catch ( ActionException ae ) {
                    match[4] = "false";
                }

                // adding write permission
                try {
                    _checkUserPermissions(userProxy, searcherUser, PERMISSION_WRITE);
                    match[5] = "true";
                } catch ( ActionException ae ) {
                    match[5] = "false";
                }
                matchesArray.add(match);
            }

            return matchesArray;
        } catch ( Exception e ) {
            Logger.error(e, "Error retrieving tags");
        }

        return new ArrayList();
    }

    /**
     * Gets all tags associated to an object
     *
     * @param inode inode of the object tagged
     * @return list of all the TagInode where the tags are associated to the object
     * FIXME: Needs cache
     */
    public List<TagInode> getTagInodeByInode ( String inode ) {
        try {
            HibernateUtil dh = new HibernateUtil(TagInode.class);
            dh.setQuery("from tag_inode in class com.dotmarketing.tag.model.TagInode where inode = ?");
            dh.setParam(inode);

            return dh.list();
        } catch ( Exception e ) {
            Logger.warn(Tag.class, "getTagInodeByInode failed:" + e, e);
        }
        return new ArrayList<>();
    }

    /**
     * Gets all tags associated to an object
     *
     * @param tagId tagId of the object tagged
     * @return list of all the TagInode where the tags are associated to the object
     * FIXME: Needs cache
     */
    public List<TagInode> getTagInodeByTagId ( String tagId ) {
        try {
            HibernateUtil dh = new HibernateUtil(TagInode.class);
            dh.setQuery("from tag_inode in class com.dotmarketing.tag.model.TagInode where tag_id = ?");
            dh.setParam(tagId);

            return dh.list();

        } catch ( Exception e ) {
            Logger.warn(Tag.class, "getTagInodeByTagId failed:" + e, e);
        }
        return new ArrayList<>();
    }

    /**
     * Gets a tagInode by name and inode
     *
     * @param tagId id of the tag
     * @param inode inode of the object tagged
     * @return the tagInode
     * FIXME: Needs cache
     */
    public TagInode getTagInode ( String tagId, String inode ) throws DotHibernateException {
        // getting the tag inode record
        HibernateUtil dh = new HibernateUtil(TagInode.class);
        dh.setQuery("from tag_inode in class com.dotmarketing.tag.model.TagInode where tag_id = ? and inode = ?");
        dh.setParam(tagId);
        dh.setParam(inode);

        TagInode tagInode;
        try {
            tagInode = (TagInode) dh.load();
        } catch ( Exception ex ) {
            tagInode = new TagInode();
        }
        return tagInode;
    }

    /**
     * Deletes a TagInode
     *
     * @param tagInode TagInode to delete
     */
    public void deleteTagInode ( TagInode tagInode ) throws DotHibernateException {
        HibernateUtil.delete(tagInode);
    }

    /**
     * Escape a single quote
     *
     * @param tagName string with single quotes
     * @return single quote string escaped
     */
    public String escapeSingleQuote ( String tagName ) {
        return tagName.replace("'", "''");
    }

    /**
     * Gets all the tags given a user List
     *
     * @param userIds the user id's associated with the tags
     * @return a complete list of all the tags, with the owner information and the respective permission
     * information
     * FIXME: Needs cache???
     */
    @SuppressWarnings ( "unchecked" )
    public List<Tag> getAllTagsForUsers ( List<String> userIds ) {

        List<Tag> tags = new ArrayList<>();

        try {
            if ( userIds != null && !userIds.isEmpty() ) {
                DotConnect dc = new DotConnect();
                dc.setSQL("select tagname, user_id from tag where user_id is not null");

                //Gets all the tags from DB that are not null.
                List<Map<String, Object>> results = (ArrayList<Map<String, Object>>) dc.loadResults();

                //Checks each of the tag to see if match any of the users in the list.
                for ( int i = 0; i < results.size(); i++ ) {
                    Map<String, Object> hash = results.get(i);

                    if ( !hash.isEmpty() ) {
                        String tagUserID = (String) hash.get("user_id");
                        String tagName = (String) hash.get("tagname");

                        //Creates the tag only if the tagUserID is in the userIds list.
                        if ( belongsToUser(userIds, tagUserID, tagName) ) {
                            Tag tag = new Tag();
                            tag.setTagName(tagName);
                            tag.setUserId(tagUserID);
                            tags.add(tag);
                        }
                    }
                }
            }
        } catch ( Exception e ) {
            Logger.warn(TagFactory.class, "getAllTagsForUsers failed:" + e, e);
        }

        return tags;
    }

    /**
     * Checks is the tagUserID belongs to one of the users in List userIds
     *
     * @param userIds   List with all the users
     * @param tagUserID
     * @param tagName
     * @return
     */
    private boolean belongsToUser ( List<String> userIds, String tagUserID, String tagName ) {
        if ( UtilMethods.isSet(tagUserID) && UtilMethods.isSet(tagName) ) {
            //The tagUserID from DB can contains several user IDs separated by commas.
            String[] tagUserIds = tagUserID.split(",");

            //Have to check under each tagUserIds
            for ( String tagUserIDAux : tagUserIds ) {

                //Have to check ALSO under each userIds
                for ( String userID : userIds ) {

                    if ( userID.equals(tagUserIDAux) ) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * FIXME: Needs cache
     * @param inode
     * @return
     */
    @Override
    public List<Tag> getTagsByInode ( String inode ) {
        try {
            HibernateUtil dh = new HibernateUtil(Tag.class);
            dh.setQuery("select tag from com.dotmarketing.tag.model.TagInode tagi, com.dotmarketing.tag.model.Tag tag " +
                    " where tagi.tagId=tag.tagId and tagi.inode = ?");
            dh.setParam(inode);

            return dh.list();

        } catch ( Exception e ) {
            Logger.warn(Tag.class, "getTagInodeByInode failed:" + e, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks the permission access of an user over an object
     *
     * @param webAsset   object to validates access
     * @param user       user to validate access
     * @param permission read or write permission to validates
     * @throws ActionException
     * @throws DotDataException
     */
    private void _checkUserPermissions ( Inode webAsset, User user, int permission ) throws ActionException, DotDataException {
        // Checking permissions
        if ( !InodeUtils.isSet(webAsset.getInode()) )
            return;

        if ( !permissionAPI.doesUserHavePermission(webAsset, permission,
                user) ) {
            throw new ActionException(WebKeys.USER_PERMISSIONS_EXCEPTION);
        }
    }

}