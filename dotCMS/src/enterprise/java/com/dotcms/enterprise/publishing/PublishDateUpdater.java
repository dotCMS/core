/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

package com.dotcms.enterprise.publishing;

import static com.dotcms.content.elasticsearch.business.ESMappingAPIImpl.publishExpireESDateTimeFormat;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.content.elasticsearch.constants.ESMappingConstants;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;


import com.liferay.util.StringUtil;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import graphql.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;


public class PublishDateUpdater {

    private final static String PUBLISH_LUCENE_QUERY = "+contentType:(%4$s) +live:false +deleted:false +%1$s:[1969-12-31t18:00:00-0000 TO %3$s] +%2$s:[%3$s TO 3000-12-31t18:00:00-0000]";
    private final static String UNPUBLISH_LUCENE_QUERY = "+live:true +deleted:false +%s:[1969-12-31t18:00:00-0000 TO %s]";

    private final static String GET_CONTENT_TYPE_WITH_PUBLISH_FIELD = "SELECT velocity_var_name from structure where publish_date_var is not null AND publish_date_var != ''";

    @VisibleForTesting
    public static List<String> getContentTypeVariableWithPublishField() throws DotDataException {
        return new DotConnect()
                .setSQL(GET_CONTENT_TYPE_WITH_PUBLISH_FIELD)
                .loadObjectResults()
                .stream()
                .map(contentTypeMap -> contentTypeMap.get("velocity_var_name"))
                .filter(Objects::nonNull)
                .map(velocityVarBameObject -> velocityVarBameObject.toString())
                .collect(Collectors.toList());
    }

    @WrapInTransaction
    public static void updatePublishExpireDates(final Date fireTime) throws DotDataException, DotSecurityException {

	    if(LicenseUtil.getLevel()< LicenseLevel.PROFESSIONAL.level){
	        return;
	    }


        final User systemUser = APILocator.getUserAPI().getSystemUser();

        final List<String> contentTypeVariableWithPublishField = getContentTypeVariableWithPublishField();

        if (!contentTypeVariableWithPublishField.isEmpty()) {
            final String luceneQueryToPublish = getPublishLuceneQuery(fireTime, contentTypeVariableWithPublishField);

            final List<Contentlet> contentletToPublish = APILocator.getContentletAPI()
                    .search(luceneQueryToPublish, 0, 0,
                            null, systemUser, false);

            for (final Contentlet contentlet : contentletToPublish) {
                try {
                    APILocator.getContentletAPI().publish(contentlet, locker(contentlet), false);
                } catch (Exception e) {
                    Logger.debug(PublishDateUpdater.class,
                            "content failed to publish: " + e.getMessage());
                }
            }
        }

        final String luceneQueryToUnPublish = getExpireLuceneQuery(fireTime);

        final List<Contentlet> contentletToUnPublish = APILocator.getContentletAPI().search(luceneQueryToUnPublish,
                0, 0, null, systemUser, false);


        for(final Contentlet contentlet : contentletToUnPublish) {
            try {
                APILocator.getContentletAPI().unpublish(contentlet, locker(contentlet), false);
            }
            catch(Exception e){
                Logger.debug(PublishDateUpdater.class, "content failed to publish: " +  e.getMessage());
            }
        }
    }

    public static String getPublishLuceneQuery(final Date date,
            final List<String> contentTypeVariableWithPublishField) {

        final String time = publishExpireESDateTimeFormat.get().format(date);
        return getLuceneQuery(PUBLISH_LUCENE_QUERY,
                ESMappingConstants.PUBLISH_DATE,
                ESMappingConstants.EXPIRE_DATE,
                time,
                StringUtils.join(contentTypeVariableWithPublishField, " OR "));
    }

    public static String getExpireLuceneQuery(final Date date) {
        return getLuceneQuery(UNPUBLISH_LUCENE_QUERY, ESMappingConstants.EXPIRE_DATE, publishExpireESDateTimeFormat.get().format(date));
    }

    private static String getLuceneQuery(final String luceneQueryTemplate, final Object ... parameters) {
        return String.format(luceneQueryTemplate, parameters );
    }

    /**
     *
     * @param contentlet
     * @return
     * @throws DotDataException
     */
    private static User locker(final Contentlet contentlet) throws DotDataException {

        User locker = APILocator.getUserAPI().getSystemUser();
        try {
            User modUser = APILocator.getUserAPI()
                    .loadUserById(contentlet.getModUser(), locker, false);
            if (APILocator.getContentletAPI().canLock(contentlet, locker)) {
                locker = modUser;
            }
        } catch (Exception userEx) {
            Logger.error(PublishDateUpdater.class, userEx.getMessage(), userEx);
        }

        return locker;
    }

}
