package com.dotmarketing.portlets.rules.actionlet;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.exception.InvalidRuleParameterException;
import com.dotcms.visitor.business.VisitorAPI;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.exception.RuleEvaluationFailedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.display.RestDropdownInput;
import com.dotmarketing.util.Config;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Actionlet to add tags to the visitor object.
 * Tags are stored on the DB as a single text comma separated value.
 * Adding tag values is allowed but those values won't be saved to
 * the system tag list, these are only part of the visitor object and
 * there are not persisted anywhere else.
 * Maximum number of tags allowed is defined on MAX_TAGS;
 */
public class VisitorTagsActionlet extends RuleActionlet<VisitorTagsActionlet.Instance> {

    private static final String I18N_BASE = "api.system.ruleengine.actionlet.VisitorTagsActionlet";

    private final VisitorAPI visitorAPI;

    public static final String TAGS_KEY = "tags";

    public static final int MAX_TAGS = Config.getIntProperty("api.system.ruleengine.actionlet.VisitorTagsActionlet.MAX_TAGS", 10);

    @SuppressWarnings("unused")
    public VisitorTagsActionlet() {
        this(APILocator.getVisitorAPI());
    }

    @VisibleForTesting
    VisitorTagsActionlet(VisitorAPI visitorAPI) {
    	super(I18N_BASE,
    			new ParameterDefinition<>(1,
    					TAGS_KEY,
                        new RestDropdownInput("/api/v1/tags", "key", "label").minSelections(1).maxSelections(MAX_TAGS).allowAdditions()));
        this.visitorAPI = visitorAPI;
    }

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(parameters);
    }

    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
        boolean result;
        try {
            Optional<Visitor> opt = visitorAPI.getVisitor(request);
            if(opt.isPresent()) {
                Visitor visitor = opt.get();
                for (String tag : instance.options.split(",")) {
                    visitor.addTag(tag);
                }
                result = true;
            } else {
                throw new RuleEvaluationFailedException("No visitor was available. Could not execute action.");
            }
        } catch (RuleEvaluationFailedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuleEvaluationFailedException(e, "Could not evaluate action %s", this.getClass().getName());
        }
        return result;
    }

    static class Instance implements RuleComponentInstance {

    	public final String options;

        public Instance(Map<String, ParameterModel> parameters) throws InvalidRuleParameterException{
            this.options = checkValid(parameters);
        }

        private String checkValid(final Map<String, ParameterModel> parameters) throws InvalidRuleParameterException {
            if(parameters == null || parameters.size() != 1) {
                throw new InvalidRuleParameterException("This actionlet only allows '%s' as parameter", TAGS_KEY);
            }
            String value = parameters.get(TAGS_KEY).getValue();
            if(value == null) {
                throw new InvalidRuleParameterException("Null is not a valid parameter value");
            }
            String trimmedValue = value.trim();
            if(trimmedValue.indexOf('\"') > -1 || trimmedValue.indexOf('\'') > -1) {
                throw new InvalidRuleParameterException("Single or double quotes are not allowed");
            }
            if(trimmedValue.startsWith(",") || trimmedValue.endsWith(",")) {
                throw new InvalidRuleParameterException("Empty tag values are not allowed");
            }
            if(trimmedValue.isEmpty()) { throw new InvalidRuleParameterException("The tags parameter requieres values. Empty values are not allowed."); }
            String[] values = trimmedValue.trim().split(",");
            Set<String> uniqueValues = new HashSet<String>();
            for (String currentValue : values) {
                if(!uniqueValues.add(currentValue)) {
                    throw new InvalidRuleParameterException("Tag '%s' is duplicated.  Duplicated values are not allowed", currentValue);
                }
            }
            return trimmedValue;
        }
    }
}
