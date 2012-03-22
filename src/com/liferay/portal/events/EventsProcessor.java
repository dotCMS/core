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

import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.liferay.portal.struts.Action;
import com.liferay.portal.struts.ActionException;
import com.liferay.portal.struts.SessionAction;
import com.liferay.portal.struts.SimpleAction;
import com.liferay.util.CollectionFactory;
import com.liferay.util.InstancePool;
import com.liferay.util.Validator;

/**
 * <a href="EventsProcessor.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.9 $
 *
 */
public class EventsProcessor {

    public static void process(String[] classes) throws ActionException {
		_getInstance()._process(classes, null, null, null, null, false);
	}

    public static void process(String[] classes, String[] ids)
    	throws ActionException {

		_getInstance()._process(classes, ids, null, null, null, false);
	}

    public static void process(String[] classes, HttpSession ses)
    	throws ActionException {

		_getInstance()._process(classes, null, null, null, ses, false);
	}

    public static void process(String[] classes, HttpServletRequest req,
    						   HttpServletResponse res)
    	throws ActionException {

		_getInstance()._process(classes, null, req, res, null, false);
	}

    public static void process(String[] classes, boolean single)
    	throws ActionException {

		_getInstance()._process(classes, null, null, null, null, single);
	}

    public static void process(String[] classes, HttpSession ses,
    						   boolean single)
    	throws ActionException {

		_getInstance()._process(classes, null, null, null, ses, single);
	}

    public static void process(String[] classes, HttpServletRequest req,
    						   HttpServletResponse res, boolean single)
    	throws ActionException {

		_getInstance()._process(classes, null, req, res, null, single);
	}

	private static EventsProcessor _getInstance() {
		if (_instance == null) {
			synchronized (EventsProcessor.class) {
				if (_instance == null) {
					_instance = new EventsProcessor();
				}
			}
		}

		return _instance;
	}

	private EventsProcessor() {
		_processPool = CollectionFactory.getHashSet();
	}

    private void _process(String[] classes, String[] ids,
    					  HttpServletRequest req, HttpServletResponse res,
    					  HttpSession ses, boolean single)
    	throws ActionException {

		if ((classes == null) || (classes.length == 0)) {
			return;
		}

		for (int i = 0; i < classes.length; i++) {
			String className = classes[i];

			if (Validator.isNotNull(className)) {
				if (single) {
					synchronized (_processPool) {
						if (_processPool.contains(className)) {
							break;
						}
						else {
							_processPool.add(className);
						}
					}
				}

				Object obj = InstancePool.get(classes[i]);

				if (obj instanceof Action) {
					Action a = (Action)obj;

					try {
						a.run(req, res);
					}
					catch (Exception e) {
						throw new ActionException(e);
					}
				}
				else if (obj instanceof SessionAction) {
					SessionAction sa = (SessionAction)obj;

					try {
						sa.run(ses);
					}
					catch (Exception e) {
						throw new ActionException(e);
					}
				}
				else if (obj instanceof SimpleAction) {
					SimpleAction sa = (SimpleAction)obj;

					try {
						sa.run(ids);
					}
					catch (Exception e) {
						throw new ActionException(e);
					}
				}
			}
		}
	}

	private static EventsProcessor _instance;

	private Set _processPool;

}