import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { TooltipModule } from 'primeng/tooltip';

import { DotAuthTrustedIdp } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import {
    DotAuthProvisioningComponent,
    ProvisioningChange
} from './dot-auth-provisioning.component';

export interface TrustedIdpChange {
    path: string;
    value: unknown;
}

@Component({
    selector: 'dot-auth-trusted-idp-editor',
    standalone: true,
    imports: [
        FormsModule,
        ButtonModule,
        InputTextModule,
        TagModule,
        ToggleSwitchModule,
        TooltipModule,
        DotMessagePipe,
        DotAuthProvisioningComponent
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './dot-auth-trusted-idp-editor.component.html',
    styleUrl: '../_dot-auth-shared.scss'
})
export class DotAuthTrustedIdpEditorComponent {
    readonly idp = input.required<DotAuthTrustedIdp>();
    readonly index = input.required<number>();
    readonly expanded = input(false);

    readonly fieldChange = output<TrustedIdpChange>();
    readonly toggle = output<void>();
    readonly remove = output<void>();
    readonly discover = output<void>();

    get basePath(): string {
        return `headless.trustedIdps.${this.index()}`;
    }

    onChange(field: string, value: unknown): void {
        this.fieldChange.emit({ path: `${this.basePath}.${field}`, value });
    }

    onProvisioningChange(event: ProvisioningChange): void {
        this.fieldChange.emit({ path: `${this.basePath}.${event.path}`, value: event.value });
    }

    onAlgsChange(value: string): void {
        this.fieldChange.emit({
            path: `${this.basePath}.algs`,
            value: value
                .split(',')
                .map((a) => a.trim())
                .filter(Boolean)
        });
    }
}
