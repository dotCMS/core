package com.dotcms.util.security;

import java.io.Serializable;
import java.security.Key;
import java.security.Provider;

import com.dotcms.util.ReflectionUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.EncryptorException;

/**
 * Encryptor Factory
 * 
 * @author jsanca
 * @version 3.7
 * @since Jun 20, 2016
 */
public class EncryptorFactory implements Serializable {

	/**
	 * Used to keep the instance of the Encryptor. Should be volatile to avoid
	 * thread-caching
	 */
    private volatile Encryptor encryptor = null;

	/**
	 * Get the encryptor implementation from the dotmarketing-config.properties
	 */
    public static final String ENCRYPTOR_IMPLEMENTATION_KEY = "encryptor.implementation";

    private EncryptorFactory() {
        // singleton
    }

    private static class SingletonHolder {
        private static final EncryptorFactory INSTANCE = new EncryptorFactory();
    }

	/**
	 * Get the instance.
	 * 
	 * @return EncryptorFactory
	 */
    public static EncryptorFactory getInstance() {

        return EncryptorFactory.SingletonHolder.INSTANCE;
    } // getInstance.

	/**
	 * Get the implementation of the encryptor based on the configuration,
	 * otherwise will use the default one.
	 * 
	 * @return Encryptor
	 */
    public Encryptor getEncryptor () {

        String encryptorFactoryClass = null;

        if (null == this.encryptor) {

            synchronized (EncryptorFactory.class) {

                if (null == this.encryptor) {

                    encryptorFactoryClass =
                            Config.getStringProperty
                                    (ENCRYPTOR_IMPLEMENTATION_KEY, null);

                    if (UtilMethods.isSet(encryptorFactoryClass)) {

                        if (Logger.isDebugEnabled(EncryptorFactory.class)) {

                            Logger.debug(EncryptorFactory.class,
                                    "Using the encryptor class: " + encryptorFactoryClass);
                        }

                        this.encryptor =
                                (Encryptor) ReflectionUtils.newInstance(encryptorFactoryClass);

                        if (null == this.encryptor) {

                            if (Logger.isDebugEnabled(EncryptorFactory.class)) {

                                Logger.debug(EncryptorFactory.class,
                                        "Could not used this class: " + encryptorFactoryClass +
                                                ", using the default implementations");
                            }

                            this.encryptor =
                                    new EncryptorFactory.EncryptorImpl();
                        }
                    } else {

                        this.encryptor =
                                new EncryptorFactory.EncryptorImpl();
                    }
                }
            }
        }

        return this.encryptor;
    }

	/**
	 * Default implementation
	 * 
	 * @author jsanca
	 */
    private final class EncryptorImpl implements Encryptor {

        @Override
        public Key generateKey() throws EncryptorException {

            return Encryptor.super.generateKey();
        }

        @Override
        public Key generateKey(final String algorithm) throws EncryptorException {

            return Encryptor.super.generateKey(algorithm);
        }

        @Override
        public Provider getProvider() throws ClassNotFoundException, IllegalAccessException, InstantiationException {

            return Encryptor.super.getProvider();
        }

        @Override
        public String decrypt(final Key key, final String encryptedString) throws EncryptorException {

            return Encryptor.super.decrypt(key, encryptedString);
        }

        @Override
        public String digest(String text) {

            return Encryptor.super.digest(text);
        }

        @Override
        public String digest(final String algorithm, final String text) {

            return Encryptor.super.digest(algorithm, text);
        }

        @Override
        public String encrypt(final Key key, final String plainText) throws EncryptorException {

            return Encryptor.super.encrypt(key, plainText);
        }

        @Override
        public String encryptString(final String x) {

            return Encryptor.super.encryptString(x);
        }

        @Override
        public String decryptString(final String x) {

            return Encryptor.super.decryptString(x);
        }
    }

} // E:O:F:EncryptorFactory.
