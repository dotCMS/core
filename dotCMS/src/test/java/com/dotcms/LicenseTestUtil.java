package com.dotcms;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.repackage.org.apache.commons.httpclient.NameValuePair;
import com.dotcms.repackage.org.apache.commons.httpclient.methods.PostMethod;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpClient;
import org.mockito.stubbing.Answer;
import org.mockito.Mockito;
import com.dotcms.enterprise.LicenseUtil;

/*
 * Util class created to emulate the get license form request
 * should only be used inside the test suite. 
 */
public class LicenseTestUtil extends UnitTestBase {
	
	public static void getLicense() throws Exception{
		
		if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {
			String license;
			HttpServletRequest req=Mockito.mock(HttpServletRequest.class);
			Mockito.when(req.getParameter("iwantTo")).thenReturn("request_code");
			Mockito.when(req.getParameter("license_type")).thenReturn("trial");
			Mockito.when(req.getParameter("license_level")).thenReturn(String.valueOf(LicenseLevel.PRIME.level));

			final StringBuilder reqcode=new StringBuilder();

			Mockito.doAnswer(new Answer() {
			    public Object answer(org.mockito.invocation.InvocationOnMock invocation) throws Throwable {
			        reqcode.append(invocation.getArguments()[1].toString());
			        return null;
			    }
			}).when(req).setAttribute(Mockito.eq("requestCode"),Mockito.any(String.class));

			LicenseUtil.processForm(req);
			
			HttpClient client=new HttpClient();
			PostMethod post=new PostMethod("https://my.dotcms.com/app/licenseRequest3");
			post.setRequestBody(new NameValuePair[] { new NameValuePair("code", reqcode.toString()) });
			client.executeMethod(post);
			
			if(post.getStatusCode()==200){
				license = post.getResponseBodyAsString();
				HttpServletRequest req2=Mockito.mock(HttpServletRequest.class);
				Mockito.when(req2.getParameter("iwantTo")).thenReturn("paste_license");
				Mockito.when(req2.getParameter("license_text")).thenReturn(license);
				LicenseUtil.processForm(req2);
			}
		} 
	}
}
