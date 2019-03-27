package com.dotmarketing.portlets.rules.conditionlet;

import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.exception.ComparisonNotPresentException;
import com.dotmarketing.portlets.rules.exception.ComparisonNotSupportedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import com.dotmarketing.portlets.rules.parameter.display.DropdownInput;
import com.dotmarketing.portlets.rules.parameter.type.TextType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import static com.dotcms.repackage.com.google.common.base.Preconditions.checkState;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.*;

/**
 * This {@link Conditionlet} will allow CMS users to execute Actionlets based on the Browser language,
 * in order to check the browser language we use the <strong>Accept-Language</strong> request header,
 * this {@link Conditionlet} it is a non-technical alternative to the {@link RequestHeaderConditionlet},
 * and provides a drop-down menu with the available comparison mechanisms, and a drop-down menu
 * where users can select one language to compare.
 *
 * @author Jonathan Gamba
 *         Date: 1/8/16
 */
public class UsersBrowserLanguageConditionlet extends Conditionlet<UsersBrowserLanguageConditionlet.Instance> {

    private static final long serialVersionUID = 1L;

    public static final String BROWSER_LANGUAGE_KEY = "api.system.ruleengine.conditionlet.BrowserLanguage";
    public static final String LANGUAGE_INPUT_KEY = "language";
    private static final String SYSTEM_LOCATE_LANGUAGE_KEY = "system.locale.language";

    public static final String BROWSER_LANGUAGE_HEADER = "Accept-Language";

    //Dropdown with the list of the ISO languages
    public static final DropdownInput isoLanguagesDropdown = new DropdownInput()
            .minSelections(1)
            .option("aa")
            .option("ab")
            .option("ae")
            .option("af")
            .option("ak")
            .option("am")
            .option("an")
            .option("ar")
            .option("as")
            .option("av")
            .option("ay")
            .option("az")
            .option("ba")
            .option("be")
            .option("bg")
            .option("bh")
            .option("bi")
            .option("bm")
            .option("bn")
            .option("bo")
            .option("br")
            .option("bs")
            .option("ca")
            .option("ce")
            .option("ch")
            .option("co")
            .option("cr")
            .option("cs")
            .option("cu")
            .option("cv")
            .option("cy")
            .option("da")
            .option("de")
            .option("dv")
            .option("dz")
            .option("ee")
            .option("el")
            .option("en")
            .option("eo")
            .option("es")
            .option("et")
            .option("eu")
            .option("fa")
            .option("ff")
            .option("fi")
            .option("fj")
            .option("fo")
            .option("fr")
            .option("fy")
            .option("ga")
            .option("gd")
            .option("gl")
            .option("gn")
            .option("gu")
            .option("gv")
            .option("ha")
            .option("he")
            .option("hi")
            .option("ho")
            .option("hr")
            .option("ht")
            .option("hu")
            .option("hy")
            .option("hz")
            .option("ia")
            .option("id")
            .option("ie")
            .option("ig")
            .option("ii")
            .option("ik")
            .option("io")
            .option("is")
            .option("it")
            .option("iu")
            .option("ja")
            .option("jv")
            .option("ka")
            .option("kg")
            .option("ki")
            .option("kj")
            .option("kk")
            .option("kl")
            .option("km")
            .option("kn")
            .option("ko")
            .option("kr")
            .option("ks")
            .option("ku")
            .option("kv")
            .option("kw")
            .option("ky")
            .option("la")
            .option("lb")
            .option("lg")
            .option("li")
            .option("ln")
            .option("lo")
            .option("lt")
            .option("lu")
            .option("lv")
            .option("mg")
            .option("mh")
            .option("mi")
            .option("mk")
            .option("ml")
            .option("mn")
            .option("mr")
            .option("ms")
            .option("mt")
            .option("my")
            .option("na")
            .option("nb")
            .option("nd")
            .option("ne")
            .option("ng")
            .option("nl")
            .option("nn")
            .option("no")
            .option("nr")
            .option("nv")
            .option("ny")
            .option("oc")
            .option("oj")
            .option("om")
            .option("or")
            .option("os")
            .option("pa")
            .option("pi")
            .option("pl")
            .option("ps")
            .option("pt")
            .option("qu")
            .option("rm")
            .option("rn")
            .option("ro")
            .option("ru")
            .option("rw")
            .option("sa")
            .option("sc")
            .option("sd")
            .option("se")
            .option("sg")
            .option("si")
            .option("sk")
            .option("sl")
            .option("sm")
            .option("sn")
            .option("so")
            .option("sq")
            .option("sr")
            .option("ss")
            .option("st")
            .option("su")
            .option("sv")
            .option("sw")
            .option("ta")
            .option("te")
            .option("tg")
            .option("th")
            .option("ti")
            .option("tk")
            .option("tl")
            .option("tn")
            .option("to")
            .option("tr")
            .option("ts")
            .option("tt")
            .option("tw")
            .option("ty")
            .option("ug")
            .option("uk")
            .option("ur")
            .option("uz")
            .option("ve")
            .option("vi")
            .option("vo")
            .option("wa")
            .option("wo")
            .option("xh")
            .option("yi")
            .option("yo")
            .option("za")
            .option("zh")
            .option("zu");

    private static final ParameterDefinition<TextType> browserLanguageParameter = new ParameterDefinition<>(
            3, LANGUAGE_INPUT_KEY, SYSTEM_LOCATE_LANGUAGE_KEY,
            isoLanguagesDropdown,
            "en"
    );

    public UsersBrowserLanguageConditionlet () {
        super(BROWSER_LANGUAGE_KEY,
                new ComparisonParameterDefinition(2, IS, IS_NOT),
                browserLanguageParameter);
    }

    @Override
    public boolean evaluate ( HttpServletRequest request, HttpServletResponse response, Instance instance ) {

        /*
        Returns an Enumeration of Locale objects indicating, in decreasing order starting with the preferred locale,
        the locales that are acceptable to the client based on the Accept-Language header
         */
        Enumeration<Locale> locales = request.getLocales();

        boolean evalSuccess = false;

        //Check if the selected language exist in the list of languages provided by the Accept-Language header
        while ( locales.hasMoreElements() ) {

            Locale locale = locales.nextElement();
            String language = locale.getLanguage();

            //Evaluate
            evalSuccess = IS.perform(language.toLowerCase(), instance.isoCode.toLowerCase());
            if ( evalSuccess ) {
                break;
            }
        }

        if ( instance.comparison == IS ) {
            //Nothing to do we already handle the IS case
        } else if ( instance.comparison == IS_NOT ) {
            //Now, for the IS_NOT case we just need to negate what the IS found
            evalSuccess = !evalSuccess;
        } else {
            throw new ComparisonNotSupportedException("Not supported comparison [" + instance.comparison.toString() + "]");
        }

        return evalSuccess;
    }

    @Override
    public Instance instanceFrom ( Map<String, ParameterModel> parameters ) {
        return new Instance(this, parameters);
    }

    public static class Instance implements RuleComponentInstance {

        private final String isoCode;
        private final Comparison comparison;

        private Instance ( UsersBrowserLanguageConditionlet definition, Map<String, ParameterModel> parameters ) {

            //Validate the provided info, on error IllegalStateException
            checkState(parameters != null && parameters.size() == 2, "Browser Language Condition requires parameters %s, %s and %s.", LANGUAGE_INPUT_KEY, COMPARISON_KEY);

            //Read the provided value
            this.isoCode = parameters.get(LANGUAGE_INPUT_KEY).getValue().toLowerCase();
            String comparisonValue = parameters.get(COMPARISON_KEY).getValue();

            try {
                this.comparison = ((ComparisonParameterDefinition) definition.getParameterDefinitions()
                        .get(COMPARISON_KEY))
                        .comparisonFrom(comparisonValue);
            } catch ( ComparisonNotPresentException e ) {
                throw new ComparisonNotSupportedException("The comparison '%s' is not supported on Condition type '%s'",
                        comparisonValue,
                        definition.getId());
            }
        }
    }

}
