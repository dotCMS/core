package com.dotcms.api.di.module;

import com.dotcms.api.di.test.TestDiResource;
import com.dotcms.util.I18NUtil;
import com.dotcms.util.MessageAPI;
import com.dotcms.util.MessageAPIFactory;
import com.dotcms.util.marshal.MarshalFactory;
import com.dotcms.util.marshal.MarshalUtils;
import com.dotcms.util.security.Encryptor;
import com.dotcms.util.security.EncryptorFactory;
import com.google.inject.Provides;

/**
 * Default Core module with so basic classes.
 * @author jsanca
 */
public class CoreModule extends DotModule {

    private final static String [] BASE_ASPECT_PACKAGES =
            new String[] {"com.dotcms.api.aop"};

    private final static String [] BASE_SERVICE_PACKAGES =
            new String[] {"com.dotcms.uuid.shorty", "com.dotcms.api.di.test"};

    public CoreModule() {
    }

    @Override
    public String[] packagesNames() {
        return BASE_SERVICE_PACKAGES;
    }

    @Override
    public Class<?>[] classes() {
        return new Class<?>[] {TestDiResource.class};
    }

    @Override
    protected void processCustomConfiguration() {

        this.scanAspects(BASE_ASPECT_PACKAGES);
    }

    // todo: this is a high coupling, should be reduce by the
    // possibility to wrap providers into the annotation BeanFactory( class = MarshallBeanFactory.class)
    @Provides protected MarshalUtils marshalUtils() {

        return MarshalFactory.getInstance().getMarshalUtils();
    }

    @Provides protected Encryptor encryptor () {

        return EncryptorFactory.getInstance().getEncryptor();
    }

    @Provides protected I18NUtil i18NUtil () {

        return I18NUtil.INSTANCE;
    }

    @Provides
    protected MessageAPI messageAPI () {
        return MessageAPIFactory.getInstance().getMessageService();
    }


} // E:O:F:CoreModule
