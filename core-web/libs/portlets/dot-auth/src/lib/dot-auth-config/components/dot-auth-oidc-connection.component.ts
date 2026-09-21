import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';

import { DOT_AUTH_HIDDEN_SECRET_MASK, DotAuthOidcConfig } from '@dotcms/dotcms-models';
import { DotCopyButtonComponent, DotMessagePipe } from '@dotcms/ui';

const OAUTH_CALLBACK_PATH = '/api/v1/oauth/callback';

export interface OidcConnectionChange {
    path: string;
    value: unknown;
}

@Component({
    selector: 'dot-auth-oidc-connection',
    imports: [
        FormsModule,
        ButtonModule,
        InputTextModule,
        TooltipModule,
        DotMessagePipe,
        DotCopyButtonComponent
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './dot-auth-oidc-connection.component.html',
    styleUrl: '../_dot-auth-shared.scss',
    host: { class: 'section-group' }
})
export class DotAuthOidcConnectionComponent {
    readonly oidc = input.required<DotAuthOidcConfig>();
    readonly callbackUrl = input<string>('');
    /** Hostname of the site being configured; the server sends none for SYSTEM_HOST. */
    readonly siteHostname = input<string>('');
    /** SYSTEM_HOST page: the real redirect host varies per inheriting site, so say so. */
    readonly isSystem = computed(() => !this.siteHostname().trim());
    readonly errors = input<Record<string, string>>({});

    readonly fieldChange = output<OidcConnectionChange>();
    readonly discover = output<void>();

    readonly showAdvanced = signal(false);

    /**
     * Predicts what OAuthWebInterceptor#computeCallbackUrl will send: the override when set,
     * else the configured site over https, else the origin the admin is using right now
     * (SYSTEM_HOST, localhost, the starter's "default" site), with the callback path appended
     * unless present. On SYSTEM_HOST the interceptor uses each login request's own host, so
     * the template adds a note whenever no override pins it.
     */
    readonly redirectUri = computed(() => {
        // ponytail: "has a URL" == hostname contains a dot; good enough to skip "default"/"localhost".
        const site = this.siteHostname().trim();
        const siteOrigin = site.includes('.') ? `https://${site}` : window.location.origin;
        const base = (this.callbackUrl().trim() || siteOrigin).replace(/\/+$/, '');

        return base.endsWith(OAUTH_CALLBACK_PATH) ? base : `${base}${OAUTH_CALLBACK_PATH}`;
    });

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
