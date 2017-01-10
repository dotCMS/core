package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.LanguageWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.exception.ComparisonNotPresentException;
import com.dotmarketing.portlets.rules.exception.ComparisonNotSupportedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import com.dotmarketing.portlets.rules.parameter.display.DropdownInput;
import com.dotmarketing.portlets.rules.parameter.display.RestDropdownInput;
import com.dotmarketing.portlets.rules.parameter.type.TextType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS_NOT;


/**
 * This conditionlet will allow CMS users to check the language a user has set
 * in their request. The language selected by the user is in the
 * {@link HttpServletRequest} object, which is used to perform the validation
 * and is retrieved using our own API. This {@link Conditionlet} provides a
 * drop-down menu with the available comparison mechanisms, and a drop-down menu
 * where users can select one or more languages to compare.
 *
 * @author Jose Castro
 * @version 1.0
 * @since 04-17-2015
 *
 */
public class CurrentSessionLanguageConditionlet extends Conditionlet<CurrentSessionLanguageConditionlet.Instance> {

    private static final long serialVersionUID = 1L;

    public static final String LANGUAGE_KEY = "language";

    private static final ParameterDefinition<TextType> language = new ParameterDefinition<>(3,
            LANGUAGE_KEY,
            new RestDropdownInput("/api/v1/languages", "key", "name").minSelections(1));

    private final LanguageWebAPI langApi;

    public CurrentSessionLanguageConditionlet() {
        this(WebAPILocator.getLanguageWebAPI());
    }

    @VisibleForTesting
    CurrentSessionLanguageConditionlet(LanguageWebAPI langApi) {
        super("api.ruleengine.system.conditionlet.CurrentSessionLanguage",
              new ComparisonParameterDefinition(2, IS, IS_NOT),
                language);
        this.langApi = langApi;
    }

    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
        Language lang = langApi.getLanguage(request);
        String language = lang.getLanguageCode().toLowerCase();
        //noinspection unchecked
        return instance.comparison.perform(instance.isoCode, language);
    }

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(this, parameters);
    }

    public static class Instance implements RuleComponentInstance {

        private final String isoCode;
        private final Comparison comparison;

        private Instance(CurrentSessionLanguageConditionlet definition, Map<String, ParameterModel> parameters) {
            this.isoCode = parameters.get(LANGUAGE_KEY).getValue().toLowerCase();
            String comparisonValue = parameters.get(COMPARISON_KEY).getValue();
            try {
                //noinspection unchecked
                this.comparison = ((ComparisonParameterDefinition)definition.getParameterDefinitions()
                    .get(COMPARISON_KEY))
                    .comparisonFrom(comparisonValue);
            } catch (ComparisonNotPresentException e) {
                throw new ComparisonNotSupportedException("The comparison '%s' is not supported on Condition type '%s'",
                                                          comparisonValue,
                                                          definition.getId());
            }
        }
    }
}
