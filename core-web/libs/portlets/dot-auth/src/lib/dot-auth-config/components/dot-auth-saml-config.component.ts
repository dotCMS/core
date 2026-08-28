import {
    ChangeDetectionStrategy,
    Component,
    computed,
    ElementRef,
    input,
    output,
    viewChild
} from '@angular/core';
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
    styleUrl: '../_dot-auth-shared.scss',
    host: { class: 'section-group' }
})
export class DotAuthSamlConfigComponent {
    readonly saml = input.required<DotAuthSamlUiConfig>();
    readonly siteId = input<string>('');
    readonly errors = input<Record<string, string>>({});

    /**
     * The ACS URL the SAML runtime actually serves: https is forced, the host comes
     * from spEndpointHostname (falling back to the entityId's host — never its path),
     * and the site-id segment the interceptor routes on is always appended.
     */
    readonly acsUrl = computed(() => {
        const hostname = (this.saml().spEndpointHostname || this.saml().entityId || '')
            .replace(/^https?:\/\//i, '')
            .replace(/[/?#].*$/, '');

        return hostname ? `https://${hostname}/dotsaml/login/${this.siteId()}` : '';
    });

    readonly fieldChange = output<SamlConfigChange>();
    readonly fetchMetadata = output<string>();

    readonly metadataDetails = viewChild<ElementRef<HTMLDetailsElement>>('metadataDetails');

    metadataFetchUrl = '';

    openMetadataPanel(): void {
        const el = this.metadataDetails()?.nativeElement;
        if (el) el.open = true;
    }

    error(field: string): string | null {
        return this.errors()[`saml.${field}`] ?? null;
    }

    onChange(field: string, value: unknown): void {
        this.fieldChange.emit({ path: `saml.${field}`, value });
    }

    addExtraProperty(): void {
        const current = [...(this.saml().extraProperties ?? [])];
        current.push({ key: '', value: '' });
        this.fieldChange.emit({ path: 'saml.extraProperties', value: current });
    }

    removeExtraProperty(index: number): void {
        const current = [...(this.saml().extraProperties ?? [])];
        current.splice(index, 1);
        this.fieldChange.emit({ path: 'saml.extraProperties', value: current });
    }

    onExtraPropertyChange(index: number, field: 'key' | 'value', value: string): void {
        const current = (this.saml().extraProperties ?? []).map((p) => ({ ...p }));
        current[index][field] = value;
        this.fieldChange.emit({ path: 'saml.extraProperties', value: current });
    }
}
