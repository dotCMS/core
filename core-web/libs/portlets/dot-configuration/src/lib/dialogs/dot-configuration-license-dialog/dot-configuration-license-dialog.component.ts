import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { SkeletonModule } from 'primeng/skeleton';

import {
    DotCompanyConfigurationService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { DotLicenseInfo } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

/** Where dotCMS describes licensing options other than the Business Source License. */
export const ALTERNATIVE_LICENSING_URL = 'https://www.dotcms.com/pricing';

/** The published license, linked when the running instance's license cannot be loaded. */
export const LICENSE_SOURCE_URL = 'https://github.com/dotCMS/core/blob/main/LICENSE';

type LicenseState =
    | { status: 'loading' }
    | { status: 'loaded'; license: DotLicenseInfo }
    | { status: 'error' };

/**
 * Shows the license shipped with the running build: its title as the dialog header, the
 * licensor, change date and change license under it, and the full text. If the license cannot
 * be loaded, it links to the published license instead.
 */
@Component({
    selector: 'dot-configuration-license-dialog',
    imports: [ButtonModule, SkeletonModule, DotMessagePipe],
    templateUrl: './dot-configuration-license-dialog.component.html',
    styleUrls: ['./dot-configuration-license-dialog.component.scss']
})
export class DotConfigurationLicenseDialogComponent implements OnInit {
    readonly #ref = inject(DynamicDialogRef);
    readonly #config = inject(DynamicDialogConfig);
    readonly #configurationService = inject(DotCompanyConfigurationService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #destroyRef = inject(DestroyRef);

    protected readonly alternativeLicensingUrl = ALTERNATIVE_LICENSING_URL;
    protected readonly licenseSourceUrl = LICENSE_SOURCE_URL;

    protected readonly $state = signal<LicenseState>({ status: 'loading' });

    protected readonly $license = computed(() => {
        const state = this.$state();

        return state.status === 'loaded' ? state.license : null;
    });

    /**
     * The header values the license states, on one line (`Licensor … · Change Date … · Change
     * License …`); the missing ones are left out. Empty when the license has none.
     */
    protected readonly $details = computed(() => {
        const license = this.$license();

        if (!license) {
            return '';
        }

        const details: [string, string | null][] = [
            ['configuration.license.licensor', license.licensor],
            ['configuration.license.change-date', license.changeDate],
            ['configuration.license.change-license', license.changeLicense]
        ];

        return details
            .filter(([, value]) => !!value)
            .map(([key, value]) => this.#dotMessageService.get(key, value as string))
            .join(' · ');
    });

    ngOnInit(): void {
        this.#configurationService
            .getLicense()
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe({
                next: (license) => {
                    // DynamicDialog reads its header from the config on every check, so the
                    // generic title given when opening becomes the license's own, with version.
                    this.#config.header = license.title;
                    this.$state.set({ status: 'loaded', license });
                },
                error: (error) => {
                    this.#httpErrorManager.handle(error);
                    this.$state.set({ status: 'error' });
                }
            });
    }

    close(): void {
        this.#ref.close();
    }
}
