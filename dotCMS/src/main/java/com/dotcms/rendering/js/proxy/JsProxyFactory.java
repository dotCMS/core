package com.dotcms.rendering.js.proxy;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rendering.velocity.viewtools.content.ContentMap;
import com.dotcms.rendering.velocity.viewtools.content.LazyLoaderContentMap;
import com.dotcms.rendering.velocity.viewtools.content.StoryBlockMap;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.design.bean.Body;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.tag.model.TagInode;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyDate;
import org.graalvm.polyglot.proxy.ProxyHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * This class has all the knowledge to create a proxy object or wrapper for the javascript graal.
 * @author jsanca
 */
public class JsProxyFactory {

    private JsProxyFactory () {}

    private static final Map<String, JsProxyMapperStrategy> proxyMapperMap = new ConcurrentHashMap<>();

    static {
        registerMapper(new JsUserProxyMapperStrategyImpl());
        registerMapper(new JsRoleProxyMapperStrategyImpl());
        registerMapper(new JsResponseProxyMapperStrategyImpl());
        registerMapper(new JsRequestProxyMapperStrategyImpl());
        registerMapper(new JsLazyLoaderContentMapProxyMapperStrategyImpl());
        registerMapper(new JsContentMapProxyMapperStrategyImpl());
        registerMapper(new JsLanguageProxyMapperStrategyImpl());
        registerMapper(new JsBlobProxyMapperStrategyImpl());
        registerMapper(new JsCategoryProxyMapperStrategyImpl());
        registerMapper(new JsStoryBlockMapProxyMapperStrategyImpl());
        registerMapper(new JsJSONObjectProxyMapperStrategyImpl());
        registerMapper(new JsMapProxyMapperStrategyImpl());
        registerMapper(new JsCollectionProxyMapperStrategyImpl());
        registerMapper(new JsTagInodeProxyMapperStrategyImpl());
        registerMapper(new JsTemplateLayoutProxyMapperStrategyImpl());
        registerMapper(new JsBodyLayoutProxyMapperStrategyImpl());
        registerMapper(new JsSiteProxyMapperStrategyImpl());
        registerMapper(new JsContainerUUIDProxyMapperStrategyImpl());
        registerMapper(new JsTreeableProxyMapperStrategyImpl());
        registerMapper(new JsDateProxyMapperStrategyImpl());
        registerMapper(new JsLocaleProxyMapperStrategyImpl());
        registerMapper(new JsFileProxyMapperStrategyImpl());
        registerMapper(new JsFileProxyMapperStrategyImpl());
        registerMapper(new JsContentTypeMapperStrategyImpl());
        registerMapper(new JsFieldMapperStrategyImpl());
        registerMapper(new JsFieldVariableMapperStrategyImpl());
    }
    /**
     * Register a custom mapper
     * @param jsProxyMapper
     */
    public static void registerMapper(final JsProxyMapperStrategy jsProxyMapper) {

        proxyMapperMap.put(jsProxyMapper.getClass().getName(), jsProxyMapper);
    }

    /**
     * Unregister an existing mapper
     * @param className
     */
    public static void unregister(final String className) {

        proxyMapperMap.remove(className);
    }

    /**
     * Tries to create a Proxy object for the object passed as parameter
     * @param obj Object proxy object for javascript graaljs context
     * @return Object
     */
    public static Object createProxy(final Object obj) {

        Object proxy = obj; // we have to decided if we return by configuration the object if has not mapper for the type
        // or we throw an exception or throw an undefined javascript value
        if (null != obj) {

            for (final JsProxyMapperStrategy jsProxyMapper : proxyMapperMap.values()
                    .stream().sorted(Comparator.comparingLong(JsProxyMapperStrategy::getPriority)).collect(Collectors.toList())) {

                if (jsProxyMapper.test(obj)) {

                    proxy = jsProxyMapper.apply(obj);
                    break;
                }
            }
        }
        return proxy;
    }

    /**
     * Tries to unwrap the object passed as parameter
     * Otherwise returns the object as it is.
     * @param value Object
     * @return Object
     */
    public static Object unwrap(final Object value) {

        if (value instanceof JsProxyObject) {

            return JsProxyObject.class.cast(value).getWrappedObject();
        }

        if (value instanceof Value) {

            final Value eval =  (Value)value;

            if (eval.isHostObject()) {
                return eval.asHostObject();
            }

            if (eval.isString()) {
                return eval.as(String.class);
            }

            return eval.as(Object.class);
        }

        return value;
    }

    // Mappers
    private static final class JsUserProxyMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return obj instanceof User;
        }

        @Override
        public Object apply(final Object obj) {
            return new JsUser((User)obj);
        }
    }

    private static final class JsRoleProxyMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return obj instanceof Role;
        }

        @Override
        public Object apply(final Object obj) {
            return new JsRole((Role)obj);
        }
    }

    private static final class JsResponseProxyMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return obj instanceof HttpServletResponse;
        }

        @Override
        public Object apply(final Object obj) {
            return new JsResponse((HttpServletResponse)obj);
        }
    }

    private static final class JsRequestProxyMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return obj instanceof HttpServletRequest;
        }

        @Override
        public Object apply(final Object obj) {
            return new JsRequest((HttpServletRequest)obj, null);
        }
    }

    private static final class JsLazyLoaderContentMapProxyMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return obj instanceof LazyLoaderContentMap;
        }

        @Override
        public Object apply(final Object obj) {
            return new JsLazyLoaderContentMap((LazyLoaderContentMap)obj);
        }
    }

    private static final class JsLanguageProxyMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return obj instanceof Language;
        }

        @Override
        public Object apply(final Object obj) {
            return new JsLanguage((Language)obj);
        }
    }

    private static final class JsContentMapProxyMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return obj instanceof ContentMap;
        }

        @Override
        public Object apply(final Object obj) {
            return new JsContentMap((ContentMap)obj);
        }
    }

    private static final class JsBlobProxyMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return obj instanceof Part;
        }

        @Override
        public Object apply(final Object obj) {
            return new JsBlob((Part)obj);
        }
    }

    private static final class JsCategoryProxyMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return obj instanceof Category;
        }

        @Override
        public Object apply(final Object obj) {
            return new JsCategory((Category)obj);
        }
    }

    private static final class JsStoryBlockMapProxyMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return obj instanceof StoryBlockMap;
        }

        @Override
        public Object apply(final Object obj) {
            return new JsStoryBlockMap((StoryBlockMap)obj);
        }
    }

    private static final class JsJSONObjectProxyMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return obj instanceof JSONObject;
        }

        @Override
        public Object apply(final Object obj) {
            return new JsJSONObject((JSONObject)obj);
        }

        @Override
        public int getPriority() {
            return 10;
        }
    }

    private static final class JsMapProxyMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return obj instanceof Map;
        }

        @Override
        public Object apply(final Object obj) {
            return ProxyHashMap.from((Map)obj);
        }
    }

    private static final class JsCollectionProxyMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return obj instanceof Collection;
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public Object apply(final Object obj) {

            final List proxyList = new ArrayList();
            final Collection collection = (Collection)obj;
            for (final Object o : collection) {
                proxyList.add(createProxy(o));
            }

            return ProxyArray.fromList(proxyList);
        }
    }

    private static final class JsTagInodeProxyMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return obj instanceof TagInode;
        }

        @Override
        public Object apply(final Object obj) {

            return new JsTagInode((TagInode)obj);
        }
    }

    private static final class JsTemplateLayoutProxyMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return obj instanceof TemplateLayout;
        }

        @Override
        public Object apply(final Object obj) {

            return new JsTemplateLayout((TemplateLayout)obj);
        }
    }

    private static final class JsBodyLayoutProxyMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return obj instanceof Body;
        }

        @Override
        public Object apply(final Object obj) {

            return new JsBody((Body)obj);
        }
    }

    private static final class JsSiteProxyMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return obj instanceof Host;
        }

        @Override
        public Object apply(final Object obj) {

            return new JsSite((Host)obj);
        }
    }

    private static final class JsContainerUUIDProxyMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return obj instanceof ContainerUUID;
        }

        @Override
        public Object apply(final Object obj) {

            return new JsContainerUUID((ContainerUUID)obj);
        }
    }

    private static final class JsTreeableProxyMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return obj instanceof Treeable;
        }

        @Override
        public Object apply(final Object obj) {

            return new JsTreeable((Treeable)obj);
        }
    }

    private static final class JsDateProxyMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return obj instanceof Date;
        }

        @Override
        public Object apply(final Object obj) {

            return ProxyDate.from(Instant.ofEpochMilli(Date.class.cast(obj).getTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate());
        }
    }

    private static final class JsLocaleProxyMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return obj instanceof Locale;
        }

        @Override
        public Object apply(final Object obj) {

            return new JsLocale((Locale)obj);
        }
    }

    private static final class JsFileProxyMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return obj instanceof File;
        }

        @Override
        public Object apply(final Object obj) {

            return new JsFile((File)obj);
        }
    }

    private static final class JsContentTypeMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return obj instanceof ContentType;
        }

        @Override
        public Object apply(final Object obj) {

            return new JsContentType((ContentType)obj);
        }
    }

    private static final class JsFieldMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return obj instanceof Field;
        }

        @Override
        public Object apply(final Object obj) {

            return new JsField((Field)obj);
        }
    }

    private static final class JsFieldVariableMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return obj instanceof FieldVariable;
        }

        @Override
        public Object apply(final Object obj) {

            return new JsFieldVariable((FieldVariable)obj);
        }
    }

}
