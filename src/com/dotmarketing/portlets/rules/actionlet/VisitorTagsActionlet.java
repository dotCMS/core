package com.dotmarketing.portlets.rules.actionlet;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.visitor.business.VisitorAPI;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.business.PersonaAPI;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.exception.RuleEvaluationFailedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.display.DropdownInput;
import com.dotmarketing.portlets.rules.parameter.display.RestDropdownInput;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Logger;
import com.dotmarketing.viewtools.TagsWebAPI;
import com.liferay.portal.model.User;

import static com.dotcms.repackage.com.google.common.base.Preconditions.checkState;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Actionlet to add tags to the visitor object.
 * Tags are stored on the DB as a single text comma separated value.
 * Adding tag values is allowed but those values won't be saved to
 * the system tag list, these are only part of the visitor object and
 * there are not persisted anywhere else.
 */
public class VisitorTagsActionlet extends RuleActionlet<VisitorTagsActionlet.Instance> {

    private static final String I18N_BASE = "api.system.ruleengine.actionlet.VisitorTagsActionlet";

    private final VisitorAPI visitorAPI;

    public static final String TAGS_KEY = "tags";

    @SuppressWarnings("unused")
    public VisitorTagsActionlet() {
        this(APILocator.getVisitorAPI());
    }

    @VisibleForTesting
    VisitorTagsActionlet(VisitorAPI visitorAPI) {
    	super(I18N_BASE,
    			new ParameterDefinition<>(1,
    					TAGS_KEY,
                        new RestDropdownInput("/api/v1/tags", "key", "label").minSelections(0).maxSelections(50).allowAdditions()));
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
            if(opt.isPresent()){
            	Visitor visitor = opt.get();
            	for(String tag: instance.options.split(","))
            		visitor.addTag(tag);
            	return true;
            } else{
                Logger.warn(VisitorTagsActionlet.class, "No visitor was available. Could not execute action.");
                result = false;
            }
        } catch (Exception e) {
            throw new RuleEvaluationFailedException(e, "Could not evaluate action %s", this.getClass().getName());
        }
        return result;
    }

    static class Instance implements RuleComponentInstance {

    	public final String options;

        public Instance(Map<String, ParameterModel> parameters) {
        	checkState(parameters != null && parameters.size() == 1, "Visitor tags actionlet needs %s.", TAGS_KEY);
            assert parameters != null;
            this.options = parameters.get(TAGS_KEY).getValue();
        }
    }
}
