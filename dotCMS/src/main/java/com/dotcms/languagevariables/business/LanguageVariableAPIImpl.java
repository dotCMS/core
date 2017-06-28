package com.dotcms.languagevariables.business;

import java.util.List;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.keyvalue.business.KeyValueAPI;
import com.dotcms.keyvalue.model.KeyValue;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.portal.model.User;

/**
 * 
 * @author Jose Castro
 * @version 4.2.0
 * @since Jun 27, 2017
 *
 */
public class LanguageVariableAPIImpl implements LanguageVariableAPI {

    final private KeyValueAPI keyValueAPI;
    final private LanguageAPI languageAPI;
    
    public LanguageVariableAPIImpl() {
        this(APILocator.getKeyValueAPI(), APILocator.getLanguageAPI());
    }
    
    public LanguageVariableAPIImpl(KeyValueAPI keyValueAPI, LanguageAPI languageAPI) {
        this.keyValueAPI = keyValueAPI;
        this.languageAPI = languageAPI;
    }

    @Override
    public String get(String key, long languageId, final User user, final boolean respectFrontEnd) {
        try {
            ContentType langVarContentType = APILocator.getContentTypeAPI(user).find("Languagevariable");
            KeyValue keyValue = this.keyValueAPI.get(key, languageId, langVarContentType, user, respectFrontEnd);
            if (null != keyValue) {
                return keyValue.getValue();
            } else {
                //Language language = this.languageAPI.getLanguage(languageId);
                //this.languageAPI.getLanguage("");
                
                // WIP: Find in generic language (without country code).
            }
        } catch (DotDataException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (DotSecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

}
