package com.dotmarketing.business.portal;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.PortletContext;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotDataValidationException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.CompanyUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PropsUtil;
import com.liferay.portlet.PortletConfigImpl;
import com.liferay.portlet.PortletContextImpl;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.ServletContext;

public class PortletAPIImpl implements PortletAPI {

    String companyId = CompanyUtils.getDefaultCompany().getCompanyId();

    private ServletContext context;

    public PortletAPIImpl(PortletFactory portletFac, ServletContext context) {
        this.portletFac = portletFac;
        this.context = context;
    }

    public PortletAPIImpl() {
        this(new PortletFactoryImpl(), Config.CONTEXT);
    }

    final PortletFactory portletFac;

    @CloseDBIfOpened
    protected boolean hasPortletRights(final User user, final String pId) {
        boolean hasRights = false;
        try {
            return APILocator.getLayoutAPI().doesUserHaveAccessToPortlet(pId, user);
        } catch (Exception ex) {
            Logger.warn(this, "can't determine if user " + user.getUserId() + " has rights to portlet " + pId, ex);
            hasRights = false;
        }
        return hasRights;
    }

    public boolean hasUserAdminRights(User user) {
        return hasPortletRights(user, "users");
    }

    public boolean hasContainerManagerRights(User user) {
        return hasPortletRights(user, "containers");
    }

    public boolean hasTemplateManagerRights(User user) {
        return hasPortletRights(user, "templates");
    }

    @Override
    @CloseDBIfOpened
    public Portlet findPortlet(String portletId) {
        if(portletId==null) {
            return null;
        }
        return portletFac.findById(portletId);

    }

    @Override
    @WrapInTransaction
    public void deletePortlet(String portletId) {

        try {
            portletFac.deletePortlet(portletId);
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    @Override
    @WrapInTransaction
    public Portlet savePortlet(final Portlet portlet, final User user) throws DotDataException, LanguageException {

        if(!UtilMethods.isSet(portlet.getPortletId())) {
            throw new DotDataValidationException("Portlet Id is Required");
        }
        final String portletId = CONTENT_PORTLET_PREFIX + portlet.getPortletId();
        if (UtilMethods.isSet(findPortlet(portletId))) {
            throw new DotDataValidationException("Portlet Id already Exists");
        }
        if(!UtilMethods.isSet(portlet.getInitParams().get("name"))) {
            throw new DotDataValidationException("Portlet Name is Required");
        }
        if(!UtilMethods.isSet(portlet.getPortletClass())) {
            throw new DotDataValidationException("You cannot save a portlet without an implementing portletClass");
        }

        final List<String> contentTypes = checkContentTypes(portlet.getInitParams().get("contentTypes")).stream().map(ct -> ct.variable())
                .collect(Collectors.toList());
        final List<String> baseTypes = checkBaseTypes(portlet.getInitParams().get("baseTypes")).stream().map(bt -> bt.name())
                .collect(Collectors.toList());

        if (contentTypes.size() + baseTypes.size() == 0) {
            throw new DotDataValidationException("You must specify at least one baseType or Content Type");
        }

        final HashMap<String,String> newMap=new HashMap<>();
        newMap.putAll(portlet.getInitParams());
        newMap.put("portletSource", "db");

        //cleaning up whitespaces from content types and base types
        newMap.put("contentTypes", String.join(",", contentTypes));
        newMap.put("baseTypes", String.join(",", baseTypes));

        final Portlet newPortlet =  portletFac.insertPortlet(new Portlet(portletId, portlet.getPortletClass(),newMap));
        //Add Languague Variable
        Map<String, String> keys = ImmutableMap
                .of(com.dotcms.repackage.javax.portlet.Portlet.class.getPackage().getName()
                        + ".title." + portletId, newPortlet.getInitParams().get("name"));
        try {
            for (Language lang : APILocator.getLanguageAPI().getLanguages()) {
                APILocator.getLanguageAPI()
                        .saveLanguageKeys(lang, keys, new HashMap<>(), ImmutableSet.of());
            }
        } catch (DotDataException e) {
            Logger.warnAndDebug(this.getClass(), e.getMessage(), e);
        }

        SystemMessageEventUtil.getInstance().pushMessage(portletId + user.getUserId(), new SystemMessageBuilder()
                .setMessage(Try.of(() -> LanguageUtil.get(user.getLocale(), "custom.content.portlet.created"))
                        .getOrElse("Custom Content Created")).create(),list(user.getUserId()));

        return newPortlet;
    }



    @CloseDBIfOpened
    public void InitPortlets() throws SystemException {
        portletFac.getPortlets();
    }
    @Override
    @CloseDBIfOpened
    public Collection<Portlet> findAllPortlets() throws SystemException {
        return portletFac.getPortlets();
    }
    @Override
    public boolean canAddPortletToLayout(Portlet portlet) {
        String[] portlets = PropsUtil.getArray(PropsUtil.PORTLETS_EXCLUDED_FOR_LAYOUT);
        for (String portletId : portlets) {
            if (portletId.trim().equals(portlet.getPortletId()))
                return false;
        }
        return true;
    }
    @Override
    public boolean canAddPortletToLayout(String portletId) {
        String[] attachablePortlets = PropsUtil.getArray(PropsUtil.PORTLETS_EXCLUDED_FOR_LAYOUT);
        for (String attachablePortlet : attachablePortlets) {
            if (attachablePortlet.trim().equals(portletId))
                return false;
        }
        return true;
    }

    @Override
    public com.dotcms.repackage.javax.portlet.Portlet getImplementingInstance(final Portlet portlet) {

        if (portlet.getCachedInstance().isPresent()) {
            return portlet.getCachedInstance().get();
        }


        PortletConfig config = getPortletConfig(portlet);

        return portlet.getCachedInstance(config);
    }

    @Override
    public PortletConfig getPortletConfig(Portlet portlet) {
        return new PortletConfigImpl(portlet.getPortletId(), getPortletContext(), portlet.getInitParams(), portlet.getResourceBundle());
    }

    @Override
    public PortletContext getPortletContext() {

        return new PortletContextImpl(context);
    }

    /**
     * This method checks if every baseType in the String exists and adds it
     * to a list.
     * @param baseTypes
     * @return list of Base Types
     */
    private List<BaseContentType> checkBaseTypes(final String baseTypes) {
        if (!UtilMethods.isSet(baseTypes)) {
            return ImmutableList.of();
        }
        String[] baseTypesArray = baseTypes.trim().split(",");
        List<BaseContentType> baseTypeList = new ArrayList<>();
        for (String type : baseTypesArray) {
            if (UtilMethods.isSet(type) && UtilMethods.isNumeric(type.trim())) {
                baseTypeList.add(BaseContentType.getBaseContentType(Integer.parseInt(type.trim())));
            } else {
                baseTypeList.add(BaseContentType.getBaseContentType(type.trim()));
            }
        }
        return baseTypeList;
    }

    /**
     * This method checks if every content Type in the String exists and adds it
     * to a list.
     * @param contentTypes
     * @return list of Content Types
     */
    private List<ContentType> checkContentTypes(final String contentTypes) {
        if (!UtilMethods.isSet(contentTypes)) {
            return ImmutableList.of();
        }
        ContentTypeAPI contentTypeApi = APILocator.getContentTypeAPI(APILocator.systemUser());
        List<ContentType> contentTypeList = new ArrayList<>();
        String[] contentTypesArray = contentTypes.trim().split(",");

        for (String type : contentTypesArray) {
            ContentType contentType = Try.of(() -> contentTypeApi.find(type.trim())).getOrNull();
            if (contentType != null) {
                contentTypeList.add(contentType);
            }
        }

        if(contentTypeList.size() < contentTypesArray.length){
            throw new IllegalArgumentException("One or more of the Content Types defined does not exist");
        }

        return contentTypeList;
    }

    @Override
    public Portlet updatePortlet(final Portlet portlet) throws DotDataException{
        return portletFac.updatePortlet(portlet);
    }

}
