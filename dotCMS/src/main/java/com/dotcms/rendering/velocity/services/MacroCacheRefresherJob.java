package com.dotcms.rendering.velocity.services;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.util.SystemProperties;
import io.vavr.control.Try;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

/**
 * This job is in charge of refreshing the macro caches, following these strategies:
 *
 * 1) read the macro in the system.properties
 * 2) find (content search) and read any file whose name is dot_velocity_macros.vtl and read those in
 * @author jsanca
 */
public class MacroCacheRefresherJob implements Runnable {

    private static final int VELOCITY_MACRO_CACHE_REFRESH_QUERY_CONTENTLETS_LIMIT = Config.getIntProperty("VELOCITY_MACRO_CACHE_REFRESH_QUERY_CONTENTLETS_LIMIT",100000);
    private final static String VELOCITY_MACRO_CACHE_REFRESH_QUERY = Config.getStringProperty("VELOCITY_MACRO_CACHE_REFRESH_QUERY",
            "+contentType:fileAsset  +conHost:%1$s +live:true +fileasset.filename:dot_velocity_macros.vtl");

    @Override
    public void run() {

        Logger.info(this, "Refreshing system macros");
        refreshSystemMacros();

        Logger.info(this, "Refreshing dot_velocity_macros.vtl macros");
        refreshDotVelocityMacros();
    }

    public void refreshSystemMacros() {

        final String [] systemVelocityMacros = Config.getStringArrayProperty(RuntimeConstants.VM_LIBRARY,
                SystemProperties.getArray(RuntimeConstants.VM_LIBRARY));

        if (UtilMethods.isSet(systemVelocityMacros)) {

            final RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();

            for (final String systemVelocityMacro : systemVelocityMacros) {

                Logger.debug(this, ()-> "Refreshing the macro cache: " + systemVelocityMacro);
                runtimeServices.getTemplate(systemVelocityMacro);
            }
        }
    }

    public void refreshDotVelocityMacros() {

        final List<Host> hostList = Try.of(()->APILocator.getHostAPI().findAllFromCache(
                APILocator.systemUser(), false)).getOrNull();
        if (UtilMethods.isSet(hostList)) {

            for (final Host site: hostList) {

                final String query =  String.format(VELOCITY_MACRO_CACHE_REFRESH_QUERY,
                        site.getIdentifier());

                Logger.debug(this.getClass().getName(), ()-> "Query: " + query);

                final List<Contentlet> contentletToProcess = Try.of(() -> APILocator.getContentletAPI().search(
                        query, VELOCITY_MACRO_CACHE_REFRESH_QUERY_CONTENTLETS_LIMIT, 0, null, APILocator.systemUser(), false)).getOrElse(Collections.emptyList());

                for (final Contentlet contentlet: contentletToProcess) {

                    final FileAsset fileAsset = APILocator.getFileAssetAPI().fromContentlet(contentlet);
                    try (InputStream in = fileAsset.getInputStream()) {

                        final String velocityCode = IOUtils.toString(in, Charset.defaultCharset());
                        VelocityUtil.getInstance().parseVelocity(velocityCode, com.dotmarketing.util.VelocityUtil.getBasicContext());
                    } catch (IOException e) {

                        Logger.error(this, e.getMessage());
                        Logger.debug(this, e.getMessage(), e);
                    }
                }
            }
        }
    }
}
