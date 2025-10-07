package com.dotcms.rest.api.v1.system;

import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;
import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Form to run an upgrade task
 * @author jsanca
 */
@JsonDeserialize(builder = UpgradeTaskForm.Builder.class)
public class UpgradeTaskForm extends Validated {

    @NotNull
    @Length(min = 2, max = 255)
    private final String upgradeTaskClass;

    public String getUpgradeTaskClass() {
        return upgradeTaskClass;
    }

    private UpgradeTaskForm(final Builder builder) {
        super();
        this.upgradeTaskClass = builder.upgradeTaskClass;
        checkValid();
    }

    public static final class Builder {

        @JsonProperty(required = true) private String upgradeTaskClass;

        public Builder upgradeTaskClass(final String upgradeTaskClass) {
            this.upgradeTaskClass = upgradeTaskClass;
            return this;
        }

        public UpgradeTaskForm build() {
            return new UpgradeTaskForm(this);
        }
    }
}

