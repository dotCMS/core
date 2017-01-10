package com.dotmarketing.factories;

import java.io.Serializable;

import com.dotcms.repackage.net.sf.hibernate.engine.SessionImplementor;
import com.dotcms.repackage.net.sf.hibernate.id.IdentifierGenerator;


/** 
 * <b>vm.long</b><br>
 * <br>
 * An <tt>IdentifierGenerator</tt> that returns a <tt>Long</tt>, constructed from the 
 * system time and a counter value. Not safe for use in a cluster!
 */
public class PermissionGenerator implements IdentifierGenerator {

    private static long counter = System.currentTimeMillis();

    protected long getCount() {
        synchronized(PermissionGenerator.class) {
            return counter++;
        }
    }

    public synchronized Serializable generate(SessionImplementor cache, Object obj) {
        return new Long(  getCount() );
    }



}
