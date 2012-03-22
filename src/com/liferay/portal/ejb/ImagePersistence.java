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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.hibernate.HibernateException;
import net.sf.hibernate.ObjectNotFoundException;
import net.sf.hibernate.Session;

import com.liferay.portal.NoSuchImageException;
import com.liferay.portal.SystemException;
import com.liferay.portal.util.HibernateUtil;

/**
 * <a href="ImagePersistence.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.9 $
 *
 */
public class ImagePersistence extends BasePersistence {
	protected com.liferay.portal.model.Image create(String imageId) {
		return new com.liferay.portal.model.Image(imageId);
	}

	protected com.liferay.portal.model.Image remove(String imageId)
		throws NoSuchImageException, SystemException {
		Session session = null;

		try {
			session = openSession();

			ImageHBM imageHBM = (ImageHBM)session.load(ImageHBM.class, imageId);
			com.liferay.portal.model.Image image = ImageHBMUtil.model(imageHBM);
			session.delete(imageHBM);
			session.flush();
			ImagePool.remove(imageId);

			return image;
		}
		catch (HibernateException he) {
			if (he instanceof ObjectNotFoundException) {
				throw new NoSuchImageException(imageId.toString());
			}
			else {
				throw new SystemException(he);
			}
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected com.liferay.portal.model.Image update(
		com.liferay.portal.model.Image image) throws SystemException {
		Session session = null;

		try {
			if (image.isNew() || image.isModified()) {
				session = openSession();

				if (image.isNew()) {
					ImageHBM imageHBM = new ImageHBM(image.getImageId(),
							image.getText());
					session.save(imageHBM);
					session.flush();
				}
				else {
					try {
						ImageHBM imageHBM = (ImageHBM)session.load(ImageHBM.class,
								image.getPrimaryKey());
						imageHBM.setText(image.getText());
						session.flush();
					}
					catch (ObjectNotFoundException onfe) {
						ImageHBM imageHBM = new ImageHBM(image.getImageId(),
								image.getText());
						session.save(imageHBM);
						session.flush();
					}
				}

				image.setNew(false);
				image.setModified(false);
				image.protect();
				ImagePool.remove(image.getPrimaryKey());
				ImagePool.put(image.getPrimaryKey(), image);
			}

			return image;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected com.liferay.portal.model.Image findByPrimaryKey(String imageId)
		throws NoSuchImageException, SystemException {
		com.liferay.portal.model.Image image = ImagePool.get(imageId);
		Session session = null;

		try {
			if (image == null) {
				session = openSession();

				ImageHBM imageHBM = (ImageHBM)session.load(ImageHBM.class,
						imageId);
				image = ImageHBMUtil.model(imageHBM);
			}

			return image;
		}
		catch (HibernateException he) {
			if (he instanceof ObjectNotFoundException) {
				throw new NoSuchImageException(imageId.toString());
			}
			else {
				throw new SystemException(he);
			}
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected List findAll() throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append("FROM Image IN CLASS com.liferay.portal.ejb.ImageHBM ");
			query.append("ORDER BY ");
			query.append("imageId ASC");

			Iterator itr = session.find(query.toString()).iterator();
			List list = new ArrayList();

			while (itr.hasNext()) {
				ImageHBM imageHBM = (ImageHBM)itr.next();
				list.add(ImageHBMUtil.model(imageHBM));
			}

			return list;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}
}