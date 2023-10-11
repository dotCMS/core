package com.dotcms.rendering.js.proxy;

import com.dotcms.rendering.velocity.viewtools.content.ContentMap;
import com.dotcms.rendering.velocity.viewtools.content.LazyLoaderContentMap;
import com.dotcms.rendering.velocity.viewtools.content.StoryBlockMap;
import com.dotmarketing.business.Role;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// todo: register a mapper for JsonObject, Content Type, ProxyHashMap and others also for the JsProxyObject (the other direction)
/**
 * This class has all the knowledge to create a proxy object or wrapper for the javascript graal.
 * @author jsanca
 */
public class JsProxyFactory {

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

        Object proxy = obj; // todo: we have to decided if we return by configuration the object if has not mapper for the type
        // or we throw an exception or throw an undefined javascript value
        if (null != obj) {

            for (final JsProxyMapperStrategy jsProxyMapper : proxyMapperMap.values()) {

                if (jsProxyMapper.test(obj)) {

                    proxy = jsProxyMapper.apply(obj);
                    break;
                }
            }
        }
        return proxy;
    }

    // Mappers
    private static final class JsUserProxyMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return null != obj && obj instanceof User;
        }

        @Override
        public Object apply(final Object obj) {
            return new JsUser((User)obj);
        }
    }

    private static final class JsRoleProxyMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return null != obj && obj instanceof Role;
        }

        @Override
        public Object apply(final Object obj) {
            return new JsRole((Role)obj);
        }
    }

    private static final class JsResponseProxyMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return null != obj && obj instanceof HttpServletResponse;
        }

        @Override
        public Object apply(final Object obj) {
            return new JsResponse((HttpServletResponse)obj);
        }
    }

    private static final class JsRequestProxyMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return null != obj && obj instanceof HttpServletRequest;
        }

        @Override
        public Object apply(final Object obj) {
            return new JsRequest((HttpServletRequest)obj);
        }
    }

    private static final class JsLazyLoaderContentMapProxyMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return null != obj && obj instanceof LazyLoaderContentMap;
        }

        @Override
        public Object apply(final Object obj) {
            return new JsLazyLoaderContentMap((LazyLoaderContentMap)obj);
        }
    }

    private static final class JsLanguageProxyMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return null != obj && obj instanceof Language;
        }

        @Override
        public Object apply(final Object obj) {
            return new JsLanguage((Language)obj);
        }
    }

    private static final class JsContentMapProxyMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return null != obj && obj instanceof ContentMap;
        }

        @Override
        public Object apply(final Object obj) {
            return new JsContentMap((ContentMap)obj);
        }
    }

    private static final class JsBlobProxyMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return null != obj && obj instanceof Part;
        }

        @Override
        public Object apply(final Object obj) {
            return new JsBlob((Part)obj);
        }
    }

    private static final class JsCategoryProxyMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return null != obj && obj instanceof Category;
        }

        @Override
        public Object apply(final Object obj) {
            return new JsCategory((Category)obj);
        }
    }

    private static final class JsStoryBlockMapProxyMapperStrategyImpl implements JsProxyMapperStrategy {
        @Override
        public boolean test(final Object obj) {
            return null != obj && obj instanceof StoryBlockMap;
        }

        @Override
        public Object apply(final Object obj) {
            return new JsStoryBlockMap((StoryBlockMap)obj);
        }
    }

}
