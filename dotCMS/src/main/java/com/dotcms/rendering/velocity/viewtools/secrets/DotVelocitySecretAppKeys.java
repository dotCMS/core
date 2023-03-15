package com.dotcms.rendering.velocity.viewtools.secrets;

/**
 * Defines the Keys for the dotVelocitySecretApp
 * @author jsanca
 */
public enum DotVelocitySecretAppKeys {
        TITLE("title");

       final public String key;

       DotVelocitySecretAppKeys(final String key){
            this.key = key;
        }

       public final static String APP_KEY = "dotVelocitySecretApp";
}
