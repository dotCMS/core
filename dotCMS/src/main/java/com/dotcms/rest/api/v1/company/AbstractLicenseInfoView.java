package com.dotcms.rest.api.v1.company;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * The dotCMS license shipped with the running build, as shown in the Configuration screen's
 * License dialog.
 *
 * @author hassandotcms
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = LicenseInfoView.class)
@JsonDeserialize(as = LicenseInfoView.class)
@Schema(description = "The dotCMS license text and its header values")
public interface AbstractLicenseInfoView {

    @Schema(
            description = "License name and version",
            example = "dotCMS Business Source License 1.1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String title();

    @Schema(description = "Licensor named in the license", example = "dotCMS LLC")
    @Nullable
    String licensor();

    @Schema(description = "When the license changes to the Change License",
            example = "Four years from August 01, 2025")
    @Nullable
    String changeDate();

    @Schema(description = "License that applies after the Change Date",
            example = "GNU General Public License (GPL) v3")
    @Nullable
    String changeLicense();

    @Schema(description = "Full license text", requiredMode = Schema.RequiredMode.REQUIRED)
    String text();
}
