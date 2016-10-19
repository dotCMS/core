/*
 * Created on Sep 30, 2004
 *
 */
package com.dotmarketing.cms.factories;

import java.security.Key;
import java.util.Random;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.Company;
import com.liferay.util.Encryptor;
import com.liferay.util.EncryptorException;

/**
 * @author will
 * 
 */
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
    
    public static String encryptString(String x){
        try{
            Company c = PublicCompanyFactory.getDefaultCompany();
            Logger.debug(PublicEncryptionFactory.class, "c:"+c);
            Key k = c.getKeyObj();
            return Encryptor.encrypt(k, x);
            
        }
        catch(EncryptorException e){
            throw new DotRuntimeException("Encryption Failed");
        }
        
    }
    
    public static String digestString(String x){
        if(x == null) return null;
        try{
            
            return Encryptor.digest(x);
            
        }
        catch(Exception e){
            Logger.debug(PublicEncryptionFactory.class, "", e);
            throw new DotRuntimeException("Encryption digest");
        }
    }
    
    public static String decryptString(String x){
        try{
            
            Key k = PublicCompanyFactory.getDefaultCompany().getKeyObj();
            return Encryptor.decrypt(k, x);
            
        }
        catch(EncryptorException e){
            Logger.debug(PublicEncryptionFactory.class, "", e);
            throw new DotRuntimeException("decryption Failed");
        }
    }
    
}
