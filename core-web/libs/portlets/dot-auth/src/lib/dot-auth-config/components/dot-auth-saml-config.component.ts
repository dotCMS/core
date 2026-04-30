import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { TooltipModule } from 'primeng/tooltip';

import { DotAuthSamlUiConfig } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

export interface SamlConfigChange {
    path: string;
    value: unknown;
}

@Component({
    selector: 'dot-auth-saml-config',
    standalone: true,
    imports: [
        FormsModule,
        ButtonModule,
        InputTextModule,
        TextareaModule,
        ToggleSwitchModule,
        TooltipModule,
        DotMessagePipe
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './dot-auth-saml-config.component.html',
    styleUrl: '../_dot-auth-shared.scss'
})
export class DotAuthSamlConfigComponent {
    readonly saml = input.required<DotAuthSamlUiConfig>();
    readonly errors = input<Record<string, string>>({});

    readonly fieldChange = output<SamlConfigChange>();
    readonly fetchMetadata = output<void>();

    error(field: string): string | null {
        return this.errors()[`saml.${field}`] ?? null;
    }

    onChange(field: string, value: unknown): void {
        this.fieldChange.emit({ path: `saml.${field}`, value });
    }
}
