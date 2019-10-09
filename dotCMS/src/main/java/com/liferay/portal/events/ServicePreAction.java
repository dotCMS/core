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

package com.liferay.portal.events;

import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.Action;
import com.liferay.portal.struts.ActionException;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.PropsUtil;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.GetterUtil;
import com.liferay.util.ListUtil;
import com.liferay.util.Validator;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * <a href="ServicePreAction.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @author  Felix Ventero
 * @version $Revision: 1.25 $
 *
 */
public class ServicePreAction extends Action {

  public void run(HttpServletRequest req, HttpServletResponse res) throws ActionException {

    try {
      HttpSession ses = req.getSession();

      User user = null;

      user = PortalUtil.getUser(req);

      boolean signedIn = false;

      if (user == null) {
        user = PortalUtil.getCompany(req).getDefaultUser();
      } else {
        signedIn = true;
      }

      Locale locale = (Locale) ses.getAttribute(Globals.LOCALE_KEY);

      if (locale == null) {
        if (signedIn) {
          locale = user.getLocale();
        } else {
          if (GetterUtil.getBoolean(PropsUtil.get(PropsUtil.LOCALE_DEFAULT_REQUEST))) {

            locale = req.getLocale();
          }

          if (locale == null) {
            locale = user.getLocale();
          }

          if (Validator.isNull(locale.getCountry())) {

            // Locales must contain the country code

            locale = LanguageUtil.getLocale(locale.getLanguage());
          }

          List availableLocales = ListUtil.fromArray(LanguageUtil.getAvailableLocales());

          if (!availableLocales.contains(locale)) {
            locale = user.getLocale();
          }
        }

        ses.setAttribute(Globals.LOCALE_KEY, locale);
      }
      final List<Layout> layouts = APILocator.getLayoutAPI().loadLayoutsForUser(user);
      final Layout layout = APILocator.getLayoutAPI().resolveLayout(req)
              .orElseGet(
                      () -> UtilMethods.isSet(layouts) ? layouts.get(0) : null
              );
      final Layout[] layoutsArray = layouts.stream().toArray(Layout[]::new);
      req.setAttribute(WebKeys.LAYOUT, layout);
      req.setAttribute(WebKeys.LAYOUTS, layoutsArray);
    } catch (Exception e) {
      throw new ActionException(e);
    }
  }
	
	

	
	

}