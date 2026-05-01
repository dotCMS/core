import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { TooltipModule } from 'primeng/tooltip';

import { DOT_AUTH_HIDDEN_SECRET_MASK, DotAuthOidcConfig } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

export interface OidcConnectionChange {
    path: string;
    value: unknown;
}

@Component({
    selector: 'dot-auth-oidc-connection',
    standalone: true,
    imports: [
        FormsModule,
        ButtonModule,
        InputTextModule,
        SelectModule,
        ToggleSwitchModule,
        TooltipModule,
        DotMessagePipe
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './dot-auth-oidc-connection.component.html',
    styleUrl: '../_dot-auth-shared.scss'
})
export class DotAuthOidcConnectionComponent {
    readonly oidc = input.required<DotAuthOidcConfig>();
    readonly callbackUrl = input<string>('');
    readonly errors = input<Record<string, string>>({});

    readonly fieldChange = output<OidcConnectionChange>();
    readonly discover = output<void>();

    readonly showAdvanced = signal(false);

    readonly responseTypeOptions = [
        { value: 'code', label: 'code (Authorization Code Flow)' },
        { value: 'id_token', label: 'id_token (Implicit)' },
        { value: 'code id_token', label: 'code id_token (Hybrid)' }
    ];

    isSecretStored(): boolean {
        const secret = this.oidc().clientSecret;
        return secret === '****' || secret === DOT_AUTH_HIDDEN_SECRET_MASK;
    }

    error(field: string): string | null {
        return this.errors()[`oidc.${field}`] ?? null;
    }

    onChange(field: string, value: unknown): void {
        this.fieldChange.emit({ path: `oidc.${field}`, value });
    }

    onCallbackUrlChange(value: string): void {
        this.fieldChange.emit({ path: 'callbackUrl', value });
    }
}
