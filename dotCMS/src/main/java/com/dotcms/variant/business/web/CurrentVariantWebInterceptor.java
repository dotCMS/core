package com.dotcms.variant.business.web;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.util.LoginMode;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import java.io.IOException;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Listen and Intercept any request to:
 *
 * - /da/*
 * - /contentAsset/*
 *
 * And check if the request has the parameter <code>variantName</code>, if the parameter not exists
 * then check if the request has the header <code>referer</code> and if the header has the parameter
 * <code>variantName</code> then set it as an attribute to the request.
 */
public class CurrentVariantWebInterceptor implements WebInterceptor {
    private static final long serialVersionUID = 1L;
    private static final String[] PATHS = new String[] {
            "/dA/*",
            "/contentAsset/*"
    };

    @Override
    public String[] getFilters() {
        return PATHS;
    }

    @Override
    public Result intercept(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        final String requestParameter = request.getParameter(VariantAPI.VARIANT_KEY);

        if (UtilMethods.isSet(requestParameter)) {
            return Result.NEXT;
        }

        String currentVariantName = null;
        final String refererValue = request.getHeader("referer");

        if (UtilMethods.isSet(refererValue)) {
            final Optional<String> variantValueOptional = getVariantValueFromReferer(refererValue);

            if (variantValueOptional.isPresent()) {
                currentVariantName = variantValueOptional.get();
            }
        }

        if (!UtilMethods.isSet(currentVariantName)) {
            final HttpSession session = request.getSession();

            if (session != null) {
                final Object attribute = session.getAttribute(VariantAPI.VARIANT_KEY);

                if (isValidSessionAtribute(attribute) && LoginMode.get() != LoginMode.FE){
                    final CurrentVariantSessionItem currentVariantSessionItem = (CurrentVariantSessionItem) attribute;

                    if (!currentVariantSessionItem.isExpired()) {
                        currentVariantName = currentVariantSessionItem.getVariantName();
                    }
                }
            }
        }

        if (UtilMethods.isSet(currentVariantName)) {
            request.setAttribute(VariantAPI.VARIANT_KEY, currentVariantName);
        }

        return Result.NEXT;
    }

    private boolean isValidSessionAtribute(Object attribute) {
        return attribute != null && attribute instanceof CurrentVariantSessionItem;
    }

    private static Optional<String> getVariantValueFromReferer(final String refererValue) {
        final int variantNameIndexOf = refererValue.indexOf(VariantAPI.VARIANT_KEY);

        if (variantNameIndexOf != -1) {

            final int indexEndParameter = refererValue.indexOf(StringPool.AMPERSAND,
                    variantNameIndexOf);
            final String variantNameParameter = (indexEndParameter != -1) ?
                    refererValue.substring(variantNameIndexOf, indexEndParameter) :
                    refererValue.substring(variantNameIndexOf);

            return Optional.of(variantNameParameter.split(StringPool.EQUAL)[1]);
        }

        return Optional.empty();
    }
}