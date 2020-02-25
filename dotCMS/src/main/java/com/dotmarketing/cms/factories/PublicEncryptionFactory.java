/*
 * Created on Sep 30, 2004
 *
 */
package com.dotmarketing.cms.factories;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.Company;
import com.liferay.util.Encryptor;
import com.liferay.util.EncryptorException;

import java.security.Key;
import java.util.Random;

/**
 * @author will
 * 
 */
@Deprecated
public class PublicEncryptionFactory {
    
    public static  String getRandomPassword(){
        // random number between 10000 AND 99999
        Random r = new Random();
        int passInt = Math.abs(r.nextInt() + 10000) % 100000;
        return String.valueOf(passInt);
    }
    
    public static  String getRandomEncryptedPassword(){
        return encryptString(getRandomPassword());
    }
    
    public static String encryptString(final String string) {
        try {

            final Company company = PublicCompanyFactory.getDefaultCompany();
            Logger.info(PublicEncryptionFactory.class, ()-> "c:" + company);
            final Key key = company.getKeyObj();
            return Encryptor.encrypt(key, string);
        } catch(EncryptorException e) {

            throw new DotRuntimeException("Encryption Failed", e);
        }
        
    }
    
    public static String digestString(final String string) {

        if(string == null) return null;

        try {
            
            return Encryptor.digest(string);
        } catch(Exception e) {

            Logger.debug(PublicEncryptionFactory.class, e.getMessage(), e);
            throw new DotRuntimeException("Encryption digest", e);
        }
    }
    
    public static String decryptString(final String string){
        try{
            
            final Key key = PublicCompanyFactory.getDefaultCompany().getKeyObj();
            return Encryptor.decrypt(key, string);
        } catch(EncryptorException e){
            
            Logger.debug(PublicEncryptionFactory.class, e.getMessage(), e);
            throw new DotRuntimeException("decryption Failed", e);
        }
    }
    
}
