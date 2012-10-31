package com.dotmarketing.portlets.linkchecker.business;

import java.util.List;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.linkchecker.bean.CheckURLBean;

public abstract class LinkCheckerFactory {
    protected abstract void save(String contentletInode, String fieldInode, List<CheckURLBean> links) throws DotDataException;
    protected abstract List<CheckURLBean> findByInode(String inode) throws DotDataException;
    protected abstract void deleteByInode(String inode) throws DotDataException;
    protected abstract List<CheckURLBean> findAll(int offset, int pageSize) throws DotDataException;
    protected abstract int findAllCount() throws DotDataException;
}
