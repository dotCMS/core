package com.dotmarketing.portlets.checkurl.business;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.checkurl.bean.CheckURLBean;

public class LinkCheckerFactoryImpl extends LinkCheckerFactory {
    
    @Override
    protected void save(String contentletInode, String fieldInode, List<CheckURLBean> links) throws DotDataException {
        DotConnect dc=new DotConnect();
        for(CheckURLBean cub : links) {
            dc.setSQL("insert into broken_link(inode,field,link,title,status_code) values(?,?,?,?,?)");
            dc.addParam(contentletInode);
            dc.addParam(fieldInode);
            dc.addParam(cub.getUrl());
            dc.addParam(cub.getTitle());
            dc.addParam(cub.getStatusCode());
            dc.loadResult();
        }
    }
    
    @Override
    protected List<CheckURLBean> findByInode(String inode) throws DotDataException {
        DotConnect dc=new DotConnect();
        dc.setSQL("select * from broken_link where inode=?");
        dc.addParam(inode);
        return readObjectResult(dc.loadObjectResults());
    }
    
    private List<CheckURLBean> readObjectResult(List<Map<String,Object>> results) {
        List<CheckURLBean> beans=new ArrayList<CheckURLBean>();
        for(Map<String,Object> rr : results) {
            CheckURLBean bean=new CheckURLBean();
            bean.setInode((String)rr.get("inode"));
            bean.setStatusCode((Integer)rr.get("status_code"));
            bean.setTitle((String)rr.get("title"));
            bean.setUrl((String)rr.get("link"));
            beans.add(bean);
        }
        return beans;
    }
    
    @Override
    protected void deleteByInode(String inode) throws DotDataException {
        DotConnect dc=new DotConnect();
        dc.setSQL("delete from broken_link where inode=?");
        dc.addParam(inode);
        dc.loadResult();
    }
    
    @Override
    protected List<CheckURLBean> findAll(int offset, int pageSize) throws DotDataException {
        String sql="select * from broken_link";
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
        else
            sql+=" limit ? offset ?";
        
        DotConnect dc=new DotConnect();
        dc.setSQL(sql);
        dc.addParam(p1);
        dc.addParam(p2);
        return readObjectResult(dc.loadObjectResults());
    }
    
    @Override
    protected int findAllCount() throws DotDataException {
        DotConnect dc=new DotConnect();
        dc.setSQL("select count(*) as cc from broken_link");
        return (Integer)dc.loadObjectResults().get(0).get("cc");
    }
    
}
