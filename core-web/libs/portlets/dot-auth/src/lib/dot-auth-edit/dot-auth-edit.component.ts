import { EMPTY } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    computed,
    inject,
    OnInit,
    signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
    AbstractControl,
    FormArray,
    FormBuilder,
    FormGroup,
    FormsModule,
    ReactiveFormsModule,
    ValidationErrors,
    ValidatorFn,
    Validators
} from '@angular/forms';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { FieldsetModule } from 'primeng/fieldset';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { PasswordModule } from 'primeng/password';
import { SelectModule } from 'primeng/select';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TextareaModule } from 'primeng/textarea';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { TooltipModule } from 'primeng/tooltip';

import { catchError, take } from 'rxjs/operators';

import { DotAuthService, DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import {
    DOT_AUTH_HIDDEN_SECRET_MASK,
    DOT_AUTH_SAML_DECLARED_KEYS,
    DOT_AUTH_SYSTEM_HOST,
    DotAuthBuildRolesStrategy,
    DotAuthConfigPayload,
    DotAuthConfigValues,
    DotAuthProtocol,
    DotAuthSamlConfigValues,
    DotAuthSignatureValidation
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

interface ProviderTypeOption {
    labelKey: string;
    value: 'OIDC' | 'OAuth2';
}

interface ProtocolOption {
    label: string;
    value: DotAuthProtocol;
}

interface SignatureValidationOption {
    labelKey: string;
    value: DotAuthSignatureValidation;
}

interface BuildRolesStrategyOption {
    labelKey: string;
    value: DotAuthBuildRolesStrategy;
}

const SAML_RESERVED_KEYS: ReadonlySet<string> = new Set(DOT_AUTH_SAML_DECLARED_KEYS);

/** Rejects an extra-attribute key that collides with a built-in SAML field. */
function reservedKeyValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
        const value = String(control.value ?? '').trim();
        return value && SAML_RESERVED_KEYS.has(value) ? { reservedKey: true } : null;
    };
}

/**
 * Rejects an extra-attribute key that duplicates another row's key in the same
 * FormArray. Attached to each row's `key` control; walks up to the parent
 * FormArray to compare siblings.
 */
function uniqueKeyWithinArrayValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
        const value = String(control.value ?? '').trim();
        if (!value) return null;
        const row = control.parent;
        const array = row?.parent as FormArray | null;
        if (!array) return null;
        const duplicate = array.controls.some(
            (sibling) =>
                sibling !== row &&
                String((sibling as FormGroup).get('key')?.value ?? '').trim() === value
        );
        return duplicate ? { duplicateKey: true } : null;
    };
}

@Component({
    selector: 'dot-auth-edit',
    imports: [
        FormsModule,
        ReactiveFormsModule,
        ButtonModule,
        ConfirmDialogModule,
        FieldsetModule,
        ToggleSwitchModule,
        InputTextModule,
        MessageModule,
        PasswordModule,
        SelectModule,
        SelectButtonModule,
        TextareaModule,
        TooltipModule,
        DotMessagePipe
    ],
    templateUrl: './dot-auth-edit.component.html',
    providers: [ConfirmationService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAuthEditComponent implements OnInit {
    readonly MASK = DOT_AUTH_HIDDEN_SECRET_MASK;
    readonly SYSTEM_HOST = DOT_AUTH_SYSTEM_HOST;

    readonly #dialogConfig = inject(DynamicDialogConfig<{ hostId: string }>);
    readonly #dialogRef = inject(DynamicDialogRef);
    readonly #service = inject(DotAuthService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #fb = inject(FormBuilder);
    readonly #destroyRef = inject(DestroyRef);

    readonly hostId = this.#dialogConfig.data?.hostId ?? this.SYSTEM_HOST;
    readonly isSystem = this.hostId === this.SYSTEM_HOST;

    readonly $inherited = signal(false);
    readonly $loading = signal(true);

    /** Drives which form (OAuth or SAML) is rendered and submitted. */
    readonly $selectedProtocol = signal<DotAuthProtocol>('OAUTH');

    /** Set after a failed save attempt so the template can show a form-level cue. */
    readonly $validationAttempted = signal(false);

    /** Protocol returned by the backend for this host; null when not configured. */
    readonly #initialProtocol = signal<DotAuthProtocol | null>(null);

    readonly protocolOptions: ProtocolOption[] = [
        { label: 'OAuth 2.0 / OIDC', value: 'OAUTH' },
        { label: 'SAML 2.0', value: 'SAML' }
    ];

    readonly providerTypeOptions: ProviderTypeOption[] = [
        { labelKey: 'dotauth.field.providerType.oidc', value: 'OIDC' },
        { labelKey: 'dotauth.field.providerType.oauth2', value: 'OAuth2' }
    ];

    readonly signatureValidationOptions: SignatureValidationOption[] = [
        { labelKey: 'dotauth.field.sigvalidation.none', value: 'none' },
        { labelKey: 'dotauth.field.sigvalidation.response', value: 'response' },
        { labelKey: 'dotauth.field.sigvalidation.assertion', value: 'assertion' },
        {
            labelKey: 'dotauth.field.sigvalidation.responseandassertion',
            value: 'responseandassertion'
        }
    ];

    readonly buildRolesStrategyOptions: BuildRolesStrategyOption[] = [
        { labelKey: 'dotauth.field.buildRolesStrategy.all', value: 'ALL' },
        { labelKey: 'dotauth.field.buildRolesStrategy.idp', value: 'IDP' },
        { labelKey: 'dotauth.field.buildRolesStrategy.staticonly', value: 'STATICONLY' },
        { labelKey: 'dotauth.field.buildRolesStrategy.staticadd', value: 'STATICADD' },
        { labelKey: 'dotauth.field.buildRolesStrategy.none', value: 'NONE' }
    ];

    readonly oauthForm: FormGroup = this.#fb.group({
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
        buildRolesStrategy: ['ALL' as DotAuthBuildRolesStrategy],
        callbackUrl: [''],
        hashUserId: [true],
        exchangeEnabled: [false],
        exchangeProviderType: ['OIDC' as 'OIDC' | 'OAuth2'],
        exchangeIssuerUrl: [''],
        exchangeClientId: [''],
        exchangeClientSecret: [''],
        exchangeScopes: ['openid email profile'],
        exchangeAuthorizationUrl: [''],
        exchangeTokenUrl: [''],
        exchangeUserinfoUrl: [''],
        exchangeRevocationUrl: [''],
        exchangeLogoutUrl: [''],
        exchangeGroupsClaim: [''],
        exchangeGroupsUrl: [''],
        exchangeExtraRoles: [''],
        exchangeBuildRolesStrategy: ['ALL' as DotAuthBuildRolesStrategy],
        exchangeCallbackUrl: [''],
        exchangeHashUserId: [true]
    });

    readonly samlForm: FormGroup = this.#fb.group({
        enable: [true],
        idpName: ['', Validators.required],
        sPIssuerURL: ['', Validators.required],
        sPEndpointHostname: ['', Validators.required],
        signatureValidationType: ['none' as DotAuthSignatureValidation],
        idPMetadataFile: ['', Validators.required],
        publicCert: ['', Validators.required],
        privateKey: ['', Validators.required],
        buttonParam: ['/api/v1/dotsaml/metadata/$siteId'],
        customAttributes: this.#fb.array<FormGroup>([])
    });

    readonly $activeForm = computed<FormGroup>(() =>
        this.$selectedProtocol() === 'OAUTH' ? this.oauthForm : this.samlForm
    );

    get customAttributes(): FormArray<FormGroup> {
        return this.samlForm.get('customAttributes') as FormArray<FormGroup>;
    }

    /** Resolved URL for the SAML metadata download, with `$siteId` substituted. */
    readonly $metadataUrl = computed(() => {
        const raw =
            (this.samlForm.get('buttonParam')?.value as string | null) ??
            '/api/v1/dotsaml/metadata/$siteId';
        return raw.replace('$siteId', this.hostId);
    });

    ngOnInit(): void {
        this.#service
            .getConfig(this.hostId)
            .pipe(
                take(1),
                catchError((error) => {
                    this.#httpErrorManager.handle(error);
                    // Stay in the loading state so the save button doesn't blindly
                    // submit against empty form state.
                    return EMPTY;
                }),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((view) => {
                this.$inherited.set(view.inherited);
                this.$selectedProtocol.set(view.protocol);
                this.#initialProtocol.set(view.configured ? view.protocol : null);
                if (view.protocol === 'OAUTH') {
                    const values = view.values ?? {};
                    this.oauthForm.patchValue({
                        ...values,
                        exchangeEnabled: values.exchangeEnabled ?? values.enabled ?? false,
                        exchangeHashUserId: values.exchangeHashUserId ?? values.hashUserId ?? true
                    });
                    this.applyProviderValidators(
                        (values.providerType as 'OIDC' | 'OAuth2') ?? 'OIDC'
                    );
                    this.applyExchangeValidators(
                        Boolean(values.exchangeEnabled ?? values.enabled ?? false)
                    );
                } else {
                    this.loadSamlValues(view.values ?? {});
                }
                this.$loading.set(false);
            });

        this.oauthForm
            .get('providerType')!
            .valueChanges.pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe((type: 'OIDC' | 'OAuth2') => this.applyProviderValidators(type));

        this.oauthForm
            .get('exchangeEnabled')!
            .valueChanges.pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe((enabled: boolean) => this.applyExchangeValidators(enabled));
    }

    /**
     * Handle a click on the protocol selector. If the currently-loaded form is
     * dirty and the user initially had the opposite protocol configured, show
     * a confirmation: saving as the new protocol will delete the stored row of
     * the other protocol.
     */
    onProtocolChange(target: DotAuthProtocol): void {
        const current = this.$selectedProtocol();
        if (target === current) {
            return;
        }

        const stored = this.#initialProtocol();
        const switchingAwayFromStored = stored !== null && stored !== target;
        const formIsDirty = this.$activeForm().dirty;

        if (!switchingAwayFromStored && !formIsDirty) {
            this.$selectedProtocol.set(target);
            return;
        }

        this.#confirmationService.confirm({
            header: this.#dotMessageService.get('dotauth.confirm.switch-protocol.header'),
            message: this.#dotMessageService.get(
                'dotauth.confirm.switch-protocol.message',
                this.labelFor(target),
                this.labelFor(current)
            ),
            acceptLabel: this.#dotMessageService.get('Continue'),
            rejectLabel: this.#dotMessageService.get('Cancel'),
            closable: true,
            closeOnEscape: true,
            accept: () => this.$selectedProtocol.set(target),
            reject: () => {
                // $selectedProtocol signal is the source of truth — ngModel
                // re-reads it, so no extra work needed here.
            }
        });
    }

    save(): void {
        const form = this.$activeForm();
        if (form.invalid) {
            this.$validationAttempted.set(true);
            form.markAllAsTouched();
            return;
        }
        this.$validationAttempted.set(false);
        const payload: DotAuthConfigPayload =
            this.$selectedProtocol() === 'OAUTH'
                ? {
                      protocol: 'OAUTH',
                      values: form.getRawValue() as DotAuthConfigValues
                  }
                : {
                      protocol: 'SAML',
                      values: this.buildSamlValues()
                  };
        this.#dialogRef.close(payload);
    }

    cancel(): void {
        this.#dialogRef.close();
    }

    /** True when a required field should show its inline validation message. */
    hasRequiredError(form: FormGroup, controlName: string): boolean {
        const control = form.get(controlName);
        return !!control && control.hasError('required') && (control.touched || control.dirty);
    }

    /** Add an empty custom-attribute row and mark the form dirty. */
    addCustomAttribute(key = '', value = ''): void {
        const row = this.#fb.group({
            key: [
                key,
                [Validators.required, reservedKeyValidator(), uniqueKeyWithinArrayValidator()]
            ],
            value: [value]
        });
        this.customAttributes.push(row);
        // Re-validate all siblings: a new row can create a duplicate, and the
        // cross-row check needs the parent chain fully wired — which only
        // holds after push completes.
        this.customAttributes.controls.forEach((sibling) =>
            sibling.get('key')?.updateValueAndValidity({ emitEvent: false })
        );
        if (key === '' && value === '') {
            this.customAttributes.markAsDirty();
        }
    }

    /** Remove a custom-attribute row. */
    removeCustomAttribute(index: number): void {
        this.customAttributes.removeAt(index);
        this.customAttributes.markAsDirty();
        // Re-validate siblings — removing a row can resolve a duplicate-key error.
        this.customAttributes.controls.forEach((row) =>
            row.get('key')?.updateValueAndValidity({ emitEvent: false })
        );
    }

    /** Trigger a browser download of the SAML metadata XML for this host. */
    downloadMetadata(): void {
        const url = this.$metadataUrl();
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = `dotsaml-metadata-${this.hostId}.xml`;
        anchor.target = '_blank';
        anchor.rel = 'noopener';
        document.body.appendChild(anchor);
        anchor.click();
        anchor.remove();
    }

    private labelFor(protocol: DotAuthProtocol): string {
        return this.#dotMessageService.get(
            protocol === 'OAUTH' ? 'dotauth.protocol.oauth' : 'dotauth.protocol.saml'
        );
    }

    /**
     * Populate the SAML form from an incoming values map: declared fields via
     * patchValue, everything else becomes a custom-attribute row.
     */
    private loadSamlValues(values: DotAuthSamlConfigValues): void {
        this.samlForm.patchValue(values);
        this.customAttributes.clear();
        for (const [key, rawValue] of Object.entries(values)) {
            if (SAML_RESERVED_KEYS.has(key)) continue;
            this.addCustomAttribute(key, rawValue == null ? '' : String(rawValue));
        }
    }

    /** Flatten the SAML form into the wire-level values map (declared + extras). */
    private buildSamlValues(): DotAuthSamlConfigValues {
        const raw = this.samlForm.getRawValue() as Record<string, unknown> & {
            customAttributes?: Array<{ key: string; value: string }>;
        };
        const { customAttributes, ...declared } = raw;
        const values: DotAuthSamlConfigValues = { ...declared };
        for (const attr of customAttributes ?? []) {
            const key = String(attr.key ?? '').trim();
            if (!key || SAML_RESERVED_KEYS.has(key)) continue;
            values[key] = attr.value ?? '';
        }
        return values;
    }

    /**
     * Tighten validators based on provider type: OIDC needs only issuer URL; OAuth2 needs
     * the three endpoint URLs. clientId / clientSecret are always required.
     */
    private applyProviderValidators(type: 'OIDC' | 'OAuth2'): void {
        const issuer = this.oauthForm.get('issuerUrl')!;
        const authz = this.oauthForm.get('authorizationUrl')!;
        const token = this.oauthForm.get('tokenUrl')!;
        const userinfo = this.oauthForm.get('userinfoUrl')!;

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

    /**
     * The exchange flow is intentionally independent from browser login. Exchange fields
     * may be omitted to inherit the browser-login value, matching the backend fallback.
     */
    private applyExchangeValidators(enabled: boolean): void {
        const issuer = this.oauthForm.get('exchangeIssuerUrl')!;
        const clientId = this.oauthForm.get('exchangeClientId')!;
        const clientSecret = this.oauthForm.get('exchangeClientSecret')!;

        issuer.setValidators(
            enabled && !this.oauthForm.get('issuerUrl')?.value ? [Validators.required] : []
        );
        clientId.setValidators(
            enabled && !this.oauthForm.get('clientId')?.value ? [Validators.required] : []
        );
        clientSecret.setValidators(
            enabled && !this.oauthForm.get('clientSecret')?.value ? [Validators.required] : []
        );

        issuer.updateValueAndValidity({ emitEvent: false });
        clientId.updateValueAndValidity({ emitEvent: false });
        clientSecret.updateValueAndValidity({ emitEvent: false });
    }
}
