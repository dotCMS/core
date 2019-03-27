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

package com.liferay.portal.ejb;

import com.dotmarketing.util.Logger;
import com.liferay.portal.model.ModelListener;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.GetterUtil;
import com.liferay.util.InstancePool;
import com.liferay.util.Validator;

/**
 * <a href="ImageUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.83 $
 *
 */
public class ImageUtil {
	public static String PERSISTENCE = GetterUtil.get(PropsUtil.get(
				"value.object.persistence.com.liferay.portal.model.Image"),
			"com.liferay.portal.ejb.ImagePersistence");
	public static String LISTENER = GetterUtil.getString(PropsUtil.get(
				"value.object.listener.com.liferay.portal.model.Image"));

	protected static com.liferay.portal.model.Image create(
		java.lang.String imageId) {
		ImagePersistence persistence = (ImagePersistence)InstancePool.get(PERSISTENCE);

		return persistence.create(imageId);
	}

	protected static com.liferay.portal.model.Image remove(
		java.lang.String imageId)
		throws com.liferay.portal.NoSuchImageException, 
			com.liferay.portal.SystemException {
		ImagePersistence persistence = (ImagePersistence)InstancePool.get(PERSISTENCE);
		ModelListener listener = null;

		if (Validator.isNotNull(LISTENER)) {
			try {
				listener = (ModelListener)Class.forName(LISTENER).newInstance();
			}
			catch (Exception e) {
				Logger.error(ImageUtil.class,e.getMessage(),e);
			}
		}

		if (listener != null) {
			listener.onBeforeRemove(findByPrimaryKey(imageId));
		}

		com.liferay.portal.model.Image image = persistence.remove(imageId);

		if (listener != null) {
			listener.onAfterRemove(image);
		}

		return image;
	}

	protected static com.liferay.portal.model.Image update(
		com.liferay.portal.model.Image image)
		throws com.liferay.portal.SystemException {
		ImagePersistence persistence = (ImagePersistence)InstancePool.get(PERSISTENCE);
		ModelListener listener = null;

		if (Validator.isNotNull(LISTENER)) {
			try {
				listener = (ModelListener)Class.forName(LISTENER).newInstance();
			}
			catch (Exception e) {
				Logger.error(ImageUtil.class,e.getMessage(),e);
			}
		}

		boolean isNew = image.isNew();

		if (listener != null) {
			if (isNew) {
				listener.onBeforeCreate(image);
			}
			else {
				listener.onBeforeUpdate(image);
			}
		}

		image = persistence.update(image);

		if (listener != null) {
			if (isNew) {
				listener.onAfterCreate(image);
			}
			else {
				listener.onAfterUpdate(image);
			}
		}

		return image;
	}

	protected static com.liferay.portal.model.Image findByPrimaryKey(
		java.lang.String imageId)
		throws com.liferay.portal.NoSuchImageException, 
			com.liferay.portal.SystemException {
		ImagePersistence persistence = (ImagePersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByPrimaryKey(imageId);
	}

	protected static java.util.List findAll()
		throws com.liferay.portal.SystemException {
		ImagePersistence persistence = (ImagePersistence)InstancePool.get(PERSISTENCE);

		return persistence.findAll();
	}
}