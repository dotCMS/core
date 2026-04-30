import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { TooltipModule } from 'primeng/tooltip';

import { DotAuthHeadlessConfig } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotAuthTrustedIdpEditorComponent, TrustedIdpChange } from './dot-auth-trusted-idp-editor.component';

export interface HeadlessChange {
    path: string;
    value: unknown;
}

@Component({
    selector: 'dot-auth-headless-section',
    standalone: true,
    imports: [
        FormsModule,
        ButtonModule,
        InputNumberModule,
        InputTextModule,
        ToggleSwitchModule,
        TooltipModule,
        DotMessagePipe,
        DotAuthTrustedIdpEditorComponent
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './dot-auth-headless-section.component.html',
    styleUrl: '../_dot-auth-shared.scss'
})
export class DotAuthHeadlessSectionComponent {
    readonly headless = input.required<DotAuthHeadlessConfig>();

    readonly fieldChange = output<HeadlessChange>();
    readonly addIdp = output<void>();
    readonly removeIdp = output<number>();
    readonly addOrigin = output<void>();
    readonly removeOrigin = output<number>();
    readonly discover = output<number>();
    readonly revoke = output<void>();

    readonly expandedIdp = signal<number | null>(0);

    onChange(field: string, value: unknown): void {
        this.fieldChange.emit({ path: `headless.${field}`, value });
    }

    onIdpChange(event: TrustedIdpChange): void {
        this.fieldChange.emit(event);
    }

    onOriginChange(index: number, value: string): void {
        this.fieldChange.emit({ path: `headless.allowedOrigins.${index}`, value });
    }

    toggleIdp(index: number): void {
        this.expandedIdp.set(this.expandedIdp() === index ? null : index);
    }
}
