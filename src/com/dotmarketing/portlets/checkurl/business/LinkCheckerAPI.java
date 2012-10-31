package com.dotmarketing.portlets.checkurl.business;

import java.util.List;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.checkurl.bean.CheckURLBean;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;

public interface LinkCheckerAPI {
    List<CheckURLBean> findInvalidLinks(String htmltext) throws DotDataException, DotSecurityException;
    void saveInvalidLinks(Contentlet contentlet, Field field, List<CheckURLBean> links) throws DotDataException, DotSecurityException;
    void deleteInvalidLinks(Contentlet contentlet) throws DotDataException, DotSecurityException;
    List<CheckURLBean> findByInode(String inode) throws DotDataException;
    List<CheckURLBean> findAll(int offset, int pageSize) throws DotDataException;
    int findAllCount() throws DotDataException;
}
