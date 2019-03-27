/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
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

package com.liferay.portal.pwd;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.LocaleUtil;
import com.liferay.util.PwdGenerator;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import org.apache.oro.text.perl.Perl5Util;

/**
 * <a href="RegExpToolkit.java.html"><b><i>View Source</i></b></a>
 *
 * @author Brian Wing Shun Chan
 * @version $Revision: 1.4 $
 */
public class RegExpToolkit extends BasicToolkit {

	private String _pattern;
	
    public RegExpToolkit () {
        _pattern = PropsUtil.get( PropsUtil.PASSWORDS_REGEXPTOOLKIT_PATTERN );
    }

    public RegExpToolkit (String pattern) {
        _pattern = pattern;
    }

    public String generate () {
        return PwdGenerator.getPassword();
    }

    public boolean validate(String password) {
        Perl5Util util = new Perl5Util();

        return password == null ? false : util.match( _pattern, password );
    }

	/**
	 * Retrieves an error message from the {@code portal.properties} file based
	 * on a message key. This {@code portal.properties} file will reference a
	 * message key that <b>MUST BE</b> located in the
	 * {@code Language.properties} files containing the respective error message
	 * users will see. The main goal of this method is to provide users feedback
	 * related to, for example:
	 * <ul>
	 * <li>The password not meeting the required security policies.</li>
	 * <li>The password not being able to use because it cannot be recycled yet.
	 * </li>
	 * <li>The password not allowed when it is a word from the dictionary.</li>
	 * <li>Etc.</li>
	 * </ul>
	 * 
	 * @param msgKey
	 *            - The message key to the internationalized error message.
	 * @return The error message users will see explaining their respective
	 *         error.
	 */
	public String getErrorMessageFromConfig(String msgKey) {
		String messageKey = PropsUtil.get(msgKey);
		String msg = null;
		try {
			HttpServletRequest req = HttpServletRequestThreadLocal.INSTANCE.getRequest();
            Locale locale = LocaleUtil.getLocale(req);
            if(UtilMethods.isSet(locale)){
            	msg = LanguageUtil.get(locale, messageKey);
            }else{
            	User systemUser = APILocator.getUserAPI().getSystemUser();
            	msg = LanguageUtil.get(systemUser, messageKey);
            }
		} catch (DotDataException e) {
			msg = messageKey;
		} catch (LanguageException e) {
			msg = messageKey;
		}
		return msg;
	}

}