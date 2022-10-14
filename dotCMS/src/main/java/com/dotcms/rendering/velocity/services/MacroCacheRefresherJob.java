package com.dotcms.rendering.velocity.services;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileEvent;
import com.dotmarketing.portlets.fileassets.business.FileListener;
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
public class MacroCacheRefresherJob implements Runnable, FileListener {

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

    /**
     * Refresh the cache of the system macros
     */
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
    } // refreshSystemMacros.

    /**
     * Refresh all user macros contained in files called: dot_velocity_macros.vtl
     */
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
                    this.refreshFileAsset(fileAsset);
                }
            }
        }
    } // refreshDotVelocityMacros.

    /**
     * Refresh the macros contained in the fileAsset
     * @param fileAsset {@link FileAsset}
     */
    public void refreshFileAsset (final FileAsset fileAsset) {

        try (InputStream in = fileAsset.getInputStream()) {

            final String velocityCode = IOUtils.toString(in, Charset.defaultCharset());
            VelocityUtil.getInstance().parseVelocity(velocityCode, com.dotmarketing.util.VelocityUtil.getBasicContext());
        } catch (IOException e) {

            final String msg = "On rendering the macro file asset: " + fileAsset.getIdentifier() + ", msg: " + e.getMessage();
            Logger.error(this,  msg);
            Logger.debug(this, msg, e);
        }
    }

    /**
     * When a dot_velocity_macros.vtl is being modified, refresh the macro inside
     * @param fileEvent {@link FileEvent}
     */
    @Override
    public void fileModify(final FileEvent fileEvent) {
        this.refreshFileAsset(fileEvent.getFileAsset());
    }
}
