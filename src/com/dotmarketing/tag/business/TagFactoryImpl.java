package com.dotmarketing.tag.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.tag.model.TagInode;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.ActionException;

import java.util.*;

/**
 * @author Jonathan Gamba
 *         Date: 1/28/16
 */
public class TagFactoryImpl implements TagFactory {

    private static final String TAG_COLUMN_TAG_ID = "tag_id";
    private static final String TAG_COLUMN_TAGNAME = "tagname";
    private static final String TAG_COLUMN_HOST_ID = "host_id";
    private static final String TAG_COLUMN_USER_ID = "user_id";
    private static final String TAG_COLUMN_PERSONA = "persona";
    private static final String TAG_COLUMN_MOD_DATE = "mod_date";

    private static final String TAG_INODE_COLUMN_TAG_ID = "tag_id";
    private static final String TAG_INODE_COLUMN_INODE = "inode";
    private static final String TAG_INODE_COLUMN_MOD_DATE = "mod_date";

    private PermissionAPI permissionAPI = APILocator.getPermissionAPI();
    private TagCache tagCache;
    private TagInodeCache tagInodeCache;

    public TagFactoryImpl () {
        tagCache = CacheLocator.getTagCache();
        tagInodeCache = CacheLocator.getTagInodeCache();
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
    public List<Tag> getAllTags () throws DotDataException {

        //Execute the search
        final DotConnect dc = new DotConnect();
        dc.setSQL("SELECT * FROM tag");

        List<Tag> tags = convertForTags(dc.loadObjectResults());

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
    public List<Tag> getTagsByName ( String name ) throws DotDataException {

        List<Tag> tags = tagCache.getByName(name);
        if ( tags == null ) {

            name = escapeSingleQuote(name);

            //Execute the search
            final DotConnect dc = new DotConnect();
            dc.setSQL("SELECT * FROM tag WHERE tagname = ?");
            dc.addParam(name.toLowerCase());

            tags = convertForTags(dc.loadObjectResults());

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

    public List<Tag> getTagsByHost ( String hostId ) throws DotDataException {

        List<Tag> tags = tagCache.getByHost(hostId);
        if ( tags == null ) {

            hostId = escapeSingleQuote(hostId);

            //Execute the search
            final DotConnect dc = new DotConnect();
            dc.setSQL("SELECT * FROM tag WHERE host_id = ?");
            dc.addParam(hostId);

            tags = convertForTags(dc.loadObjectResults());

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

    public List<Tag> getTagsLikeNameAndHostIncludingSystemHost ( String name, String hostId ) throws DotDataException {

        name = escapeSingleQuote(name);

        //Execute the search
        final DotConnect dc = new DotConnect();
        dc.setSQL("SELECT * FROM tag WHERE tagname LIKE ? AND (host_id LIKE ? OR host_id LIKE ?)");
        dc.addParam(name.toLowerCase() + "%");
        dc.addParam(hostId);
        dc.addParam(Host.SYSTEM_HOST);

        List<Tag> tags = convertForTags(dc.loadObjectResults());

        //And add the results to the cache
        for ( Tag tag : tags ) {
            if ( tagCache.get(tag.getTagId()) == null ) {
                tagCache.put(tag);
            }
        }

        return tags;
    }

    public Tag getTagByNameAndHost ( String name, String hostId ) throws DotDataException {

        Tag tag = tagCache.get(name, hostId);
        if ( tag == null ) {

            name = escapeSingleQuote(name);

            //Execute the search
            final DotConnect dc = new DotConnect();
            dc.setSQL("SELECT * FROM tag WHERE tagname = ? AND host_id = ?");
            dc.addParam(name.toLowerCase());
            dc.addParam(hostId);

            List<Map<String, Object>> sqlResults = dc.loadObjectResults();
            if ( sqlResults != null && !sqlResults.isEmpty() ) {
                tag = convertForTag(sqlResults.get(0));
            }

            //And add the result to the cache
            if ( tag != null && tag.getTagId() != null ) {
                tagCache.put(tag);
            }
        }

        return tag;
    }

    /**
     * Gets a Tag by a tagId retrieved from a TagInode.
     *
     * @param tagId the tag id to get
     * @return tag
     * @throws DotDataException
     */
    public Tag getTagByTagId ( String tagId ) throws DotDataException {

        Tag tag = tagCache.get(tagId);
        if ( tag == null ) {

            //Execute the search
            final DotConnect dc = new DotConnect();
            dc.setSQL("SELECT * FROM tag WHERE tag_id = ?");
            dc.addParam(tagId);

            List<Map<String, Object>> sqlResults = dc.loadObjectResults();
            if ( sqlResults != null && !sqlResults.isEmpty() ) {
                tag = convertForTag(sqlResults.get(0));
            }

            //And add the result to the cache
            if ( tag != null && tag.getTagId() != null ) {
                tagCache.put(tag);
            }
        }

        return tag;
    }

    public List<Tag> getTagsForUserByUserInode ( String userInode ) throws DotDataException {
        return getTagsByInode(userInode);
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
     */
    public List<Tag> getFilteredTags ( String tagName, String hostFilter, boolean globalTagsFilter, String sort, int start, int count ) {
        try {

            sort = SQLUtil.sanitizeSortBy(sort);

            //Execute the update
            final DotConnect dc = new DotConnect();

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
                    dc.setSQL("SELECT * FROM tag WHERE tagname LIKE ? AND (host_id = ? OR host_id = ?) " + sortStr + limitAndOffset(start, count));
                    dc.addParam("%" + tagName.toLowerCase() + "%");
                    try {
                        dc.addParam(host.getMap().get("tagStorage").toString());
                    } catch ( NullPointerException e ) {
                        dc.addParam(Host.SYSTEM_HOST);
                    }
                    dc.addParam(Host.SYSTEM_HOST);
                }
                //if global host is not set as unique filter but tag name is set, search in current host
                else if ( UtilMethods.isSet(tagName) && !globalTagsFilter ) {
                    dc.setSQL("SELECT * FROM tag WHERE tagname LIKE ? AND (host_id = ?) " + sortStr + limitAndOffset(start, count));
                    dc.addParam("%" + tagName.toLowerCase() + "%");
                    try {
                        dc.addParam(host.getMap().get("tagStorage").toString());
                    } catch ( NullPointerException e ) {
                        dc.addParam(Host.SYSTEM_HOST);
                    }
                } else if ( !UtilMethods.isSet(tagName) && globalTagsFilter ) {
                    //check if tag name is not set and if should display global tags
                    //it will check all global tags and current host tags.
                    dc.setSQL("SELECT * FROM tag WHERE (host_id = ? OR host_id = ? ) " + sortStr + limitAndOffset(start, count));

                    try {
                        dc.addParam(host.getMap().get("tagStorage").toString());
                    } catch ( NullPointerException e ) {
                        dc.addParam(Host.SYSTEM_HOST);
                    }
                    dc.addParam(Host.SYSTEM_HOST);
                } else {
                    //check all current host tags.
                    String sql = "SELECT * FROM tag ";

                    Object tagStorage = host.getMap().get("tagStorage");

                    if ( UtilMethods.isSet(tagStorage) ) {
                        sql = sql + "WHERE ( host_id = ? ) " + sortStr + limitAndOffset(start, count);
                        dc.setSQL(sql);
                        dc.addParam(tagStorage.toString());
                    } else {
                        dc.setSQL(sql);
                    }
                }

                //Execute and return the result of the query
                return convertForTags(dc.loadObjectResults());
            }
        } catch ( Exception e ) {
            Logger.warn(Tag.class, "getFilteredTags failed:" + e, e);
        }
        return new java.util.ArrayList<>();
    }

    private String limitAndOffset ( int start, int limit ) {

        String currentSql = "";
        if ( limit > 0 ) {
            currentSql = currentSql.concat(" LIMIT ").concat(String.valueOf(limit));
        }
        if ( start != -1 ) {
            currentSql = currentSql.concat(" OFFSET ").concat(String.valueOf(start));
        }

        return currentSql;
    }

    public void updateTagInode ( TagInode tagInode, String tagId ) throws DotDataException {

        //First lets clean up the cache
        List<TagInode> cachedTagInodes = tagInodeCache.getByTagId(tagInode.getTagId());
        if ( cachedTagInodes != null && !cachedTagInodes.isEmpty() ) {
            for ( TagInode cachedTagInode : cachedTagInodes ) {
                tagCache.removeByInode(cachedTagInode.getInode());
            }
        }
        tagInodeCache.remove(tagInode);

        //Execute the update
        final DotConnect dc = new DotConnect();
        dc.setSQL("UPDATE tag SET tag_id = ?, mod_date = ? WHERE tag_id = ? AND inode = ?");
        dc.addParam(tagId);
        dc.addParam(new Date());
        dc.addParam(tagInode.getTagId());
        dc.addParam(tagInode.getInode());

        dc.loadResult();
    }

    public Tag createTag ( Tag tag ) throws DotDataException {

        if ( !UtilMethods.isSet(tag.getTagId()) ) {
            tag.setTagId(UUID.randomUUID().toString());
        }

        //Execute the insert
        final DotConnect dc = new DotConnect();
        dc.setSQL("INSERT INTO tag (tag_id, tagname, host_id, user_id, persona, mod_date) VALUES (?,?,?,?,?,?)");
        dc.addParam(tag.getTagId());
        dc.addParam(tag.getTagName());
        dc.addParam(tag.getHostId());
        dc.addParam(tag.getUserId());
        if ( tag.isPersona() ) {
            dc.addParam(DbConnectionFactory.getDBTrue());
        } else {
            dc.addParam(DbConnectionFactory.getDBFalse());
        }
        dc.addParam(new Date());

        dc.loadResult();

        return tag;
    }

    public TagInode createTagInode ( TagInode tagInode ) throws DotDataException {

        //First lets clean up the cache
        tagCache.removeByInode(tagInode.getInode());
        tagInodeCache.remove(tagInode);

        //Execute the insert
        final DotConnect dc = new DotConnect();
        dc.setSQL("INSERT INTO tag_inode (tag_id, inode, mod_date) VALUES (?,?,?)");
        dc.addParam(tagInode.getTagId());
        dc.addParam(tagInode.getInode());
        dc.addParam(new Date());

        dc.loadResult();

        return tagInode;
    }

    public void updateTag ( Tag tag ) throws DotDataException {

        //First lets clean up the cache
        List<TagInode> cachedTagInodes = tagInodeCache.getByTagId(tag.getTagId());
        if ( cachedTagInodes != null && !cachedTagInodes.isEmpty() ) {
            for ( TagInode cachedTagInode : cachedTagInodes ) {
                tagCache.removeByInode(cachedTagInode.getInode());
            }
        }
        tagCache.remove(tag);

        //Execute the update
        final DotConnect dc = new DotConnect();
        dc.setSQL("UPDATE tag SET tagname = ?, host_id = ?, user_id = ?, persona = ?, mod_date = ? WHERE tag_id = ?");
        dc.addParam(tag.getTagName());
        dc.addParam(tag.getHostId());
        dc.addParam(tag.getUserId());
        if ( tag.isPersona() ) {
            dc.addParam(DbConnectionFactory.getDBTrue());
        } else {
            dc.addParam(DbConnectionFactory.getDBFalse());
        }
        dc.addParam(tag.getModDate());
        dc.addParam(tag.getTagId());

        dc.loadResult();
    }

    public void deleteTag ( Tag tag ) throws DotDataException {

        //First lets clean up the cache
        List<TagInode> cachedTagInodes = tagInodeCache.getByTagId(tag.getTagId());
        if ( cachedTagInodes != null && !cachedTagInodes.isEmpty() ) {
            for ( TagInode cachedTagInode : cachedTagInodes ) {
                tagCache.removeByInode(cachedTagInode.getInode());
            }
        }
        tagCache.remove(tag);
        tagInodeCache.removeByTagId(tag.getTagId());

        //Execute the update
        final DotConnect dc = new DotConnect();
        dc.setSQL("DELETE FROM tag WHERE tag_id = ?");
        dc.addParam(tag.getTagId());

        dc.loadResult();
    }

    /**
     * Gets all tags associated to an object
     *
     * @param inode inode of the object tagged
     * @return list of all the TagInode where the tags are associated to the object
     */
    public List<TagInode> getTagInodesByInode ( String inode ) throws DotDataException {

        List<TagInode> tagInodes = tagInodeCache.getByInode(inode);
        if ( tagInodes == null ) {

            //Execute the search
            final DotConnect dc = new DotConnect();
            dc.setSQL("SELECT * FROM tag_inode WHERE inode = ?");
            dc.addParam(inode);

            tagInodes = convertForTagInodes(dc.loadObjectResults());

            //And add the results to the cache
            for ( TagInode tagInode : tagInodes ) {
                if ( tagInodeCache.get(tagInode.getTagId(), tagInode.getInode()) == null ) {
                    tagInodeCache.put(tagInode);
                }
            }
            tagInodeCache.putForInode(inode, tagInodes);
        }

        return tagInodes;
    }

    /**
     * Gets all tags associated to an object
     *
     * @param tagId tagId of the object tagged
     * @return list of all the TagInode where the tags are associated to the object
     */
    public List<TagInode> getTagInodesByTagId ( String tagId ) throws DotDataException {

        List<TagInode> tagInodes = tagInodeCache.getByTagId(tagId);
        if ( tagInodes == null ) {

            //Execute the search
            final DotConnect dc = new DotConnect();
            dc.setSQL("SELECT * FROM tag_inode where tag_id = ?");
            dc.addParam(tagId);

            tagInodes = convertForTagInodes(dc.loadObjectResults());

            //And add the results to the cache
            for ( TagInode tagInode : tagInodes ) {
                if ( tagInodeCache.get(tagInode.getTagId(), tagInode.getInode()) == null ) {
                    tagInodeCache.put(tagInode);
                }
            }
            tagInodeCache.putForTagId(tagId, tagInodes);
        }

        return tagInodes;
    }

    /**
     * Gets a tagInode by name and inode
     *
     * @param tagId id of the tag
     * @param inode inode of the object tagged
     * @return the tagInode
     */
    public TagInode getTagInode ( String tagId, String inode ) throws DotDataException {

        TagInode tagInode = tagInodeCache.get(tagId, inode);
        if ( tagInode == null ) {

            //Execute the search
            final DotConnect dc = new DotConnect();
            dc.setSQL("SELECT * FROM tag_inode WHERE tag_id = ? AND inode = ?");
            dc.addParam(tagId);
            dc.addParam(inode);

            List<Map<String, Object>> sqlResults = dc.loadObjectResults();
            if ( sqlResults != null && !sqlResults.isEmpty() ) {
                tagInode = convertForTagInode(sqlResults.get(0));
            }

            //And add the result to the cache
            if ( tagInode != null && tagInode.getTagId() != null ) {
                tagInodeCache.put(tagInode);
            }
        }

        return tagInode;
    }

    /**
     * Deletes a TagInode
     *
     * @param tagInode TagInode to delete
     */
    public void deleteTagInode ( TagInode tagInode ) throws DotDataException {

        //First lets clean up the cache
        tagInodeCache.remove(tagInode);
        tagCache.removeByInode(tagInode.getInode());

        //Execute the update
        final DotConnect dc = new DotConnect();
        dc.setSQL("DELETE FROM tag_inode WHERE tag_id = ? AND inode = ?");
        dc.addParam(tagInode.getTagId());
        dc.addParam(tagInode.getInode());

        dc.loadResult();
    }

    @Override
    public List<Tag> getTagsByInode ( String inode ) throws DotDataException {

        List<Tag> tags = tagCache.getByInode(inode);
        if ( tags == null ) {

            //Execute the search
            final DotConnect dc = new DotConnect();
            dc.setSQL("SELECT tag.* FROM tag_inode tagInode, tag tag WHERE tagInode.tag_id=tag.tag_id AND tagInode.inode = ?");
            dc.addParam(inode);

            tags = convertForTags(dc.loadObjectResults());

            //And add the results to the cache
            for ( Tag tag : tags ) {

                if ( tagInodeCache.get(tag.getTagId(), inode) == null ) {
                    TagInode tagInode = new TagInode();
                    tagInode.setTagId(tag.getTagId());
                    tagInode.setInode(inode);
                    tagInodeCache.put(tagInode);
                }

                if ( tagCache.get(tag.getTagId()) == null ) {
                    tagCache.put(tag);
                }
            }
            tagCache.putForInode(inode, tags);
        }

        return tags;
    }

    /**
     * Escape a single quote
     *
     * @param tagName string with single quotes
     * @return single quote string escaped
     */
    private String escapeSingleQuote ( String tagName ) {
        return tagName.replace("'", "''");
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

    private List<Tag> convertForTags ( List<Map<String, Object>> sqlResults ) {

        List<Tag> tags = new ArrayList<>();

        if ( sqlResults != null ) {

            for ( Map<String, Object> row : sqlResults ) {
                Tag tag = convertForTag(row);
                tags.add(tag);
            }
        }

        return tags;
    }

    private List<TagInode> convertForTagInodes ( List<Map<String, Object>> sqlResults ) {

        List<TagInode> tagInodes = new ArrayList<>();

        if ( sqlResults != null ) {

            for ( Map<String, Object> row : sqlResults ) {
                TagInode tagInode = convertForTagInode(row);
                tagInodes.add(tagInode);
            }
        }

        return tagInodes;
    }

    private Tag convertForTag ( Map<String, Object> sqlResult ) {

        Tag tag = null;
        if ( sqlResult != null ) {
            tag = new Tag();
            tag.setTagId((String) sqlResult.get(TAG_COLUMN_TAG_ID));
            tag.setTagName((String) sqlResult.get(TAG_COLUMN_TAGNAME));
            tag.setHostId((String) sqlResult.get(TAG_COLUMN_HOST_ID));
            tag.setUserId((String) sqlResult.get(TAG_COLUMN_USER_ID));
            if ( DbConnectionFactory.isMsSql() ) {
                tag.setPersona(Boolean.valueOf(sqlResult.get(TAG_COLUMN_PERSONA).toString()));
            } else {
                tag.setPersona((boolean) sqlResult.get(TAG_COLUMN_PERSONA));
            }
            tag.setModDate((Date) sqlResult.get(TAG_COLUMN_MOD_DATE));
        }

        return tag;
    }

    private TagInode convertForTagInode ( Map<String, Object> sqlResult ) {

        TagInode tagInode = null;
        if ( sqlResult != null ) {
            tagInode = new TagInode();
            tagInode.setTagId((String) sqlResult.get(TAG_INODE_COLUMN_TAG_ID));
            tagInode.setInode((String) sqlResult.get(TAG_INODE_COLUMN_INODE));
            tagInode.setModDate((Date) sqlResult.get(TAG_INODE_COLUMN_MOD_DATE));
        }

        return tagInode;
    }

}