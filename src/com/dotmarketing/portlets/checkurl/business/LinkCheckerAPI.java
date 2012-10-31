package com.dotmarketing.portlets.checkurl.business;

import java.util.List;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.checkurl.bean.CheckURLBean;

public interface LinkCheckerAPI {
    List<CheckURLBean> findInvalidLinks(String htmltext) throws DotDataException, DotSecurityException;
}
