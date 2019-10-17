
package com.liferay.portal.ejb;

import com.dotcms.repackage.net.sf.hibernate.Session;
import com.dotcms.repackage.net.sf.hibernate.dialect.Dialect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotRuntimeException;
import io.vavr.control.Try;



public class BasePersistence {





    protected Session openSession() {
        return HibernateUtil.getSession();
    }

    protected Dialect getDialect() {

        return HibernateUtil.getDialect();


    }


}
