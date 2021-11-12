package com.dotmarketing.listeners;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.content.elasticsearch.util.DotRestHighLevelClientProvider;
import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.util.AsciiArt;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.reindex.ReindexThread;
import com.dotmarketing.loggers.Log4jUtil;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;
import org.apache.felix.framework.OSGIUtil;
import org.apache.velocity.runtime.pool.commons.CommonsParserPool;
import org.apache.velocity.runtime.pool.commons.CommonsParserPoolImpl;

/**
 *
 * @author Andres Olarte
 *
 */
public class ContextLifecycleListener implements ServletContextListener {

	public ContextLifecycleListener() {
	    AsciiArt.doArt();
	}

    public void contextDestroyed(ServletContextEvent arg0) {
        Logger.info(this, "Shutdown : Started, executing a clean shutdown.");

        Try.run(() -> OSGIUtil.getInstance().stopFramework())
                .onFailure(e -> Logger.warn(ContextLifecycleListener.class, "Shutdown : " + e.getMessage()));

        Try.run(QuartzUtils::stopSchedulers)
                        .onFailure(e -> Logger.warn(ContextLifecycleListener.class, "Shutdown : " + e.getMessage()));

        Try.run(LicenseUtil::freeLicenseOnRepo)
                        .onFailure(e -> Logger.warn(ContextLifecycleListener.class, "Shutdown : " + e.getMessage()));

        
        Try.run(() -> CacheLocator.getCacheAdministrator().shutdown())
                        .onFailure(e -> Logger.warn(ContextLifecycleListener.class, "Shutdown : " + e.getMessage()));
        


        Try.run(() -> DotConcurrentFactory.getInstance().shutdownAndDestroy())
                        .onFailure(e -> Logger.warn(ContextLifecycleListener.class, "Shutdown : " + e.getMessage()));

        Try.run(() -> ReindexThread.stopThread())
                        .onFailure(e -> Logger.warn(ContextLifecycleListener.class, "Shutdown : " + e.getMessage()));

        Try.run(() ->   DotRestHighLevelClientProvider.close())
                .onFailure(e -> Logger.warn(ContextLifecycleListener.class, "Shutdown : " + e.getMessage(),e));

        Logger.info(this, "Shutdown : Finished.");

        CommonsParserPoolImpl.shutdown();

        //dotCMSPool housekeeper
        //reindex-thread
        //commons-pool-evictor
        //FelixResolver

        //thread locals com.dotmarketing.util.RegEX$2
        //org.apache.logging.log4j.core.async.RingBufferLogEventTranslator
        //com.dotmarketing.util.RegEX$1
        //com.dotmarketing.db.HibernateUtil$1

    }

	public void contextInitialized(ServletContextEvent arg0) {

		Config.setMyApp(arg0.getServletContext());


        String path = null;
		try {

            String contextPath = Config.CONTEXT_PATH;
            if ( !contextPath.endsWith( File.separator ) ) {
                contextPath += File.separator;
            }
			File file = new File(contextPath + "WEB-INF" + File.separator + "log4j" + File.separator + "log4j2.xml");
			path = file.toURI().toString();

        } catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}

		//Initialises/reconfigures log4j based on a given log4j configuration file
		Log4jUtil.initializeFromPath(path);


	}

}
