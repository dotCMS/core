import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    OnInit,
    computed,
    inject,
    input,
    output,
    signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
    FormArray,
    FormControl,
    FormGroup,
    FormsModule,
    ReactiveFormsModule,
    ValidatorFn,
    Validators
} from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PanelModule } from 'primeng/panel';
import { PasswordModule } from 'primeng/password';
import { TagModule } from 'primeng/tag';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { TooltipModule } from 'primeng/tooltip';

import { DotAiService, DotMessageService } from '@dotcms/data-access';
import {
    DotAiProviderField,
    DotAiProviderMetadata,
    DotAiTestConnectionResult
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import {
    CAPABILITY_LABELS,
    DotAiCapabilityMeta,
    PROVIDER_DISPLAY_NAMES,
    PROVIDER_ORDER,
    isFieldAlwaysVisible,
    requiredUnlessValidator
} from '../../dot-ai-config.constants';
import {
    DotAiAdditionalPropertiesComponent,
    DotAiAdditionalPropertyGroup
} from '../dot-ai-additional-properties/dot-ai-additional-properties.component';
import { DotAiDynamicFieldComponent } from '../dot-ai-dynamic-field/dot-ai-dynamic-field.component';

/** Raw shape of one `chat`/`embeddings`/`image` section inside the `providerConfig` JSON. */
export type DotAiCapabilitySectionValue = Record<string, unknown> & { provider?: string };

@Component({
    selector: 'dot-ai-capability-card',
    templateUrl: './dot-ai-capability-card.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        FormsModule,
        ReactiveFormsModule,
        ButtonModule,
        InputTextModule,
        PasswordModule,
        PanelModule,
        TagModule,
        ToggleSwitchModule,
        TooltipModule,
        DotMessagePipe,
        DotAiDynamicFieldComponent,
        DotAiAdditionalPropertiesComponent
    ]
})
export class DotAiCapabilityCardComponent implements OnInit {
    private readonly dotAiService = inject(DotAiService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly destroyRef = inject(DestroyRef);

    readonly meta = input.required<DotAiCapabilityMeta>();
    readonly providers = input.required<DotAiProviderMetadata[]>();
    readonly initialValue = input<DotAiCapabilitySectionValue | null>(null);
    readonly siteId = input<string | undefined>(undefined);

    readonly changed = output<void>();

    readonly enabled = signal(false);
    readonly providerId = signal<string | null>(null);
    readonly fieldsGroup = signal(new FormGroup({}));
    readonly additionalProperties = new FormArray<DotAiAdditionalPropertyGroup>([]);

    readonly capabilityLabel = computed(() =>
        this.dotMessageService.get(CAPABILITY_LABELS[this.meta().capability])
    );

    readonly orderedProviders = computed(() => {
        const list = [...this.providers()];
        list.sort((a, b) => providerSortIndex(a.provider) - providerSortIndex(b.provider));

        return list;
    });

    readonly currentProviderMeta = computed(
        () => this.providers().find((p) => p.provider === this.providerId()) ?? null
    );

    /** Fields shown above the "Advanced" panel — required, plus optional fields worth surfacing
     *  by default (credentials, identity fields). See {@link isFieldAlwaysVisible}. */
    readonly visibleFields = computed(() =>
        this.fieldsForCurrentProvider().filter((f) => isFieldAlwaysVisible(f))
    );

    /** Truly-optional tuning fields (e.g. temperature, timeout) tucked under "Advanced". */
    readonly advancedFields = computed(() =>
        this.fieldsForCurrentProvider().filter((f) => !isFieldAlwaysVisible(f))
    );

    readonly badgeLabel = computed(() => {
        if (!this.enabled() || !this.providerId()) {
            return 'apps.ai.badge.not-configured';
        }

        return displayName(this.providerId() as string);
    });

    readonly badgeSeverity = computed(() =>
        this.enabled() && this.providerId() ? 'success' : 'secondary'
    );

    readonly testing = signal(false);
    readonly testResult = signal<DotAiTestConnectionResult | null>(null);

    ngOnInit(): void {
        this.additionalProperties.valueChanges
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(() => this.changed.emit());

        const initial = this.initialValue();
        const fallbackProvider = this.defaultProviderId();

        if (initial?.provider) {
            this.enabled.set(true);
            this.providerId.set(String(initial.provider));
            this.hydrateFields(initial);
        } else {
            this.enabled.set(false);
            this.providerId.set(fallbackProvider);
            this.rebuildFieldsGroup(fallbackProvider, {});
        }
    }

    onToggleEnabled(value: boolean): void {
        this.enabled.set(value);
        this.testResult.set(null);
        this.changed.emit();
    }

    isProviderSupported(provider: DotAiProviderMetadata): boolean {
        return provider.supportedCapabilities.includes(this.meta().capability);
    }

    providerCaption(provider: DotAiProviderMetadata): string {
        if (!this.isProviderSupported(provider)) {
            return this.dotMessageService.get(
                'apps.ai.provider.capability.unsupported',
                this.capabilityLabel()
            );
        }

        return provider.supportedCapabilities
            .map((c) => this.dotMessageService.get(CAPABILITY_LABELS[c]))
            .join(' · ');
    }

    displayNameFor(providerId: string): string {
        return displayName(providerId);
    }

    selectProvider(provider: DotAiProviderMetadata): void {
        if (!this.isProviderSupported(provider) || provider.provider === this.providerId()) {
            return;
        }

        this.providerId.set(provider.provider);
        this.testResult.set(null);
        this.rebuildFieldsGroup(provider.provider, {});
        this.changed.emit();
    }

    testConnection(): void {
        if (!this.isValid()) {
            this.markAllTouched();
            this.testResult.set({
                success: false,
                message: 'apps.ai.validation.required-fields-test'
            });

            return;
        }

        const section = this.buildPayloadSection();
        if (!section) {
            return;
        }

        this.testing.set(true);
        this.testResult.set(null);
        this.dotAiService
            .testConnection(this.meta().sectionKey, section, this.siteId())
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (result) => {
                    this.testing.set(false);
                    this.testResult.set(result);
                },
                error: (err) => {
                    this.testing.set(false);
                    this.testResult.set({
                        success: false,
                        message:
                            (err as { error?: { error?: string }; message?: string })?.error
                                ?.error ??
                            (err as { message?: string })?.message ??
                            'apps.ai.error.test-connection'
                    });
                }
            });
    }

    /**
     * Returns `null` when the capability is disabled (omitted from the saved payload so the
     * backend treats it as unconfigured), otherwise the assembled section value — including any
     * additional properties — regardless of what the currently selected provider's fields are.
     */
    buildPayloadSection(): DotAiCapabilitySectionValue | null {
        if (!this.enabled() || !this.providerId()) {
            return null;
        }

        const section: DotAiCapabilitySectionValue = { provider: this.providerId() as string };

        Object.entries(this.fieldsGroup().value as Record<string, unknown>).forEach(
            ([key, value]) => {
                if (value !== null && value !== undefined && value !== '') {
                    section[key] = value;
                }
            }
        );

        this.additionalProperties.controls.forEach((group) => {
            const key = group.value.key?.trim();
            if (key) {
                section[key] = group.value.value;
            }
        });

        return section;
    }

    isValid(): boolean {
        return !this.enabled() || this.fieldsGroup().valid;
    }

    markAllTouched(): void {
        this.fieldsGroup().markAllAsTouched();
    }

    private fieldsForCurrentProvider(): DotAiProviderField[] {
        return this.currentProviderMeta()?.fields[this.meta().capability] ?? [];
    }

    private defaultProviderId(): string | null {
        const supported = this.orderedProviders().filter((p) => this.isProviderSupported(p));

        return supported[0]?.provider ?? this.orderedProviders()[0]?.provider ?? null;
    }

    private hydrateFields(initial: DotAiCapabilitySectionValue): void {
        const fields = this.fieldsForCurrentProvider();
        const knownNames = new Set(fields.map((f) => f.name));

        this.rebuildFieldsGroup(this.providerId(), initial);

        Object.entries(initial).forEach(([key, value]) => {
            if (key === 'provider' || knownNames.has(key)) {
                return;
            }
            this.additionalProperties.push(
                new FormGroup({
                    key: new FormControl(key, { nonNullable: true }),
                    value: new FormControl(value == null ? '' : String(value), {
                        nonNullable: true
                    })
                })
            );
        });
    }

    private rebuildFieldsGroup(
        providerId: string | null,
        presetValues: Record<string, unknown>
    ): void {
        const providerMeta = this.providers().find((p) => p.provider === providerId);
        const fields = providerMeta?.fields[this.meta().capability] ?? [];

        const group = new FormGroup({});
        fields.forEach((field) => {
            const preset = presetValues[field.name];
            group.addControl(field.name, new FormControl(preset ?? null, fieldValidators(field)));
        });

        const requiredUnlessFields = fields.filter((field) => field.requiredUnless);

        // A control's initial status is computed in its own constructor, before Angular has
        // wired it into this group — so `requiredUnlessValidator`'s sibling lookup (via
        // `control.parent`) sees no parent yet and assumes the sibling is empty. Force one
        // recheck now that every control (and its parent link) exists, so a hydrated config
        // where only the sibling was saved doesn't render this field as falsely invalid on load.
        requiredUnlessFields.forEach((field) => {
            group.get(field.name)?.updateValueAndValidity({ onlySelf: true, emitEvent: false });
        });

        // From here on, a field with `requiredUnless` must re-validate whenever that sibling
        // changes — not just when its own value changes.
        requiredUnlessFields.forEach((field) => {
            const ownControl = group.get(field.name);
            const siblingControl = group.get(field.requiredUnless as string);
            siblingControl?.valueChanges
                .pipe(takeUntilDestroyed(this.destroyRef))
                .subscribe(() => ownControl?.updateValueAndValidity({ emitEvent: false }));
        });

        group.valueChanges
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(() => this.changed.emit());
        this.fieldsGroup.set(group);
    }
}

function fieldValidators(field: DotAiProviderField): ValidatorFn[] {
    if (field.required) {
        return [Validators.required];
    }

    if (field.requiredUnless) {
        return [requiredUnlessValidator(field.requiredUnless)];
    }

    return [];
}

function providerSortIndex(providerId: string): number {
    const index = PROVIDER_ORDER.indexOf(providerId);

    return index === -1 ? PROVIDER_ORDER.length : index;
}

function displayName(providerId: string): string {
    return PROVIDER_DISPLAY_NAMES[providerId] ?? providerId;
}
