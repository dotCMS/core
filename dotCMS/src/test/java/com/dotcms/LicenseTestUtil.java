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

			license = "sZ+kjQ1zFpw/J4lMkyzGfn4sgaCZ//jGqObjzeg/pn3a8YCzYKlVpaqyLA7WT858vNTMBTu6RGOGf+RIjVwezWt1FOn7svkTC1JeGu+rrryECD19spxq1iaWinXSJAwV9g14s3GKwXPmI8Tvnr+RXFDP0O/xS9Y5A9pdRsY6KCEAAAAIRGV2IFRlc3QAAAAIAAABWbfTJtkAAAAIAAAAAAAAAAAAAAAEAAAB9AAAACQ0OTQ4Zjc3Mi1mZWJjLTQ1ZWYtYTMzMi01NDU4NDc2ZmVkMGMAAAAEcHJvZAAAAAQAAAABAAAABAAAAAAAAAAEAAABLAAAACQzNTVlZTIzNy1kM2M3LTQzMjQtODlkOC1iNTBhNTIzNjg2NWM=";
			HttpServletRequest req2=Mockito.mock(HttpServletRequest.class);
			Mockito.when(req2.getParameter("iwantTo")).thenReturn("paste_license");
			Mockito.when(req2.getParameter("license_text")).thenReturn(license);
			LicenseUtil.processForm(req2);
		}
	}
}
