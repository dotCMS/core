/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
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
