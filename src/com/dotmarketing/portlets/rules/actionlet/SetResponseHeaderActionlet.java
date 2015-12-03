package com.dotmarketing.portlets.rules.actionlet;

import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.portlets.rules.actionlet.ActionParameterDefinition.DataType;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.portlets.rules.model.RuleActionParameter;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Actionlet to add Key/Value to the Response header.
 * The exact names that had to be set in params are: headerKey and headerValue.
 *
 * @author Geoff M. Granum
 * @version 1.0
 * @since 09-22-2015
 *
 */
public class SetResponseHeaderActionlet extends RuleActionlet{

    private static final String I18N_BASE = "api.system.ruleengine.actionlet.SetResponseHeader";

    public static final String HEADER_KEY = "headerKey";
    public static final String HEADER_VALUE = "headerValue";
    private static final List<ActionParameterDefinition> PARAMS = ImmutableList.of(
			new ActionParameterDefinition(HEADER_KEY, DataType.TEXT),
			new ActionParameterDefinition(HEADER_VALUE)
    );

    public SetResponseHeaderActionlet(){
        super(I18N_BASE, PARAMS);
    }

    @Override
    public void validateActionInstance(RuleAction actionInstance) throws InvalidActionInstanceException {
        Map<String, RuleActionParameter> params = actionInstance.getParameterMap();
        RuleActionParameter keyParam = Preconditions.checkNotNull(params.get(HEADER_KEY), "SetResponseHeaderActionlet requires headerKey parameter.");
        RuleActionParameter valueParam = Preconditions.checkNotNull(params.get(HEADER_VALUE), "SetResponseHeaderActionlet requires headerValue parameter.");
        Preconditions.checkArgument(StringUtils.isNotBlank(keyParam.getValue()), "SetResponseHeaderActionlet requires valid headerKey.");
    }

    @Override
    public void executeAction(HttpServletRequest request, HttpServletResponse response, Map<String, RuleActionParameter> params) {
        String key = params.get(HEADER_KEY).getValue();
        String value = params.get(HEADER_VALUE).getValue();
        response.setHeader(key, value);
    }


}
