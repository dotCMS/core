package com.dotmarketing.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

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
		String captchaSession =  (String) session.getAttribute(nl.captcha.servlet.Constants.SIMPLE_CAPCHA_SESSION_KEY);

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

		Boolean isResponseCorrect =Boolean.FALSE;
		String captchaId = request.getSession().getId();  
		String audioCaptcha = request.getParameter("audioCaptcha");
		
		if(UtilMethods.isSet(audioCaptcha) && UtilMethods.isSet(captchaId)){
			try {
				//isResponseCorrect = CaptchaServiceSingleton.getInstance().validateResponseForID(captchaId, audioCaptcha);
				
				SoundCaptchaService soundCaptchaService = (SoundCaptchaService) request.getSession().getAttribute(WebKeys.SESSION_JCAPTCHA_SOUND_SERVICE);
				isResponseCorrect = soundCaptchaService.validateResponseForID(captchaId, audioCaptcha);
				request.getSession().removeAttribute(WebKeys.SESSION_JCAPTCHA_SOUND_SERVICE);
			} catch (CaptchaServiceException e) {
				Logger.error(CaptchaUtil.class, "An error ocurred trying to validate audio captcha", e);
			}
		}

		return isResponseCorrect;

	}


}
