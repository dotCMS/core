package com.dotmarketing.cms.content.submit.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import nl.captcha.Captcha;

import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.octo.captcha.service.CaptchaServiceException;
import com.octo.captcha.service.sound.SoundCaptchaService;

/**
 * This util validate the captcha value
 * @author Oswaldo
 *
 */
public class CaptchaUtil {


	/**
	 * Return true if a valid image captcha have been set
	 * @param request
	 * @return boolean
	 */
	public static boolean isValidImageCaptcha(HttpServletRequest request){

		HttpSession session = request.getSession();
		String captcha = request.getParameter("captcha");
		Captcha captchaObj = (Captcha) session.getAttribute(Captcha.NAME);
        String captchaSession=captchaObj!=null ? captchaObj.getAnswer() : null;
		if(!UtilMethods.isSet(captcha) || !UtilMethods.isSet(captchaSession) || !captcha.equals(captchaSession)){
			return false;
		}
		return true;

	} 

	/**
	 * Return true if a valid audio captcha have been set
	 * @param request
	 * @return boolean
	 */
	public static boolean isValidAudioCaptcha(HttpServletRequest request){

		HttpSession session = request.getSession();
		Captcha captcha = (Captcha) session.getAttribute(Captcha.NAME);
        String captchaSession=captcha!=null ? captcha.getAnswer() : null;
		Boolean isResponseCorrect = Boolean.FALSE;
		String captchaId = request.getSession().getId();  
		String audioCaptcha = request.getParameter("captcha");

		if(UtilMethods.isSet(audioCaptcha) && UtilMethods.isSet(captchaSession) && audioCaptcha.equals(captchaSession)){
			isResponseCorrect = Boolean.TRUE;
			session.removeAttribute(Captcha.NAME);
			
		}else if(UtilMethods.isSet(audioCaptcha) && UtilMethods.isSet(captchaId)){

			SoundCaptchaService soundCaptchaService = (SoundCaptchaService)session.getAttribute(WebKeys.SESSION_JCAPTCHA_SOUND_SERVICE);
			
			try {
				isResponseCorrect = soundCaptchaService.validateResponseForID(captchaId, audioCaptcha);
		
			} catch (CaptchaServiceException e) {
				Logger.error(CaptchaUtil.class, "An error ocurred trying to validate audio captcha", e);
			}
		}

		return isResponseCorrect;

	}


}
