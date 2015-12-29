package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
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
import com.dotmarketing.portlets.rules.parameter.type.TextType;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS_NOT;


///**
// * This conditionlet will allow CMS users to check the language a user has set
// * in their request. The language selected by the user is in the
// * {@link HttpServletRequest} object, which is used to perform the validation
// * and is retrieved using our own API. This {@link Conditionlet} provides a
// * drop-down menu with the available comparison mechanisms, and a drop-down menu
// * where users can select one or more languages to compare.
// *
// * @author Jose Castro
// * @version 1.0
// * @since 04-17-2015
// *
// */
public class VisitorsLanguageConditionlet extends Conditionlet<VisitorsLanguageConditionlet.Instance> {

    private static final long serialVersionUID = 1L;

    public static final String LANGUAGE_KEY = "language";

    private static final ParameterDefinition<TextType> LANGUAGE_PARAMETER = new ParameterDefinition<>(
        3, LANGUAGE_KEY,
        new DropdownInput()
            .allowAdditions()
            .minSelections(1)
            .option("af")
            .option("sq")
            .option("ar")
            .option("be")
            .option("bn")
            .option("bs")
            .option("bg")
            .option("ca")
            .option("zh")
            .option("hr")
            .option("cs")
            .option("da")
            .option("nl")
            .option("en")
            .option("fi")
            .option("fr")
            .option("de")
            .option("el")
            .option("ht")
            .option("he")
            .option("hi")
            .option("hu")
            .option("id")
            .option("is")
            .option("it")
            .option("ja")
            .option("ko")
            .option("ku")
            .option("lt")
            .option("no")
            .option("fa")
            .option("pl")
            .option("pt")
            .option("ro")
            .option("ru")
            .option("sd")
            .option("sm")
            .option("sr")
            .option("sk")
            .option("es")
            .option("sv")
            .option("th")
            .option("tr")
            .option("uk")
            .option("vi")
            .option("yi")
            .option("za"),
        "en"
    );
    private final LanguageWebAPI langApi;

    public VisitorsLanguageConditionlet() {
        this(WebAPILocator.getLanguageWebAPI());
    }

    @VisibleForTesting
    VisitorsLanguageConditionlet(LanguageWebAPI langApi) {
        super("api.ruleengine.system.conditionlet.VisitorsLanguage",
              new ComparisonParameterDefinition(2, IS, IS_NOT),
              LANGUAGE_PARAMETER);
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

        private Instance(VisitorsLanguageConditionlet definition, Map<String, ParameterModel> parameters) {
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
