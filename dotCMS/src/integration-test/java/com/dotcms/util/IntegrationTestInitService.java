package com.dotcms.util;

import com.dotcms.config.DotInitializationService;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.repackage.org.apache.struts.config.ModuleConfig;
import com.dotcms.repackage.org.apache.struts.config.ModuleConfigFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.util.Config;
import com.liferay.util.SystemProperties;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.concurrent.atomic.AtomicBoolean;
import org.mockito.Mockito;

/**
 * Sets up the web environment needed to execute integration tests without a server application
 * Created by nollymar on 9/29/16.
 */
public class IntegrationTestInitService {

    private static IntegrationTestInitService service = new IntegrationTestInitService();

    private static AtomicBoolean initCompleted;

    static {
        SystemProperties.getProperties();
    }

    private IntegrationTestInitService() {
        initCompleted = new AtomicBoolean(false);
    }

    public static IntegrationTestInitService getInstance() {
        return service;
    }

    public void init() throws Exception {

        if (!initCompleted.get()) {

            final String threadName = Thread.currentThread().getName();

            if ("Test worker".equalsIgnoreCase(threadName)) { // Called made from gradle

                RandomAccessFile randomAccessFile = new RandomAccessFile("blockingTests.txt", "rw");
                FileChannel fc = randomAccessFile.getChannel();

                try {
                    if (fc.size() > 0) {
                        internalInit();
                    } else {
                        try (FileLock fileLock = fc
                                .lock()) {//We just need one initial lock for the db creation

                            internalInit();

                            if (fc.size() == 0) {
                                writeToFile(fc);
                            }

                        }
                    }

                } finally {
                    CloseUtils.closeQuietly(fc, randomAccessFile);
                }
            } else {
                internalInit();
            }

        }

    }

    private void internalInit() throws Exception {

        if (!initCompleted.get()) {

            // FIXME: DELETE!!!
            // FIXME: DELETE!!!
            System.out.println();
            System.out.println(
                    "-------------------------------------------------------------");
            System.out.println(
                    "-------------------------------------------------------------");
            System.out.println(initCompleted.get());
            System.out.println(">>>> " + Thread.currentThread().getName());
            System.out.println(">>>> " + Thread.currentThread().getId());
            System.out.println(
                    "-------------------------------------------------------------");
            System.out.println(
                    "-------------------------------------------------------------");
            System.out.println();
            // FIXME: DELETE!!!
            // FIXME: DELETE!!!

            TestingJndiDatasource.init();
            ConfigTestHelper._setupFakeTestingContext();

            CacheLocator.init();
            FactoryLocator.init();
            APILocator.init();

            //Running the always run startup tasks
            StartupTasksUtil.getInstance().init();

            //For these tests fire the reindex immediately
            Config.setProperty("ASYNC_REINDEX_COMMIT_LISTENERS", false);
            Config.setProperty("ASYNC_COMMIT_LISTENERS", false);

            Config.setProperty("NETWORK_CACHE_FLUSH_DELAY", (long) 0);
            // Init other dotCMS services.
            DotInitializationService.getInstance().initialize();

            initCompleted.set(true);
        }
    }

    private void writeToFile(FileChannel fc) throws IOException {
        final String data = String.valueOf(System.currentTimeMillis());
        ByteBuffer buffer = ByteBuffer.wrap(data.getBytes());
        buffer.put(data.getBytes());
        buffer.flip();
        while (buffer.hasRemaining()) {
            fc.write(buffer);
        }
    }

    public void mockStrutsActionModule() {
        ModuleConfigFactory factoryObject = ModuleConfigFactory.createFactory();
        ModuleConfig config = factoryObject.createModuleConfig("");
        Mockito.when(Config.CONTEXT.getAttribute(Globals.MODULE_KEY)).thenReturn(config);
    }

}