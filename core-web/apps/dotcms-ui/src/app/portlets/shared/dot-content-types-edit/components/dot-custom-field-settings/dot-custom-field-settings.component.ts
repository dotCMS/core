import { EMPTY, forkJoin, merge } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    input,
    output,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';

import { catchError, switchMap, take } from 'rxjs/operators';

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

@Component({
    selector: 'dot-custom-field-settings',
    imports: [DotRenderOptionsSettingsComponent, DotHideLabelSettingsComponent],
    templateUrl: './dot-custom-field-settings.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCustomFieldSettingsComponent {
    readonly $field = input.required<DotCMSContentTypeField>({ alias: 'field' });
    readonly $isVisible = input<boolean>(false, { alias: 'isVisible' });
    /** Live render mode from the properties form — overrides saved fieldVariables when provided */
    readonly $renderMode = input<string | undefined>(undefined, { alias: 'renderMode' });

    readonly $save = output<void>();
    readonly $valid = output<boolean>();
    readonly $changeControls = output<DotDialogActions>();

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

    readonly #dotMessageService = inject(DotMessageService);
    readonly #dotHttpErrorManagerService = inject(DotHttpErrorManagerService);

    constructor() {
        effect(() => {
            if (this.$isVisible()) {
                this.$changeControls.emit(this.#dialogActions());
            }
        });

        merge(
            toObservable(this.renderOptions).pipe(switchMap((s) => s?.valueChanges$ ?? EMPTY)),
            toObservable(this.hideLabel).pipe(switchMap((s) => s?.valueChanges$ ?? EMPTY))
        )
            .pipe(takeUntilDestroyed())
            .subscribe(() => this.$valid.emit(this.#canSave()));
    }

    saveSettings(): void {
        const sections = this.#activeSections();
        const saveActions = sections.filter((s) => s.isDirty).map((s) => s.save(this.$field()));

        forkJoin(saveActions)
            .pipe(
                take(1),
                catchError((err) => this.#dotHttpErrorManagerService.handle(err).pipe(take(1)))
            )
            .subscribe(() => this.$save.emit());
    }

    #activeSections(): FieldSettingsSection[] {
        return [this.renderOptions(), this.hideLabel()].filter(
            (s): s is DotRenderOptionsSettingsComponent | DotHideLabelSettingsComponent =>
                s != null
        );
    }

    #canSave(): boolean {
        const sections = this.#activeSections();

        return sections.some((s) => s.isDirty) && sections.every((s) => s.isValid());
    }

    #dialogActions(): DotDialogActions {
        return {
            cancel: {
                label: this.#dotMessageService.get('contenttypes.dropzone.action.cancel')
            },
            accept: {
                action: () => this.saveSettings(),
                disabled: !this.#canSave(),
                label: this.#dotMessageService.get('contenttypes.dropzone.action.save')
            }
        };
    }
}
