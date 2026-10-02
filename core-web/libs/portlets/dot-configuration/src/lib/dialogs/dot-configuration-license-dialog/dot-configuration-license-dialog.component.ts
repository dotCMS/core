import { Component, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessagePipe } from '@dotcms/ui';

/** Where dotCMS describes licensing options other than the Business Source License. */
export const ALTERNATIVE_LICENSING_URL = 'https://www.dotcms.com/pricing';

/** The license that ships with dotCMS, readable while the license endpoint does not exist. */
export const LICENSE_SOURCE_URL = 'https://github.com/dotCMS/core/blob/main/LICENSE';

/**
 * Shows the dotCMS Business Source License.
 *
 * Placeholder until the backend exposes the license text: it links to the license instead of
 * showing it, which is what the server itself falls back to when it cannot read the file.
 */
@Component({
    selector: 'dot-configuration-license-dialog',
    imports: [ButtonModule, DotMessagePipe],
    templateUrl: './dot-configuration-license-dialog.component.html',
    styleUrls: ['./dot-configuration-license-dialog.component.scss']
})
export class DotConfigurationLicenseDialogComponent {
    readonly #ref = inject(DynamicDialogRef);

    protected readonly alternativeLicensingUrl = ALTERNATIVE_LICENSING_URL;
    protected readonly licenseSourceUrl = LICENSE_SOURCE_URL;

    close(): void {
        this.#ref.close();
    }
}
