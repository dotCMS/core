package com.dotmarketing.servlets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.octo.captcha.component.sound.soundconfigurator.FreeTTSSoundConfigurator;
import com.octo.captcha.component.sound.soundconfigurator.SoundConfigurator;
import com.octo.captcha.component.sound.wordtosound.FreeTTSWordToSound;
import com.octo.captcha.component.sound.wordtosound.WordToSound;
import com.octo.captcha.component.word.worddecorator.SpellerWordDecorator;
import com.octo.captcha.component.word.wordgenerator.DummyWordGenerator;
import com.octo.captcha.component.word.wordgenerator.WordGenerator;
import com.octo.captcha.service.CaptchaServiceException;
import com.octo.captcha.service.sound.DefaultManageableSoundCaptchaService;
import com.octo.captcha.service.sound.SoundCaptchaService;
import com.octo.captcha.sound.SoundCaptcha;
import com.octo.captcha.sound.SoundCaptchaFactory;
import com.octo.captcha.sound.speller.SpellerSoundFactory;


public class AudioCaptchaServlet extends HttpServlet {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

    private static String voicePackage = "com.sun.speech.freetts.en.us.cmu_time_awb.AlanVoiceDirectory,com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory";
	
    private static String voiceName = "kevin16";
    
	public void init(ServletConfig servletConfig) throws ServletException {

    }

	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		byte[] captchaChallengeSound = null;
		ByteArrayOutputStream soundOutputStream = new ByteArrayOutputStream();
		AudioInputStream challenge = null;
		try {

			String captchaSession =  (String) request.getSession().getAttribute(nl.captcha.servlet.Constants.SIMPLE_CAPCHA_SESSION_KEY);	

			if(UtilMethods.isSet(captchaSession)){
				
				/*If we have a normal captcha in the session we should generate the word in the session instead of using
				  a custom sound challenge*/
				WordGenerator word = new DummyWordGenerator(captchaSession.trim());
				SoundConfigurator configurator = new FreeTTSSoundConfigurator(voiceName, voicePackage, 1.0f, 100, 100);
				WordToSound word2sound = new FreeTTSWordToSound(configurator, captchaSession.length(), captchaSession.length());
				SoundCaptchaFactory factory = new SpellerSoundFactory(word, word2sound, new SpellerWordDecorator(";"));
				SoundCaptcha tCaptcha = factory.getSoundCaptcha();
				challenge = (AudioInputStream)tCaptcha.getChallenge();
			
			}else{

				//get the session id that will identify the generated captcha. 
				//the same id must be used to validate the response, the session id is a good candidate!
				//Look for the captcha parameter from the session if it's null create a new audio challenge 	
				String captchaId = request.getSession().getId();
				//challenge = CaptchaServiceSingleton.getInstance().getSoundChallengeForID(captchaId);
				
				SoundCaptchaService soundCaptchaService = new DefaultManageableSoundCaptchaService();
				challenge = soundCaptchaService.getSoundChallengeForID(captchaId);
				request.getSession().setAttribute(WebKeys.SESSION_JCAPTCHA_SOUND_SERVICE, soundCaptchaService);
			}

			AudioSystem.write(challenge, AudioFileFormat.Type.WAVE, soundOutputStream);
			soundOutputStream.flush();
			soundOutputStream.close();

		} catch (IllegalArgumentException e) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		} catch (CaptchaServiceException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		captchaChallengeSound = soundOutputStream.toByteArray();
		response.setHeader("Content-Length", "" + captchaChallengeSound.length);
		response.setHeader("Cache-Control", "no-store");
		response.setHeader("Pragma", "no-cache");
		response.setDateHeader("Expires", 0L);
		response.setContentType("audio/x-wav");

		ServletOutputStream responseOutputStream = response.getOutputStream();
		responseOutputStream.write(captchaChallengeSound);
		responseOutputStream.flush();
		responseOutputStream.close();
		return;
	}
}
