package com.dotmarketing.tag.business;

import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.repackage.com.google.common.base.Strings;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.tag.model.TagInode;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.Lazy;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation class for the {@link TagFactory} interface.
 *
 * @author Jonathan Gamba
 *         Date: 1/28/16
 */
public class TagFactoryImpl implements TagFactory {

    private static final Lazy<Integer> MAX_TOP_TAGS = Lazy.of(() -> Config.getIntProperty("MAX_TOP_TAGS", 1000));
    private static final String TAG_COLUMN_TAG_ID = "tag_id";
    private static final String TAG_COLUMN_TAGNAME = "tagname";
    private static final String TAG_COLUMN_HOST_ID = "host_id";
    private static final String TAG_COLUMN_USER_ID = "user_id";
    private static final String TAG_COLUMN_PERSONA = "persona";
    private static final String TAG_COLUMN_MOD_DATE = "mod_date";

    private static final String TAG_ORDER_BY_DEFAULT = "ORDER BY tagname";

    private static final String TAG_INODE_COLUMN_TAG_ID = "tag_id";
    private static final String TAG_INODE_COLUMN_INODE = "inode";
    private static final String TAG_INODE_COLUMN_FIELD_VAR_NAME = "field_var_name";
    private static final String TAG_INODE_COLUMN_MOD_DATE = "mod_date";

    private TagCache tagCache;
    private TagInodeCache tagInodeCache;

    /**
     * Creates an instance of this class.
     */
    public TagFactoryImpl () {
        tagCache = CacheLocator.getTagCache();
        tagInodeCache = CacheLocator.getTagInodeCache();
    }

    @Override
    public List<Tag> getAllTags () throws DotDataException {

        //Execute the search
        final DotConnect dc = new DotConnect();
        dc.setSQL("SELECT * FROM tag " + TAG_ORDER_BY_DEFAULT);

        List<Tag> tags = convertForTags(dc.loadObjectResults());

        //And add the results to the cache
        for ( Tag tag : tags ) {
            if ( tagCache.get(tag.getTagId()) == null ) {
                tagCache.put(tag);
            }
        }

        return tags;
    }

    @Override
    public List<Tag> getTagsByName (final String name) throws DotDataException {

        List<Tag> tags = tagCache.getByName(name);
        if ( tags == null ) {
            //Execute the search
            final DotConnect dc = new DotConnect();
			dc.setSQL("SELECT * FROM tag WHERE tagname = ?");
            dc.addParam(name.toLowerCase());
            tags = convertForTags(dc.loadObjectResults());

            //And add the results to the cache
            for (final Tag tag : tags) {
                if ( tagCache.get(tag.getTagId()) == null ) {
                    tagCache.put(tag);
                }
            }
            tagCache.putForName(name, tags);
        }

        return tags;
    }

    @Override
    public List<Tag> getTagsByHost (final String siteId) throws DotDataException {

        List<Tag> tags = tagCache.getByHost(siteId);
        if ( tags == null ) {
            //Execute the search
            final DotConnect dc = new DotConnect();
            dc.setSQL("SELECT * FROM tag WHERE host_id = ? " + TAG_ORDER_BY_DEFAULT);
            dc.addParam(siteId);

            tags = convertForTags(dc.loadObjectResults());

            //And add the results to the cache
            for (final Tag tag : tags) {
                if ( tagCache.get(tag.getTagId()) == null ) {
                    tagCache.put(tag);
                }
            }
            tagCache.putForHost(siteId, tags);
        }

        return tags;
    }

    @Override
    public List<Tag> getSuggestedTags(final String name, final String siteId) throws DotDataException {
        //Execute the search
        final DotConnect dc = new DotConnect();

        if ( UtilMethods.isSet(siteId) ) {
            dc.setSQL("SELECT * FROM tag WHERE tagname LIKE ? AND (host_id LIKE ? OR host_id LIKE ?) ORDER BY tagname, host_id");
        } else {
            dc.setSQL("SELECT * FROM tag WHERE tagname LIKE ? AND host_id LIKE ? ORDER BY tagname, host_id");
        }

        dc.addParam("%" + name.toLowerCase() + "%");
        dc.addParam("%" + Host.SYSTEM_HOST + "%");
        if ( UtilMethods.isSet(siteId) ) {
            dc.addParam("%" + siteId + "%");
        }

        //Convert and return the list of found tags excluding tags with the same tag name
        final List<Tag> tags = convertForTagsFilteringDuplicated(dc.loadObjectResults());

        //And add the results to the cache
        for (final Tag tag : tags) {
            if ( tagCache.get(tag.getTagId()) == null ) {
                tagCache.put(tag);
            }
        }

        return tags;
    }

    @Override
    public Tag getTagByNameAndHost (final String name, String siteId) throws DotDataException {

        Tag tag = tagCache.get(name, siteId);
        if ( tag == null ) {
            //Execute the search
            final DotConnect dc = new DotConnect();
            dc.setSQL("SELECT * FROM tag WHERE tagname = ? AND host_id = ?");
            dc.addParam(name.toLowerCase());
            dc.addParam(siteId);

            final List<Map<String, Object>> sqlResults = dc.loadObjectResults();
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

    @Override
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

    @Override
    public List<Tag> getTagsForUserByUserInode ( String userInode ) throws DotDataException {
        return getTagsByInode(userInode);
    }

    @Override
    public List<Tag> getFilteredTags(String tagName, String hostFilter, boolean globalTagsFilter, boolean excludePersonas, String sort, int start, int count) {
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
                if ( UtilMethods.isSet(tagName) ) {
                    // When filtering by tag name, order by length first (shortest matches first), then alphabetically
                    sortStr = "ORDER BY LENGTH(tagname), tagname ASC";
                } else if ( UtilMethods.isSet(sort) ) {
                    String sortDirection = sort.startsWith("-") ? "desc" : "asc";
                    sort = sort.startsWith("-") ? sort.substring(1, sort.length()) : sort;

                    if ( sort.equalsIgnoreCase("hostname") ) sort = "host_id";

                    sortStr = "ORDER BY " + sort + " " + sortDirection;
                } else {
                    sortStr = TAG_ORDER_BY_DEFAULT;
                }

                //Filter by tagname, hosts and persona
                if ( UtilMethods.isSet(tagName) ) {

                    String effectiveTagStorage = getEffectiveTagStorage(host);

                    String personaFragment = "";
                    if ( excludePersonas ) {
                        personaFragment = " AND persona = ? ";
                    }
                    String globalTagsFragment = "";
                    if ( globalTagsFilter ) {
                        globalTagsFragment = " AND (host_id = ? OR host_id = ?) ";
                    } else {
                        globalTagsFragment = " AND host_id = ? ";
                    }

                    String sql = "SELECT * FROM tag WHERE tagname LIKE ? ESCAPE '\\' " + globalTagsFragment + personaFragment + sortStr;

                    dc.setSQL(SQLUtil.addLimits(sql, start, count));
                    String escapedTagName = tagName.replace("\\", "\\\\")
                                                   .replace("_", "\\_")
                                                   .replace("%", "\\%")
                                                   .toLowerCase();
                    dc.addParam("%" + escapedTagName + "%");
                    dc.addParam(effectiveTagStorage);
                    if ( globalTagsFilter ) {
                        dc.addParam(Host.SYSTEM_HOST);
                    }
                    if ( excludePersonas ) {
                        dc.addParam(false);
                    }
                } else if ( !UtilMethods.isSet(tagName) && globalTagsFilter ) {
                    //check if tag name is not set and if should display global tags
                    //it will check all global tags and current host tags.

                    String effectiveTagStorage = getEffectiveTagStorage(host);

                    String personaFragment = "";
                    if ( excludePersonas ) {
                        personaFragment = " AND persona = ? ";
                    }

                    String sql = "SELECT * FROM tag WHERE (host_id = ? OR host_id = ? ) " + personaFragment + sortStr;
                    dc.setSQL(SQLUtil.addLimits(sql, start, count));

                    dc.addParam(effectiveTagStorage);
                    dc.addParam(Host.SYSTEM_HOST);
                    if ( excludePersonas ) {
                        dc.addParam(false);
                    }
                } else {
                    //check all current host tags.
                    String sql = "SELECT * FROM tag ";

                    String effectiveTagStorage = getEffectiveTagStorage(host);

                    String personaFragment = "";
                    if ( excludePersonas ) {
                        personaFragment = " AND persona = ? ";
                    }

                    sql = sql + "WHERE host_id = ? " + personaFragment + sortStr;
                    dc.setSQL(SQLUtil.addLimits(sql,start,count));
                    dc.addParam(effectiveTagStorage);
                    if ( excludePersonas ) {
                        dc.addParam(false);
                    }
                }

                //Execute and return the result of the query
                return convertForTags(dc.loadObjectResults());
            }
        } catch ( Exception e ) {
            Logger.warn(Tag.class, "getFilteredTags failed: " + e, e);
        }
        return List.of();
    }

    @Override
    public long getFilteredTagsCount(String tagName, String hostFilter, boolean globalTagsFilter, boolean excludePersonas) throws DotDataException {
        try {
            final DotConnect dc = new DotConnect();
            
            Host host = null;
            try {
                host = APILocator.getHostAPI().find(hostFilter, APILocator.getUserAPI().getSystemUser(), true);
            } catch (Exception e) {
                Logger.warn(Tag.class, "Unable to get host according to search criteria - hostId = " + hostFilter);
            }
            
            if (UtilMethods.isSet(host)) {
                
                // Filter by tagname, hosts and persona
                if (UtilMethods.isSet(tagName)) {
                    
                    String effectiveTagStorage = getEffectiveTagStorage(host);
                    
                    String personaFragment = "";
                    if (excludePersonas) {
                        personaFragment = " AND persona = ? ";
                    }
                    String globalTagsFragment = "";
                    if (globalTagsFilter) {
                        globalTagsFragment = " AND (host_id = ? OR host_id = ?) ";
                    } else {
                        globalTagsFragment = " AND host_id = ? ";
                    }
                    
                    String sql = "SELECT COUNT(*) as count FROM tag WHERE tagname LIKE ? ESCAPE '\\' " + globalTagsFragment + personaFragment;
                    
                    dc.setSQL(sql);
                    String escapedTagName = tagName.replace("\\", "\\\\")
                                                   .replace("_", "\\_")
                                                   .replace("%", "\\%")
                                                   .toLowerCase();
                    dc.addParam("%" + escapedTagName + "%");
                    dc.addParam(effectiveTagStorage);
                    if (globalTagsFilter) {
                        dc.addParam(Host.SYSTEM_HOST);
                    }
                    if (excludePersonas) {
                        dc.addParam(false);
                    }
                } else if (!UtilMethods.isSet(tagName) && globalTagsFilter) {
                    // Check if tag name is not set and if should display global tags
                    
                    String effectiveTagStorage = getEffectiveTagStorage(host);
                    
                    String personaFragment = "";
                    if (excludePersonas) {
                        personaFragment = " AND persona = ? ";
                    }
                    
                    String sql = "SELECT COUNT(*) as count FROM tag WHERE (host_id = ? OR host_id = ? ) " + personaFragment;
                    dc.setSQL(sql);
                    
                    dc.addParam(effectiveTagStorage);
                    dc.addParam(Host.SYSTEM_HOST);
                    if (excludePersonas) {
                        dc.addParam(false);
                    }
                } else {
                    // Check all current host tags.
                    String sql = "SELECT COUNT(*) as count FROM tag ";
                    
                    String effectiveTagStorage = getEffectiveTagStorage(host);
                    
                    String personaFragment = "";
                    if (excludePersonas) {
                        personaFragment = " AND persona = ? ";
                    }
                    
                    sql = sql + "WHERE host_id = ? " + personaFragment;
                    dc.setSQL(sql);
                    dc.addParam(effectiveTagStorage);
                    if (excludePersonas) {
                        dc.addParam(false);
                    }
                }
                
                // Execute and return the count
                List<Map<String, Object>> results = dc.loadObjectResults();
                if (!results.isEmpty()) {
                    return Long.parseLong(results.get(0).get("count").toString());
                }
            }
        } catch (Exception e) {
            Logger.warn(Tag.class, "getFilteredTagsCount failed: " + e, e);
        }
        return 0L;
    }

    @Override
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
        dc.setSQL("UPDATE tag_inode SET tag_id = ?, mod_date = ? WHERE tag_id = ? AND inode = ? AND field_var_name = ?");
        dc.addParam(tagId);
        dc.addParam(new Date());
        dc.addParam(tagInode.getTagId());
        dc.addParam(tagInode.getInode());
        dc.addParam(tagInode.getFieldVarName());

        dc.loadResult();
    }

    @Override
    public Tag createTag(Tag tag) throws DotDataException {

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
        dc.addParam(tag.isPersona());
        dc.addParam(new Date());

        dc.loadResult();

        //Clean up list references to this tag name and host
        tagCache.remove(tag);

        return tag;
    }

    @Override
    public TagInode createTagInode ( TagInode tagInode ) throws DotDataException {

        //First lets clean up the cache
        tagCache.removeByInode(tagInode.getInode());
        tagInodeCache.remove(tagInode);

        //Execute the insert
        final DotConnect dc = new DotConnect();
        dc.setSQL("INSERT INTO tag_inode (tag_id, inode, field_var_name, mod_date) VALUES (?,?,?,?)");
        dc.addParam(tagInode.getTagId());
        dc.addParam(tagInode.getInode());
        dc.addParam(tagInode.getFieldVarName());
        dc.addParam(new Date());

        dc.loadResult();

        //Clean up list references to this inode
        tagInodeCache.remove(tagInode);

        return tagInode;
    }

    @Override
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
        dc.addParam(tag.isPersona());
        dc.addParam(tag.getModDate());
        dc.addParam(tag.getTagId());

        dc.loadResult();
    }

    @Override
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

    @Override
    public void deleteTagsInBatch(java.util.Collection<String> tagIds) throws DotDataException {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }

        // Clear cache for each tag (same as single delete does)
        for (String tagId : tagIds) {
            List<TagInode> cachedTagInodes = tagInodeCache.getByTagId(tagId);
            if (cachedTagInodes != null && !cachedTagInodes.isEmpty()) {
                for (TagInode cachedTagInode : cachedTagInodes) {
                    tagCache.removeByInode(cachedTagInode.getInode());
                }
            }
            Tag tag = tagCache.get(tagId);
            if (tag != null) {
                tagCache.remove(tag);
            }
            tagInodeCache.removeByTagId(tagId);
        }

        // Prepare batch parameters
        List<com.dotmarketing.common.db.Params> params = tagIds.stream()
            .map(com.dotmarketing.common.db.Params::new)
            .collect(java.util.stream.Collectors.toList());

        // Batch delete operations
        new com.dotmarketing.common.db.DotConnect()
            .executeBatch("DELETE FROM tag_inode WHERE tag_id = ?", params);
        new com.dotmarketing.common.db.DotConnect()
            .executeBatch("DELETE FROM tag WHERE tag_id = ?", params);
    }

    @Override
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
                if ( tagInodeCache.get(tagInode.getTagId(), tagInode.getInode(), tagInode.getFieldVarName()) == null ) {
                    tagInodeCache.put(tagInode);
                }
            }
            tagInodeCache.putForInode(inode, tagInodes);
        }

        return tagInodes;
    }

    @Override
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
                if ( tagInodeCache.get(tagInode.getTagId(), tagInode.getInode(), tagInode.getFieldVarName()) == null ) {
                    tagInodeCache.put(tagInode);
                }
            }
            tagInodeCache.putForTagId(tagId, tagInodes);
        }

        return List.copyOf(tagInodes);
    }

    @Override
    public TagInode getTagInode ( String tagId, String inode, String fieldVarName ) throws DotDataException {

        TagInode tagInode = tagInodeCache.get(tagId, inode, fieldVarName);
        if ( tagInode == null ) {

            //Execute the search
            final DotConnect dc = new DotConnect();
            dc.setSQL("SELECT * FROM tag_inode WHERE tag_id = ? AND inode = ? AND field_var_name = ?");
            dc.addParam(tagId);
            dc.addParam(inode);
            dc.addParam(fieldVarName);

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

    @Override
    public void deleteTagInodesByInode(String inode) throws DotDataException {

        try {
            //Get the current tagInodes in order to do a proper clean up
            List<TagInode> currentTagsInodes = getTagInodesByInode(inode);
            if ( currentTagsInodes != null ) {
                for ( TagInode tagInode : currentTagsInodes ) {
                    tagInodeCache.remove(tagInode);
                    tagCache.removeByInode(tagInode.getInode());
                }
            }
        } catch (DotDataException e) {
            Logger.error(this, "Error cleaning up cache.", e);
        }

        //Execute the delete
        final DotConnect dc = new DotConnect();
        dc.setSQL("DELETE FROM tag_inode WHERE inode = ?");
        dc.addParam(inode);

        dc.loadResult();
    }

    @Override
    public void deleteTagInodesByTagId(String tagId) throws DotDataException {

        try {
            //Get the current tagInodes in order to do a proper clean up
            List<TagInode> currentTagsInodes = getTagInodesByTagId(tagId);
            if ( currentTagsInodes != null ) {
                for ( TagInode tagInode : currentTagsInodes ) {
                    tagInodeCache.remove(tagInode);
                    tagCache.removeByInode(tagInode.getInode());
                }
            }
        } catch (DotDataException e) {
            Logger.error(this, "Error cleaning up cache.", e);
        }

        //Execute the delete
        final DotConnect dc = new DotConnect();
        dc.setSQL("DELETE FROM tag_inode WHERE tag_id = ?");
        dc.addParam(tagId);

        dc.loadResult();
    }

    @Override
    public void deleteTagInodesByInodeAndFieldVarName(String inode, String fieldVarName) throws DotDataException {

    	//Get the current tagInodes in order to do a proper clean up
        for ( TagInode tagInode : getTagInodesByInode(inode) ) {
        	if (fieldVarName != null && fieldVarName.equals(tagInode.getFieldVarName())) {
        		tagInodeCache.remove(tagInode);
                tagCache.removeByInode(tagInode.getInode());
        	}
        }

        //Execute the delete
        final DotConnect dc = new DotConnect();
        dc.setSQL("DELETE FROM tag_inode WHERE inode = ? AND field_var_name = ?");
        dc.addParam(inode);
        dc.addParam(fieldVarName);

        dc.loadResult();
    }

    @Override
    public void deleteTagInode ( TagInode tagInode ) throws DotDataException {

        //First lets clean up the cache
        tagInodeCache.remove(tagInode);
        tagCache.removeByInode(tagInode.getInode());

        //Execute the delete
        final DotConnect dc = new DotConnect();
        if ( UtilMethods.isSet(tagInode.getFieldVarName()) ) {
            dc.setSQL("DELETE FROM tag_inode WHERE tag_id = ? AND inode = ? AND field_var_name = ?");
        } else {
            dc.setSQL("DELETE FROM tag_inode WHERE tag_id = ? AND inode = ?");
        }
        dc.addParam(tagInode.getTagId());
        dc.addParam(tagInode.getInode());
        if ( UtilMethods.isSet(tagInode.getFieldVarName()) ) {
            dc.addParam(tagInode.getFieldVarName());
        }

        dc.loadResult();
    }

    @Override
    public List<Tag> getTagsByInode ( String inode ) throws DotDataException {

        List<Tag> tags = tagCache.getByInode(inode);
        if ( tags == null ) {

            //Execute the search
            final DotConnect dc = new DotConnect();
            dc.setSQL("SELECT tag.* FROM tag_inode tagInode, tag tag WHERE tagInode.tag_id=tag.tag_id AND tagInode.inode = ? ORDER BY tag.tagname");
            dc.addParam(inode);

            tags = convertForTags(dc.loadObjectResults());

            //And add the results to the cache
            for ( Tag tag : tags ) {
                if ( tagCache.get(tag.getTagId()) == null ) {
                    tagCache.put(tag);
                }
            }
            tagCache.putForInode(inode, tags);
        }

        return List.copyOf(tags);
    }

    @Override
    public List<Tag> getTagsByInodeAndFieldVarName(String inode, String fieldVarName) throws DotDataException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(inode));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(fieldVarName));
        //Execute the search
        final DotConnect dc = new DotConnect();
        dc.setSQL("SELECT t.* FROM tag t JOIN tag_inode ti ON t.tag_id = ti.tag_id WHERE ti.inode = ? and ti.field_var_name = ?");
        dc.addParam(inode);
        dc.addParam(fieldVarName);

        List<Tag> tags = convertForTags(dc.loadObjectResults());

        //And add the results to the cache
        for ( Tag tag : tags ) {
            if ( tagCache.get(tag.getTagId()) == null ) {
                tagCache.put(tag);
            }
        }

        return List.copyOf(tags);
    }

    @Override
    public Set<String> getTopTagsBySiteId(final String siteId) throws DotDataException {

        Preconditions.checkArgument(!Strings.isNullOrEmpty(siteId));

        final String selectTopTagsQuery =
                "select distinct(tagname), count(tinode) from " +
                        "( " +
                        "   select lower(tagname) as tagname, tag_inode.inode as tinode " +
                        "   from  " +
                        "   tag, tag_inode  " +
                        "   where  " +
                        "   tag.tag_id=tag_inode.tag_id  " +
                        "   and tag.host_id=? " +
                        "UNION ALL " +
                        "   select lower(tagname) as tagname, tag_inode.inode as tinode " +
                        "   from  " +
                        "   tag, tag_inode  " +
                        "   where  " +
                        "   tag.tag_id=tag_inode.tag_id  " +
                        ") as foo  " +
                        "group by tagname  " +
                        "order by count(tinode) desc " +
                        "limit " + MAX_TOP_TAGS.get();

        final List<Map<String, Object>> results = new DotConnect()
                .setSQL(selectTopTagsQuery)
                .addParam(siteId)
                .loadObjectResults();

        return results.stream().map(row -> row.get(TAG_COLUMN_TAGNAME).toString()).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Resolves the effective tag storage for a host, respecting the global parameter.
     * 
     * @param host The host to resolve tag storage for
     * @return The effective tag storage ID
     */
    private String getEffectiveTagStorage(Host host) {
        Object tagStorage = host.getMap().get("tagStorage");
        
        // If no tagStorage set, use site's own identifier (consistent with TagAPIImpl)
        if (!UtilMethods.isSet(tagStorage)) {
            return host.getIdentifier();
        }
        
        return tagStorage.toString();
    }

    /**
     * Convert the SQL tags results into a list of Tags objects.
     * <p>This method will exclude duplicated Tags, a Tag is duplicated when exist another in the given list with
     * the same tag name.</p>
     * <p>What decides what tag is exclude depends on the order of the given elements, as soon as an element
     * duplicated is found will be exclude it, so the sql that generates this given list matters.</p>
     *
     * @param sqlResults sql query results
     * @return a list of tags objects
     */
    private List<Tag> convertForTagsFilteringDuplicated(List<Map<String, Object>> sqlResults) {

        Map<String, Tag> tagsMap = new LinkedHashMap<>();

        if ( sqlResults != null ) {

            for ( Map<String, Object> row : sqlResults ) {
                Tag tag = convertForTag(row);

                if ( !tagsMap.containsKey(tag.getTagName()) ) {
                    tagsMap.put(tag.getTagName(), tag);
                } else {
                    //Even if this tag is duplicated we want to give preferences to Personas tags
                    if ( tag.isPersona() ) {
                        tagsMap.put(tag.getTagName(), tag);
                    }
                }
            }
        }

        return List.copyOf(tagsMap.values());
    }

    /**
     * Convert the SQL tags results into a list of Tags objects
     * @param sqlResults sql query results
     * @return a list of tags objects
     */
    private List<Tag> convertForTags ( List<Map<String, Object>> sqlResults ) {

        List<Tag> tags = new ArrayList<>();

        if ( sqlResults != null ) {

            for ( Map<String, Object> row : sqlResults ) {
                Tag tag = convertForTag(row);
                tags.add(tag);
            }
        }

        return List.copyOf(tags);
    }

    /**
     * Convert the SQL tagInodes results into a list of TagInodes objects
     * @param sqlResults sql query results
     * @return a list of tags objects
     */
    private List<TagInode> convertForTagInodes ( List<Map<String, Object>> sqlResults ) {

        List<TagInode> tagInodes = new ArrayList<>();

        if ( sqlResults != null ) {

            for ( Map<String, Object> row : sqlResults ) {
                TagInode tagInode = convertForTagInode(row);
                tagInodes.add(tagInode);
            }
        }

        return List.copyOf(tagInodes);
    }

	/**
	 * Converts the tag information coming from the database into a {@link Tag}
	 * object with all of its properties. If the information is not present, a 
	 * <code>null</code> value will be returned.
	 * 
	 * @param sqlResult
	 *            - The data of a specific tag from the database.
	 * @return The {@link Tag} object.
	 */
    private Tag convertForTag ( Map<String, Object> sqlResult ) {

        Tag tag = null;
        if ( sqlResult != null ) {
            tag = new Tag();
            tag.setTagId((String) sqlResult.get(TAG_COLUMN_TAG_ID));
            tag.setTagName((String) sqlResult.get(TAG_COLUMN_TAGNAME));
            tag.setHostId((String) sqlResult.get(TAG_COLUMN_HOST_ID));
            tag.setUserId((String) sqlResult.get(TAG_COLUMN_USER_ID));
            if(UtilMethods.isSet(sqlResult.get(TAG_COLUMN_PERSONA))){
                tag.setPersona(DbConnectionFactory.isDBTrue(sqlResult.get(TAG_COLUMN_PERSONA).toString()));
            } else {
                tag.setPersona(false);
            }
            tag.setModDate((Date) sqlResult.get(TAG_COLUMN_MOD_DATE));
        }

        return tag;
    }

    /**
     * Convert the SQL tagInode result into a TagInode object
     * @param sqlResult sql query result
     * @return a TagInode object
     */
    private TagInode convertForTagInode ( Map<String, Object> sqlResult ) {

        TagInode tagInode = null;
        if ( sqlResult != null ) {
            tagInode = new TagInode();
            tagInode.setTagId((String) sqlResult.get(TAG_INODE_COLUMN_TAG_ID));
            tagInode.setInode((String) sqlResult.get(TAG_INODE_COLUMN_INODE));
            tagInode.setFieldVarName((String) sqlResult.get(TAG_INODE_COLUMN_FIELD_VAR_NAME));
            tagInode.setModDate((Date) sqlResult.get(TAG_INODE_COLUMN_MOD_DATE));
        }

        return tagInode;
    }

}
