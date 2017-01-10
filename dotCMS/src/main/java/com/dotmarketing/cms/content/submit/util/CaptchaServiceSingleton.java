package com.dotmarketing.cms.content.submit.util;

import com.dotcms.repackage.com.octo.captcha.service.sound.DefaultManageableSoundCaptchaService;
import com.dotcms.repackage.com.octo.captcha.service.sound.SoundCaptchaService;

public class CaptchaServiceSingleton {
    
    private static SoundCaptchaService instance = new DefaultManageableSoundCaptchaService();
    
    public static SoundCaptchaService getInstance(){
        return instance;
    }
}