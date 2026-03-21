import { EMPTY, forkJoin, merge } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    input,
    OnChanges,
    output,
    SimpleChanges,
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
export class DotCustomFieldSettingsComponent implements OnChanges {
    readonly $field = input.required<DotCMSContentTypeField>({ alias: 'field' });
    readonly $isVisible = input<boolean>(false, { alias: 'isVisible' });
    /** Live render mode from the properties form — overrides saved fieldVariables when provided */
    readonly $renderMode = input<string | undefined>(undefined, { alias: 'renderMode' });

    readonly $changeControls = output<DotDialogActions>();
    readonly $save = output<void>();
    readonly $valid = output<boolean>();

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

    readonly #dotHttpErrorManagerService = inject(DotHttpErrorManagerService);
    readonly #dotMessageService = inject(DotMessageService);

    constructor() {
        merge(
            toObservable(this.renderOptions).pipe(switchMap((s) => s?.valueChanges$ ?? EMPTY)),
            toObservable(this.hideLabel).pipe(switchMap((s) => s?.valueChanges$ ?? EMPTY))
        )
            .pipe(takeUntilDestroyed())
            .subscribe(() => this.$valid.emit(this.#canSave()));
    }

    ngOnChanges(changes: SimpleChanges): void {
        const { $isVisible } = changes;
        if ($isVisible?.currentValue) {
            this.$changeControls.emit(this.#dialogActions());
        }
    }

    saveSettings(): void {
        const sections = this.#activeSections();
        const saveActions = sections.filter((s) => s.isDirty).map((s) => s.save(this.$field()));

        if (saveActions.length === 0) {
            return;
        }

        forkJoin(saveActions)
            .pipe(
                take(1),
                catchError((err) => this.#dotHttpErrorManagerService.handle(err).pipe(take(1)))
            )
            .subscribe(() => this.$save.emit());
    }

    #activeSections(): FieldSettingsSection[] {
        return [this.renderOptions(), this.hideLabel()].filter(
            (s): s is DotRenderOptionsSettingsComponent | DotHideLabelSettingsComponent => s != null
        );
    }

    #canSave(): boolean {
        const sections = this.#activeSections();

        return sections.some((s) => s.isDirty) && sections.every((s) => s.$isValid());
    }

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
