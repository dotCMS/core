import { Component, computed, inject, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputTextModule } from 'primeng/inputtext';

import { DotMessagePipe } from '@dotcms/ui';

import { DotConfigurationCardComponent } from '../../components/dot-configuration-card/dot-configuration-card.component';
import { DotConfigurationStore } from '../../store/dot-configuration.store';

/**
 * Outbound Communication card: the portal URL used to build links in system email, and the
 * sender address those emails go out from, with a button to send a test message.
 */
@Component({
    selector: 'dot-configuration-outbound',
    imports: [
        FormsModule,
        ButtonModule,
        InputGroupModule,
        InputTextModule,
        DotMessagePipe,
        DotConfigurationCardComponent
    ],
    templateUrl: './dot-configuration-outbound.component.html',
    styleUrls: ['./dot-configuration-outbound.component.scss']
})
export class DotConfigurationOutboundComponent {
    protected readonly store = inject(DotConfigurationStore);

    /** Send Test Mail was clicked with a valid address; the page opens the confirmation. */
    readonly sendTestMail = output<string>();

    protected readonly $branding = computed(() => this.store.draft()?.branding);
    protected readonly $canSendTestMail = computed(
        () => !this.store.errors().emailAddress && !this.store.saving()
    );

    onPortalUrlChange(portalURL: string): void {
        this.store.patchBranding({ portalURL });
    }

    onEmailChange(emailAddress: string): void {
        this.store.patchBranding({ emailAddress });
    }

    onSendTestMail(): void {
        const emailAddress = this.$branding()?.emailAddress;

        if (emailAddress && this.$canSendTestMail()) {
            this.sendTestMail.emit(emailAddress.trim());
        }
    }
}
