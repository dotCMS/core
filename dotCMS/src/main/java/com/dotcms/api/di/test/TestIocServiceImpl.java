package com.dotcms.api.di.test;

import com.dotcms.api.di.DotBean;
import com.dotcms.util.I18NUtil;
import com.dotcms.util.LogTime;
import com.dotcms.util.marshal.MarshalUtils;
import com.dotcms.util.security.Encryptor;
import com.dotcms.uuid.shorty.ShortyIdAPI;

import javax.inject.Inject;
import java.util.Locale;
import java.util.Map;

@DotBean
public class TestIocServiceImpl implements TestIocService {

    private final ShortyIdAPI  shortyIdAPI;
    private final MarshalUtils marshalUtils;
    private final Encryptor    encryptor;
    private final I18NUtil     i18NUtil;

    @Inject
    public TestIocServiceImpl(final ShortyIdAPI shortyIdAPI,
                              final MarshalUtils marshalUtils,
                              final Encryptor encryptor,
                              final I18NUtil i18NUtil) {

        this.shortyIdAPI = shortyIdAPI;
        this.marshalUtils = marshalUtils;
        this.encryptor = encryptor;
        this.i18NUtil = i18NUtil;
    }

    @LogTime
    @Override
    public String testMe() {

        final String shorty = this.shortyIdAPI.shortify("1234-5678-9102-345679");
        final String encryptShorty = this.encryptor.encryptString(shorty);
        final Map<String, String> map = i18NUtil.getMessagesMap(Locale.ENGLISH, "operation-timeout", "change-password");
        map.put("shorty", shorty);
        map.put("encryptShorty", encryptShorty);

        return this.marshalUtils.marshal(map);
    }
}
