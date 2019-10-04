
package com.liferay.portal.ejb;

import com.dotcms.repackage.net.sf.hibernate.Session;
import com.dotcms.repackage.net.sf.hibernate.dialect.Dialect;
import com.dotcms.repackage.net.sf.hibernate.impl.SessionFactoryImpl;
import com.dotmarketing.exception.DotRuntimeException;
import io.vavr.control.Try;



public class BasePersistence {

    private static Dialect _dialect = null;



    protected Session openSession() {
        return Try.of(() -> com.dotmarketing.db.HibernateUtil.getSession()).getOrElseThrow(e -> new DotRuntimeException(e));
    }

    protected Dialect getDialect() {

        if (_dialect == null) {
            _dialect = ((SessionFactoryImpl) openSession().getSessionFactory()).getDialect();

        }
        return _dialect;


    }


}
