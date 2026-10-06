import { EMPTY, forkJoin, merge } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    inject,
    input,
    OnChanges,
    OnInit,
    output,
    SimpleChanges,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';

import { switchMap, take } from 'rxjs/operators';

import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import {
    DotCMSContentTypeField,
    DotDialogActions,
    DotRenderModes,
    NEW_RENDER_MODE_VARIABLE_KEY
} from '@dotcms/dotcms-models';

import { DotHideLabelSettingsComponent } from './sections/dot-hide-label-settings';
import { DotRenderOptionsSettingsComponent } from './sections/dot-render-options-settings';
import { FieldSettingsSection } from './sections/field-settings-section';

/**
 * Orchestrator panel for the "Settings" tab of the Custom Field dialog.
 * Hosts all {@link FieldSettingsSection} sub-components (render options, hide-label, etc.),
 * aggregates their dirty/valid state, and exposes Save/Cancel dialog controls to the parent.
 */
@Component({
    selector: 'dot-custom-field-settings',
    imports: [DotRenderOptionsSettingsComponent, DotHideLabelSettingsComponent],
    templateUrl: './dot-custom-field-settings.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCustomFieldSettingsComponent implements OnChanges, OnInit {
    /** The content-type field whose settings are being edited. */
    readonly $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    /** Controls whether the settings panel is rendered. Dialog controls are emitted on show. */
    readonly $isVisible = input<boolean>(false, { alias: 'isVisible' });

    /** Live render mode from the properties form — overrides saved fieldVariables when provided. */
    readonly $renderMode = input<string | undefined>(undefined, { alias: 'renderMode' });

    /** Emits updated dialog accept/cancel actions whenever the panel becomes visible or state changes. */
    readonly $changeControls = output<DotDialogActions>();

    /** Emits after all dirty sections have been saved successfully. */
    readonly $save = output<void>();

    /** Emits the current saveable state (`true` when at least one section is dirty and all are valid). */
    readonly $valid = output<boolean>();

    /**
     * Whether the field is currently configured to render in an iframe.
     * Derived from the live `$renderMode` input when present, otherwise from the persisted field variable.
     */
    protected readonly $isIframeMode = computed(() => {
        const liveMode = this.$renderMode();

        if (liveMode !== undefined) {
            return liveMode === DotRenderModes.IFRAME;
        }

        const renderModeVar = (this.$field().fieldVariables ?? []).find(
            (v) => v.key === NEW_RENDER_MODE_VARIABLE_KEY
        );

        return !renderModeVar || renderModeVar.value === DotRenderModes.IFRAME;
    });

    private readonly renderOptions = viewChild(DotRenderOptionsSettingsComponent);
    private readonly hideLabel = viewChild(DotHideLabelSettingsComponent);

    readonly #destroyRef = inject(DestroyRef);
    readonly #dotHttpErrorManagerService = inject(DotHttpErrorManagerService);
    readonly #dotMessageService = inject(DotMessageService);

    readonly #renderOptions$ = toObservable(this.renderOptions);
    readonly #hideLabel$ = toObservable(this.hideLabel);

    ngOnInit(): void {
        merge(
            this.#renderOptions$.pipe(switchMap((s) => s?.valueChanges$ ?? EMPTY)),
            this.#hideLabel$.pipe(switchMap((s) => s?.valueChanges$ ?? EMPTY))
        )
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe(() => this.$valid.emit(this.#canSave()));
    }

    ngOnChanges(changes: SimpleChanges): void {
        const { $isVisible } = changes;
        if ($isVisible?.currentValue) {
            this.$changeControls.emit(this.#dialogActions());
        }
    }

    /**
     * Saves all dirty sections in parallel via `forkJoin`.
     * Emits `$save` on success or delegates errors to `DotHttpErrorManagerService`.
     */
    saveSettings(): void {
        const sections = this.#activeSections();
        const saveActions = sections.filter((s) => s.isDirty).map((s) => s.save(this.$field()));

        if (saveActions.length === 0) {
            return;
        }

        forkJoin(saveActions)
            .pipe(take(1))
            .subscribe({
                next: () => this.$save.emit(),
                error: (err) =>
                    this.#dotHttpErrorManagerService.handle(err).pipe(take(1)).subscribe()
            });
    }

    /** Returns only the section components that are currently present in the DOM. */
    #activeSections(): FieldSettingsSection[] {
        return [this.renderOptions(), this.hideLabel()].filter(
            (s): s is DotRenderOptionsSettingsComponent | DotHideLabelSettingsComponent => s != null
        );
    }

    /** `true` when at least one section is dirty and every section is valid. */
    #canSave(): boolean {
        const sections = this.#activeSections();

        return sections.some((s) => s.isDirty) && sections.every((s) => s.$isValid());
    }

    /** Builds the accept/cancel actions for the parent dialog. */
    #dialogActions(): DotDialogActions {
        return {
            accept: {
                action: () => this.saveSettings(),
                label: this.#dotMessageService.get('contenttypes.dropzone.action.save'),
                disabled: !this.#canSave()
            },
            cancel: {
                label: this.#dotMessageService.get('contenttypes.dropzone.action.cancel')
            }
        };
    }
}
