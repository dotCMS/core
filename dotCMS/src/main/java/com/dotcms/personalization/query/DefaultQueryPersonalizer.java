package com.dotcms.personalization.query;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import com.dotcms.visitor.domain.Visitor;
import com.dotcms.visitor.domain.Visitor.AccruedTag;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.personas.model.IPersona;
import com.dotmarketing.util.Config;

public class DefaultQueryPersonalizer implements QueryPersonalizer {

    @Override
    public String addPersonalizationToQuery(String query, HttpServletRequest request) {
        Optional<Visitor> opt = APILocator.getVisitorAPI().getVisitor(request);
        if (!opt.isPresent() || query == null) {
            return query;
        }
        query = query.toLowerCase();

        // if we are already personalized
        if (query.indexOf(" tags:") > -1) {
            return query;
        }

        final StringWriter buff = new StringWriter().append(query);
        final Visitor visitor = opt.get();
        final IPersona p = visitor.getPersona();
        final String keyTag = (p == null) ? null : p.getKeyTag();
        final Map<String, Float> personas = visitor.getWeightedPersonas();


        final List<AccruedTag> tags = visitor.getAccruedTags();
        if (p == null && (tags == null || tags.isEmpty()) && personas.isEmpty()) {
            return query;
        }

        int maxBoost = Config.getIntProperty("PULLPERSONALIZED_PERSONA_WEIGHT", 100);

        // make personas more powerful than the most powerful tag
        if (!tags.isEmpty()) {
            maxBoost = tags.get(0).getCount() + maxBoost;
        }


        if (Config.getBooleanProperty("PULLPERSONALIZED_USE_MULTIPLE_PERSONAS", true)) {

            if (personas != null && !personas.isEmpty()) {
                for (Map.Entry<String, Float> map : personas.entrySet()) {
                    int boostMe = Math.round(maxBoost * map.getValue());
                    if (map.getKey().equals(keyTag)) {
                        final int boostForCurrentPersona=boostMe + Config.getIntProperty("PULLPERSONALIZED_LAST_PERSONA_WEIGHT", 1);
                        buff.append(" tags:\"" + map.getKey().toLowerCase() + "\"^" + boostForCurrentPersona);
                    }else {
                        buff.append(" tags:\"" + map.getKey().toLowerCase() + "\"^" + boostMe);
                    }

                    
                }
            }


        } else {
            if (p != null) {
                buff.append(" tags:\"" + keyTag + "\"^" + maxBoost);
            }
        }



        for (AccruedTag tag : tags) {
            buff.append(" tags:\"" + tag.getTag().toLowerCase() + "\"^" + (tag.getCount() + 1) + " ");
        }

        return buff.toString();
    }

}
