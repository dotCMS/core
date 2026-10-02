import { Component, computed, inject, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotCompanyAuthType } from '@dotcms/dotcms-models';
import { DotCopyButtonComponent, DotMessagePipe } from '@dotcms/ui';

import { DotConfigurationCardComponent } from '../../components/dot-configuration-card/dot-configuration-card.component';
import { DotConfigurationStore } from '../../store/dot-configuration.store';

/**
 * Security card: how users identify themselves on the login form, and the digest of the company
 * key with buttons to copy it and to regenerate the key. The digest is only returned to CMS
 * Administrators, so the key row is hidden for everyone else.
 */
@Component({
    selector: 'dot-configuration-security',
    imports: [
        FormsModule,
        ButtonModule,
        InputTextModule,
        SelectModule,
        TooltipModule,
        DotCopyButtonComponent,
        DotMessagePipe,
        DotConfigurationCardComponent
    ],
    templateUrl: './dot-configuration-security.component.html',
    styleUrls: ['./dot-configuration-security.component.scss']
})
export class DotConfigurationSecurityComponent {
    readonly #dotMessageService = inject(DotMessageService);

    protected readonly store = inject(DotConfigurationStore);

    /** Regenerate was clicked; the page asks for confirmation before calling the store. */
    readonly regenerateKey = output<void>();

    protected readonly authTypeOptions = [
        {
            label: this.#dotMessageService.get('configuration.security.auth-type.email'),
            value: DotCompanyAuthType.EMAIL_ADDRESS
        },
        {
            label: this.#dotMessageService.get('configuration.security.auth-type.user-id'),
            value: DotCompanyAuthType.USER_ID
        }
    ];

    protected readonly $authType = computed(() => this.store.draft()?.authType);

    onAuthTypeChange(authType: DotCompanyAuthType): void {
        this.store.setAuthType(authType);
    }
}
