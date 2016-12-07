package com.dotcms.persistence;

import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.db.DbConnectionFactory;
import com.liferay.util.PropertiesUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * This class will returns the generic and vendor resource name to load the Properties and XML Properties from the classpath.
 *
 * The file name on the classpath will be the same package, for instance if you have the class
 *
 * com.dotcms.thing.ThingDAOImpl
 *
 * And My SQL DB, It will returns
 *
 * [com/dotcms/thing/ThingDAOImpl.properties, com/dotcms/thing/ThingDAOImpl.xml, com/dotcms/thing/ThingDAOImplMySQL.properties, com/dotcms/thing/ThingDAOImplMySQL.xml]
 *
 * @author jsanca
 */
public class ClasspathPropertiesResourceNameStrategy implements ResourceNameStrategy {

    private static final String DOT = ".";

    private static final String SLASH = "/";


    @Override
    public List<String> getResourceNames(final Class persistenceClass) {

        final List<String> classpathResourcesNameList =
                new ArrayList<>();
        final String vendorName = this.getVendorName();

        final String basePath =
                StringUtils.replace
                        (persistenceClass.getName(), DOT, SLASH);

        // adding the generic prop and xml
        classpathResourcesNameList.add(basePath + PropertiesUtil.PROP_EXT);
        classpathResourcesNameList.add(basePath + PropertiesUtil.XML_EXT);

        // adding the vendor prop and xml
        classpathResourcesNameList.add(basePath + vendorName + PropertiesUtil.PROP_EXT);
        classpathResourcesNameList.add(basePath + vendorName + PropertiesUtil.XML_EXT);

        return classpathResourcesNameList;
    } // getResourceNames.

    /**
     * Get the vendor name for the current persistance.
     * @return String
     */
    protected String getVendorName () {

        return DbConnectionFactory.getDBType();
    }
} // E:O:F:ClasspathPropertiesResourceNameStrategy.
