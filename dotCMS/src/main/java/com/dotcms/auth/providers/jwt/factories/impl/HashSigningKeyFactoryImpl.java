package com.dotcms.auth.providers.jwt.factories.impl;

import com.dotcms.auth.providers.jwt.factories.SigningKeyFactory;
import com.dotmarketing.util.Config;
import com.liferay.util.Base64;
import java.security.Key;

/**
 * Default implementation of the {@link SigningKeyFactory}. This class provides a functional secure
 * key that can be used to generate the JWT.
 *
 * @author jsanca
 * @version 3.7
 * @since Jun 23, 2016
 */
public class HashSigningKeyFactoryImpl implements SigningKeyFactory {

  @Override
  public Key getKey() {
    final String hashKey =
        Config.getStringProperty(
            "json.web.token.hash.signing.key",
            "rO0ABXNyABRqYXZhLnNlY3VyaXR5LktleVJlcL35T7OImqVDAgAETAAJYWxnb3JpdGhtdAASTGphdmEvbGFuZy9TdHJpbmc7WwAHZW5jb2RlZHQAAltCTAAGZm9ybWF0cQB+AAFMAAR0eXBldAAbTGphdmEvc2VjdXJpdHkvS2V5UmVwJFR5cGU7eHB0AANERVN1cgACW0Ks8xf4BghU4AIAAHhwAAAACBksSlj3ReywdAADUkFXfnIAGWphdmEuc2VjdXJpdHkuS2V5UmVwJFR5cGUAAAAAAAAAABIAAHhyAA5qYXZhLmxhbmcuRW51bQAAAAAAAAAAEgAAeHB0AAZTRUNSRVQ=");
    return (Key) Base64.stringToObject(hashKey);
  }
}
