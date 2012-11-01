package com.dotmarketing.portlets.linkchecker.business;

import java.util.List;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.linkchecker.bean.InvalidLink;

public abstract class LinkCheckerFactory {
    public abstract void save(String contentletInode, String fieldInode, List<InvalidLink> links) throws DotDataException;
    public abstract List<InvalidLink> findByInode(String inode) throws DotDataException;
    public abstract void deleteByInode(String inode) throws DotDataException;
    public abstract List<InvalidLink> findAll(int offset, int pageSize) throws DotDataException;
    public abstract int findAllCount() throws DotDataException;
}
