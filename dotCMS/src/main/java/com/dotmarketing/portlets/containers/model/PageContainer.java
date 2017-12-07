package com.dotmarketing.portlets.containers.model;

import com.dotcms.repackage.jersey.repackaged.com.google.common.base.Objects;

import com.dotmarketing.business.DotStateException;

import java.io.Serializable;



public class PageContainer implements Serializable {


    private static final long serialVersionUID = 1L;
    private final Container container;
    private final String uniqueVal;

    public PageContainer(final Container container, final String uniqueVal) {
        super();
        if (null == container) {
            throw new DotStateException("Container cannot be null");
        }
        this.container = container;
        this.uniqueVal = uniqueVal;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((container == null) ? 0 : container.hashCode());
        result = prime * result + ((uniqueVal == null) ? 0 : uniqueVal.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        PageContainer other = (PageContainer) obj;
        if (other == null) {
            return false;
        } else if (getClass() != obj.getClass()) {
            return false;
        } else if (!Objects.equal(container, other.container)) {
            return false;
        } else if (!Objects.equal(uniqueVal, other.uniqueVal)) {
            return false;
        }
        return true;
    }



}
