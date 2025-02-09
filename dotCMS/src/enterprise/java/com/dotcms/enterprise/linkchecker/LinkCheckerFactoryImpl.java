/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included 
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.linkchecker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.linkchecker.bean.InvalidLink;
import com.dotmarketing.portlets.linkchecker.business.LinkCheckerFactory;
import com.dotmarketing.util.UUIDGenerator;

public class LinkCheckerFactoryImpl extends LinkCheckerFactory {

    @Override
    public void save(String contentletInode, List<InvalidLink> links) throws DotDataException {
        if(LicenseUtil.getLevel()<LicenseLevel.STANDARD.level)
            return;

        DotConnect dc=new DotConnect();
        for(InvalidLink cub : links) {
            dc.setSQL("insert into broken_link(id,inode,field,link,title,status_code) values(?,?,?,?,?,?)");
            dc.addParam(UUIDGenerator.generateUuid());
            dc.addParam(contentletInode);
            dc.addParam(cub.getField());
            dc.addParam(cub.getUrl());
            dc.addParam(cub.getTitle());
            dc.addParam(cub.getStatusCode());
            dc.loadResult();
        }
    }

    @Override
    public List<InvalidLink> findByInode(String inode) throws DotDataException {
        if(LicenseUtil.getLevel()<LicenseLevel.STANDARD.level)
            return new ArrayList<>();

        DotConnect dc=new DotConnect();
        dc.setSQL("select * from broken_link where inode=?");
        dc.addParam(inode);
        return readObjectResult(dc.loadObjectResults());
    }

    private List<InvalidLink> readObjectResult(List<Map<String,Object>> results) {
        List<InvalidLink> beans=new ArrayList<>();
        for(Map<String,Object> rr : results) {
            InvalidLink bean=new InvalidLink();
            bean.setId((String)rr.get("id"));
            bean.setInode((String)rr.get("inode"));
            bean.setStatusCode(((Number)rr.get("status_code")).intValue());
            bean.setTitle((String)rr.get("title"));
            bean.setUrl((String)rr.get("link"));
            bean.setField((String)rr.get("field"));
            beans.add(bean);
        }
        return beans;
    }

    @Override
    public void deleteByInode(String inode) throws DotDataException {
        if(LicenseUtil.getLevel()< LicenseLevel.STANDARD.level)
            return;

        DotConnect dc=new DotConnect();
        dc.setSQL("delete from broken_link where inode=?");
        dc.addParam(inode);
        dc.loadResult();
    }

    @Override
    public List<InvalidLink> findAll(int offset, int pageSize) throws DotDataException {
        if(LicenseUtil.getLevel()<LicenseLevel.STANDARD.level)
            return new ArrayList<>();

        // we join with contentlet_version to ensure those broken links belongs to active contentlets
        String sql="select * from broken_link join contentlet_version_info vinfo " +
        		"on (vinfo.working_inode=broken_link.inode or vinfo.live_inode=broken_link.inode)";
        int p1=offset,p2=pageSize;
        if(DbConnectionFactory.isOracle()) {
            sql="select * from (" +
            		"select rownum rnum, a.* " +
                    "from (" +
            		    sql +" order by inode,field,link "+
                    ") a " +
                    "where rownum<=?"+
                ") where rnum>=?";
            p1+=pageSize;
            p2=offset;
        }
        else if(DbConnectionFactory.isMsSql()) {
            sql="select * from (" +
                  "select row_number() over (order by inode,field,link) as row_id, * " +
                  "from ( "+sql+" ) as unordered_data " +
                ") as ordered_data " +
                "where row_id between ? and ?";
            p2+=p1;
        }
        else if(DbConnectionFactory.isMySql()) {
        	sql+=" limit ?, ?";
        } else
            sql+=" offset ? limit ?";

        DotConnect dc=new DotConnect();
        dc.setSQL(sql);
        dc.addParam(p1);
        dc.addParam(p2);
        return readObjectResult(dc.loadObjectResults());
    }

    public List<InvalidLink> findAllByStructure(String structureInode, int offset, int pageSize) throws DotDataException {
    if(LicenseUtil.getLevel()<LicenseLevel.STANDARD.level)
        return new ArrayList<>();

    // we join with contentlet_version to ensure those broken links belongs to active contentlets
    String sql="select broken_link.* from broken_link " +
    		"join contentlet_version_info vinfo on (vinfo.working_inode=broken_link.inode or vinfo.live_inode=broken_link.inode) " +
    		"join contentlet cont on (cont.inode=broken_link.inode) " +
    		"where cont.structure_inode=?";

    int p2=offset,p3=pageSize;
    if(DbConnectionFactory.isOracle()) {
        sql="select * from (" +
        		"select rownum rnum, a.* " +
                "from (" +
        		    sql +" order by broken_link.inode,field,link "+
                ") a " +
                "where rownum<=?"+
            ") where rnum>=?";
        p2+=pageSize;
        p3=offset;
    }
    else if(DbConnectionFactory.isMsSql()) {
        sql="select * from (" +
              "select row_number() over (order by inode,field,link) as row_id, * " +
              "from ( "+sql+" ) as unordered_data " +
            ") as ordered_data " +
            "where row_id between ? and ?";
        p3+=p2;
    }
    else if(DbConnectionFactory.isMySql()) {
    	sql+=" limit ?, ?";
    } else
        sql+=" offset ? limit ?";

    final DotConnect dc=new DotConnect();
    dc.setSQL(sql);
    dc.addParam(structureInode);
    dc.addParam(p2);
    dc.addParam(p3);

    return readObjectResult(dc.loadObjectResults());
}

    @Override
    public int findAllCount() throws DotDataException {
        if(LicenseUtil.getLevel()<LicenseLevel.STANDARD.level)
            return 0;

        // we join with contentlet_version to ensure those broken links belongs to active contentlets
        String sql="select count(*) as cc from broken_link " +
        		"join contentlet_version_info vinfo on (vinfo.working_inode=broken_link.inode or vinfo.live_inode=broken_link.inode)";
        DotConnect dc=new DotConnect();
        dc.setSQL(sql);
        Object result=dc.loadObjectResults().get(0).get("cc");
        if(result instanceof Number)
            return ((Number)result).intValue();
        else
            return Integer.parseInt(result.toString());
    }

    public int findAllByStructureCount(String structureInode) throws DotDataException {
        if(LicenseUtil.getLevel()<LicenseLevel.STANDARD.level)
            return 0;

        // we join with contentlet_version to ensure those broken links belongs to active contentlets
        String sql="select count(*) as cc from broken_link " +
        		"join contentlet_version_info vinfo on (vinfo.working_inode=broken_link.inode or vinfo.live_inode=broken_link.inode) " +
        		"join contentlet cont on (cont.inode=broken_link.inode) " +
        		"where cont.structure_inode=?";

        DotConnect dc=new DotConnect();
        dc.setSQL(sql);
        dc.addParam(structureInode);
        Object result=dc.loadObjectResults().get(0).get("cc");
        if(result instanceof Number)
            return ((Number)result).intValue();
        else
            return Integer.parseInt(result.toString());
    }
}
