package com.dotcms.rest.api.v1.experiments;

import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Form to exclude {@link com.dotcms.experiments.model.Experiment} from {@link ExperimentsResource#isUserIncluded(HttpServletRequest, HttpServletResponse, ExcludedExperimentListForm)}
 * resource.
 *
 * @see ExperimentsResource#isUserIncluded(HttpServletRequest, HttpServletResponse, ExcludedExperimentListForm)
 */
@JsonDeserialize(builder = ExcludedExperimentListForm.Builder.class)
public class ExcludedExperimentListForm extends Validated  {

    private List<String> exclude;

    private ExcludedExperimentListForm(final ExcludedExperimentListForm.Builder builder) {
        this.exclude = builder.exclude;
    }

    public List<String> getExclude() {
        return exclude;
    }

    public static final class Builder {
        private List<String> exclude = Collections.emptyList();

        private Builder() {
        }

        public ExcludedExperimentListForm.Builder withExclude(final List<String> exclude) {
            this.exclude = exclude;
            return this;
        }

        public ExcludedExperimentListForm build() {
            return new ExcludedExperimentListForm(this);
        }
    }
}
