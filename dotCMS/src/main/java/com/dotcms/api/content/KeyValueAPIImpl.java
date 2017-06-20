package com.dotcms.api.content;

import java.util.List;

import com.dotcms.cache.KeyValueCache;
import com.dotcms.content.model.DefaultKeyValue;
import com.dotcms.content.model.KeyValue;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.KeyValueContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * Implementation class for the {@link KeyValueAPI}.
 * 
 * @author Jose Castro
 * @version 4.3.0
 * @since Jun 19, 2017
 *
 */
public class KeyValueAPIImpl implements KeyValueAPI {

	protected final ContentletAPI contentletAPI;
	protected final FolderAPI folderAPI;
	protected final UserAPI userAPI;
	
	/**
	 * Creates a new instance of the {@link KeyValueAPI}.
	 */
	public KeyValueAPIImpl() {
		this.contentletAPI = APILocator.getContentletAPI();
		this.folderAPI = APILocator.getFolderAPI();
		this.userAPI = APILocator.getUserAPI();
	}
	
	@Override
	public KeyValue fromContentlet(Contentlet contentlet) {
		if (null == contentlet) {
			throw new DotStateException("The contentlet cannot be null.");
		}
		try {
			if (!contentlet.getContentType().baseType().equals(BaseContentType.KEY_VALUE)) {
				throw new DotStateException(String.format("The contentlet with ID %s is not a KeyValue content.",
						contentlet.getIdentifier()));
			}
		} catch (DotSecurityException | DotDataException e) {
			throw new DotStateException(
					String.format("The contentlet with ID %s could not be identified as a KeyValue content.",
							contentlet.getIdentifier()));
		}
		DefaultKeyValue keyValue;
		KeyValueCache cache = CacheLocator.getKeyValueCache();
		keyValue = DefaultKeyValue.class.cast(cache.get(contentlet.getMap().get(KeyValueContentType.KEY_VALUE_KEY_FIELD_VAR).toString()));
		if (null != keyValue) {
			return keyValue;
		}
		keyValue = new DefaultKeyValue();
		keyValue.setContentTypeId(contentlet.getContentTypeId());
		try {
			this.contentletAPI.copyProperties(Contentlet.class.cast(keyValue), contentlet.getMap());
		} catch (DotRuntimeException | DotSecurityException e) {
			throw new DotStateException(
					String.format("Properties of Contentlet %s could not be copied to a KeyValue object.",
							contentlet.getIdentifier()),
					e);
		}
		keyValue.setHost(contentlet.getHost());
		if (UtilMethods.isSet(contentlet.getFolder())) {
			try {
				final Folder folder = this.folderAPI.find(contentlet.getFolder(), this.userAPI.getSystemUser(), Boolean.FALSE);
				keyValue.setFolder(folder.getInode());
			} catch (DotDataException | DotSecurityException e) {
				Logger.warn(this, String.format("Contentlet with ID %s coudl not be converted to a KeyValue object.",
						contentlet.getIdentifier()), e);
				keyValue = new DefaultKeyValue();
			}
		}
		return keyValue;
	}

	@Override
	public List<KeyValue> get(String key, User user, boolean respectFrontEnd) {
		return get(key, null, user, respectFrontEnd);
	}

	@Override
	public List<KeyValue> get(String key, ContentType contentType, User user, boolean respectFrontEnd) {
		return null;
	}

	@Override
	public KeyValue get(String key, long language, ContentType contentType, User user, boolean respectFrontEnd) {
		// TODO Auto-generated method stub
		return null;
	}

}
