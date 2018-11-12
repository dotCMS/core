package com.dotcms.rendering.velocity.directive;

import com.dotcms.rendering.velocity.services.VelocityType;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import org.apache.velocity.context.Context;

import java.util.List;
import java.util.Optional;

import static com.dotmarketing.util.StringUtils.builder;
import static com.liferay.util.StringPool.FORWARD_SLASH;
import static com.liferay.util.StringPool.PERIOD;

/**
 * Subscribe Strategies and get the strategy for a set of arguments if applies
 * @author jsanca
 */
public class TemplatePathStrategyResolver {

    private static final String HOST_INDICATOR     = "//";
    public  static final String DEFAULT_UUID_VALUE = MultiTree.LEGACY_RELATION_TYPE;

    private volatile TemplatePathStrategy       defaultOne = null;
    private volatile List<TemplatePathStrategy> strategies = this.getDefaultStrategies();

    private List<TemplatePathStrategy> getDefaultStrategies() {

        final ImmutableList.Builder<TemplatePathStrategy> builder =
                new ImmutableList.Builder<>();

        final IdentifierTemplatePathStrategyImpl identifierTemplatePathStrategy = new IdentifierTemplatePathStrategyImpl();

        builder.add(identifierTemplatePathStrategy);
        builder.add(new PathTemplatePathStrategyImpl());

        this.defaultOne = identifierTemplatePathStrategy;

        return builder.build();
    }

    public TemplatePathStrategy getDefaultStrategy () {

        return defaultOne;
    }

    public synchronized void setDefaultStrategy (final TemplatePathStrategy strategy) {

        if (null != strategy) {

            this.defaultOne = strategy;
        }
    }


    private static class SingletonHolder {
        private static final TemplatePathStrategyResolver INSTANCE = new TemplatePathStrategyResolver();
    }
    /**
     * Get the instance.
     * @return TemplatePathStrategyResolver
     */
    public static TemplatePathStrategyResolver getInstance() {

        return TemplatePathStrategyResolver.SingletonHolder.INSTANCE;
    } // getInstance.

    /**
     * Adds a new strategy
     * @param strategy
     */
    public synchronized void subscribe (final TemplatePathStrategy strategy) {

        if (null != strategy) {

            final ImmutableList.Builder<TemplatePathStrategy> builder =
                    new ImmutableList.Builder<>();

            builder.addAll(this.strategies);
            builder.add(strategy);

            this.strategies = builder.build();
        }
    }

    /**
     * Get a strategy if applies
     * @param context
     * @param params
     * @param arguments
     * @return Optional TemplatePathStrategy
     */
    public Optional<TemplatePathStrategy> get(final Context context, final RenderParams params, final String[] arguments) {

        for (int i = 0; i < this.strategies.size(); ++i) {

            final TemplatePathStrategy strategy = this.strategies.get(i);
            if (strategy.test(context, params, arguments)) {

                return Optional.of(strategy);
            }
        }

        return Optional.empty();
    }

    /////////////
    private class PathTemplatePathStrategyImpl implements TemplatePathStrategy {

        @Override
        public boolean test(final Context context, final RenderParams params, final String[] arguments) {

            final  String templatePath = arguments[0];
            return this.isPath(templatePath);
        }

        @Override
        public String apply(final Context context, final RenderParams params, final String[] arguments) {

            final String templatePath = arguments[0];
            final String uid          = (arguments.length > 1 && UtilMethods.isSet(arguments[1])) ? arguments[1] :  DEFAULT_UUID_VALUE;
            return this.getPath(params, templatePath, uid);
        }

        private boolean isPath (final String templatePath) {

            return UtilMethods.isSet(templatePath) && templatePath.contains(FORWARD_SLASH);
        }

        private String getPath(final RenderParams params, final String path, final String uid) {


            final Host host     = params.currentHost;
            String templatePath = path;


            try {

                templatePath =(!templatePath.startsWith(HOST_INDICATOR))?
                        builder(FORWARD_SLASH, params.mode.name(), FORWARD_SLASH, HOST_INDICATOR,
                            this.getHost(host).getHostname(), path.startsWith(FORWARD_SLASH)? StringPool.BLANK:FORWARD_SLASH,
                            path, FORWARD_SLASH, uid, PERIOD, VelocityType.CONTAINER.fileExtension).toString():

                        builder(FORWARD_SLASH, params.mode.name(), FORWARD_SLASH, HOST_INDICATOR,
                            path.startsWith(FORWARD_SLASH)? StringPool.BLANK:FORWARD_SLASH,
                            path, FORWARD_SLASH, uid, PERIOD, VelocityType.CONTAINER.fileExtension).toString();
            } catch (Exception e) {

                Logger.warn(this.getClass(), " - unable to resolve " + templatePath + " getting this: "+ e.getMessage() );
                if(e.getStackTrace().length>0) {
                    Logger.warn(this.getClass(), " - at " + e.getStackTrace()[0]);
                }
                throw new DotStateException(e);
            }

            return templatePath;
        }

        private Host getHost(final Host host) {

            if (null == host) {

                try {
                    return APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
                } catch (DotDataException  | DotSecurityException e) {

                    return APILocator.systemHost();
                }
            }

            return host;
        }
    } // PathTemplatePathStrategyImpl.

    /////////////
    private class IdentifierTemplatePathStrategyImpl implements TemplatePathStrategy {

        @Override
        public boolean test(final Context context, final RenderParams params, final String[] arguments) {

            final String id = arguments[0];
            return this.isIdentifier(id);
        }

        @Override
        public String apply(final Context context, final RenderParams params, final String[] arguments) {

            final String id   = arguments[0];
            final String uid  = (arguments.length > 1 && UtilMethods.isSet(arguments[1])) ? arguments[1] :  DEFAULT_UUID_VALUE;

            return builder(FORWARD_SLASH, params.mode.name(), FORWARD_SLASH, this.getIdentifier(id),
                    FORWARD_SLASH, uid, PERIOD, VelocityType.CONTAINER.fileExtension).toString();
        }

        private String getIdentifier(final String identifier) {

            final Optional<ShortyId> shortyIdOptional =
                    APILocator.getShortyAPI().getShorty(identifier);

            return shortyIdOptional.isPresent()?
                    shortyIdOptional.get().longId:identifier;
        }

        private boolean isIdentifier (final String id) {

            boolean isIdentifier = false;
            isIdentifier |= UUIDUtil.isUUID(id);

            if (!isIdentifier) {
                try {
                    APILocator.getShortyAPI().validShorty(id);
                    isIdentifier = true;
                } catch (Exception e) {
                    isIdentifier = false;
                }
            }

            return isIdentifier;
        }
    } // IdentifierTemplatePathStrategyImpl.
} // E:O:F:TemplatePathStrategyResolver.
