import { Component, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';

import { DotMessagePipe } from '@dotcms/ui';

import { DotConfigurationCardComponent } from '../../components/dot-configuration-card/dot-configuration-card.component';
import { DotConfigurationColorFieldComponent } from '../../components/dot-configuration-color-field/dot-configuration-color-field.component';
import { assetFileName } from '../../store/dot-configuration.mappers';
import { DotConfigurationStore } from '../../store/dot-configuration.store';

/**
 * Branding card: primary and secondary colors, the login background, the login screen logo and
 * the Enterprise-only navbar logo override. Image changes are picked in dialogs the page opens
 * from this card's outputs.
 */
@Component({
    selector: 'dot-configuration-branding',
    imports: [
        FormsModule,
        ButtonModule,
        CheckboxModule,
        DotMessagePipe,
        DotConfigurationCardComponent,
        DotConfigurationColorFieldComponent
    ],
    templateUrl: './dot-configuration-branding.component.html',
    styleUrls: ['./dot-configuration-branding.component.scss']
})
export class DotConfigurationBrandingComponent {
    protected readonly store = inject(DotConfigurationStore);

    /** The navbar logo override is shown only on Enterprise licenses. */
    readonly isEnterprise = input(false);

    readonly changeBackground = output<void>();
    readonly changeLoginLogo = output<void>();
    readonly changeNavBarLogo = output<void>();

    /** Ticked by the user before a navbar logo has been chosen. */
    readonly #overrideRequested = signal(false);

    protected readonly $branding = computed(() => this.store.draft()?.branding);
    protected readonly $backgroundImage = computed(() => this.$branding()?.backgroundImage ?? '');
    protected readonly $backgroundFileName = computed(() => assetFileName(this.$backgroundImage()));
    protected readonly $loginLogo = computed(() => this.$branding()?.loginScreenLogo ?? '');
    protected readonly $loginLogoFileName = computed(() => assetFileName(this.$loginLogo()));
    protected readonly $navBarLogo = computed(() => this.$branding()?.navBarLogo ?? '');
    protected readonly $navBarLogoFileName = computed(() => assetFileName(this.$navBarLogo()));
    protected readonly $overrideNavBarLogo = computed(
        () => !!this.$navBarLogo() || this.#overrideRequested()
    );

    onPrimaryColorChange(primaryColor: string): void {
        this.store.patchBranding({ primaryColor });
    }

    onSecondaryColorChange(secondaryColor: string): void {
        this.store.patchBranding({ secondaryColor });
    }

    /**
     * Unticking the override clears the navbar logo, so the admin top bar goes back to the
     * dotCMS logo on save.
     */
    onOverrideChange(override: boolean): void {
        this.#overrideRequested.set(override);

        if (!override) {
            this.store.patchBranding({ navBarLogo: '' });
        }
    }
}
