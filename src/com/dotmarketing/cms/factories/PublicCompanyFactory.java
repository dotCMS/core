/**
 * Copyright (c) 2000-2004 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.dotmarketing.cms.factories;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.CompanyUtil;
import com.liferay.portal.ejb.ImageManagerUtil;
import com.liferay.portal.model.Company;
import com.liferay.util.FileUtil;

/**
 * <a href="AddressUtil.java.html"><b><i>View Source</i></b></a>
 * 
 * @author Brian Wing Shun Chan
 * @version $Revision: 1.1 $
 * 
 */
public class PublicCompanyFactory extends CompanyUtil {

	public static Company getDefaultCompany() {
		try {

			return findByPrimaryKey(getDefaultCompanyId());

		} catch (Exception e) {
			throw new DotRuntimeException("No Company!");

		}
	}

	public static String getDefaultCompanyId() {
		try {
			ServletContext c = Config.CONTEXT;
			return c.getInitParameter("company_id");

		} catch (Exception e) {
			throw new DotRuntimeException("No Default Company Id!");

		}
	}
	
	

	public static List getCompanies() {
			
			try {
				return findAll();
			} catch (SystemException e) {
				// TODO Auto-generated catch block
				Logger.error(PublicCompanyFactory.class,e.getMessage(),e);
			}
			return new ArrayList();
	
	}
	

	/*
	 * This method runs the first time a server is started. It creates the
	 * default company and default logo.
	 * 
	 */
	public static void createDefaultCompany() {
		try {
			Company c = getDefaultCompany();
			c.setPortalURL("localhost");
			c
					.setKey("rO0ABXNyABRqYXZhLnNlY3VyaXR5LktleVJlcL35T7OImqVDAgAETAAJYWxnb3JpdGhtdAASTGphdmEvbGFuZy9TdHJpbmc7WwAHZW5jb2RlZHQAAltCTAAGZm9ybWF0cQB+AAFMAAR0eXBldAAbTGphdmEvc2VjdXJpdHkvS2V5UmVwJFR5cGU7eHB0AANERVN1cgACW0Ks8xf4BghU4AIAAHhwAAAACBksSlj3ReywdAADUkFXfnIAGWphdmEuc2VjdXJpdHkuS2V5UmVwJFR5cGUAAAAAAAAAABIAAHhyAA5qYXZhLmxhbmcuRW51bQAAAAAAAAAAEgAAeHB0AAZTRUNSRVQ=");
			c.setHomeURL("localhost");
			c.setMx("dotcms.com");
			c.setName("dotcms.com");
			c.setShortName("dotcms.com");
			c.setType("biz");
			c.setSize("100");
			c.setState("FL");
			c.setStreet("3059 Grand Ave.");
			c.setCity("Miami");
			c.setZip("33133");
			c.setPhone("3058581422");
			c.setEmailAddress("support@dotcms.com");
			c.setAuthType("emailAddress");
			c.setStrangers(false);
			c.setAutoLogin(true);
			c.setModified(true);

			CompanyUtil.update(c);

			/* Set the DM logo */
			File f = new File(FileUtil.getRealPath("/html/images/shim.gif"));

			BufferedInputStream in = new BufferedInputStream(new FileInputStream(f));

			ByteArrayOutputStream baout = new ByteArrayOutputStream();

			byte[] buf = new byte[2048];
			int i = 0;
			while ((i = in.read(buf)) != -1) {
				baout.write(buf, 0, i);
			}

			in.close();

			ImageManagerUtil.updateImage("dotcms.org", baout.toByteArray());

		} catch (Exception e) {
			Logger.debug(PublicCompanyFactory.class,e.getMessage());
		}

	}

}