package com.dotmarketing.portlets.linkchecker.business;

import java.util.List;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.linkchecker.bean.InvalidLink;

public abstract class LinkCheckerFactory {
    protected abstract void save(String contentletInode, String fieldInode, List<InvalidLink> links) throws DotDataException;
    protected abstract List<InvalidLink> findByInode(String inode) throws DotDataException;
    protected abstract void deleteByInode(String inode) throws DotDataException;
    protected abstract List<InvalidLink> findAll(int offset, int pageSize) throws DotDataException;
    protected abstract int findAllCount() throws DotDataException;
}
