import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    inject,
    OnInit,
    signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
    FormBuilder,
    FormGroup,
    ReactiveFormsModule,
    Validators
} from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { FieldsetModule } from 'primeng/fieldset';
import { InputSwitchModule } from 'primeng/inputswitch';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { PasswordModule } from 'primeng/password';
import { SelectModule } from 'primeng/select';
import { take } from 'rxjs/operators';

import { DotAuthService } from '@dotcms/data-access';
import {
    DOT_AUTH_HIDDEN_SECRET_MASK,
    DOT_AUTH_SYSTEM_HOST,
    DotAuthConfigPayload,
    DotAuthConfigValues
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

interface ProviderTypeOption {
    labelKey: string;
    value: 'OIDC' | 'OAuth2';
}

@Component({
    selector: 'dot-auth-edit',
    standalone: true,
    imports: [
        ReactiveFormsModule,
        ButtonModule,
        FieldsetModule,
        InputSwitchModule,
        InputTextModule,
        MessageModule,
        PasswordModule,
        SelectModule,
        DotMessagePipe
    ],
    templateUrl: './dot-auth-edit.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAuthEditComponent implements OnInit {
    readonly MASK = DOT_AUTH_HIDDEN_SECRET_MASK;
    readonly SYSTEM_HOST = DOT_AUTH_SYSTEM_HOST;

    private readonly dialogConfig = inject(DynamicDialogConfig<{ hostId: string }>);
    private readonly dialogRef = inject(DynamicDialogRef);
    private readonly service = inject(DotAuthService);
    private readonly fb = inject(FormBuilder);
    private readonly destroyRef = inject(DestroyRef);

    readonly hostId = this.dialogConfig.data?.hostId ?? this.SYSTEM_HOST;
    readonly isSystem = this.hostId === this.SYSTEM_HOST;

    readonly inherited = signal(false);
    readonly loading = signal(true);

    readonly providerTypeOptions: ProviderTypeOption[] = [
        { labelKey: 'dotauth.field.providerType.oidc', value: 'OIDC' },
        { labelKey: 'dotauth.field.providerType.oauth2', value: 'OAuth2' }
    ];

    readonly form: FormGroup = this.fb.group({
        enabled: [false],
        enableBackend: [true],
        enableFrontend: [false],
        providerType: ['OIDC' as 'OIDC' | 'OAuth2'],
        issuerUrl: [''],
        clientId: ['', Validators.required],
        clientSecret: ['', Validators.required],
        scopes: ['openid email profile'],
        authorizationUrl: [''],
        tokenUrl: [''],
        userinfoUrl: [''],
        revocationUrl: [''],
        logoutUrl: [''],
        groupsClaim: [''],
        groupsUrl: [''],
        extraRoles: [''],
        callbackUrl: ['']
    });

    ngOnInit(): void {
        this.service
            .getConfig(this.hostId)
            .pipe(take(1), takeUntilDestroyed(this.destroyRef))
            .subscribe((view) => {
                this.inherited.set(view.inherited);
                this.form.patchValue(view.values ?? {});
                this.applyProviderValidators(
                    (view.values?.providerType as 'OIDC' | 'OAuth2') ?? 'OIDC'
                );
                this.loading.set(false);
            });

        this.form
            .get('providerType')!
            .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((type: 'OIDC' | 'OAuth2') => this.applyProviderValidators(type));
    }

    save(): void {
        if (this.form.invalid) {
            this.form.markAllAsTouched();
            return;
        }
        const raw = this.form.getRawValue() as DotAuthConfigValues;
        const payload: DotAuthConfigPayload = { values: raw };
        this.dialogRef.close(payload);
    }

    cancel(): void {
        this.dialogRef.close();
    }

    /**
     * Tighten validators based on provider type: OIDC needs only issuer URL; OAuth2 needs
     * the three endpoint URLs. clientId / clientSecret are always required.
     */
    private applyProviderValidators(type: 'OIDC' | 'OAuth2'): void {
        const issuer = this.form.get('issuerUrl')!;
        const authz = this.form.get('authorizationUrl')!;
        const token = this.form.get('tokenUrl')!;
        const userinfo = this.form.get('userinfoUrl')!;

        if (type === 'OIDC') {
            issuer.setValidators([Validators.required]);
            authz.clearValidators();
            token.clearValidators();
            userinfo.clearValidators();
        } else {
            issuer.clearValidators();
            authz.setValidators([Validators.required]);
            token.setValidators([Validators.required]);
            userinfo.setValidators([Validators.required]);
        }

        issuer.updateValueAndValidity({ emitEvent: false });
        authz.updateValueAndValidity({ emitEvent: false });
        token.updateValueAndValidity({ emitEvent: false });
        userinfo.updateValueAndValidity({ emitEvent: false });
    }
}
