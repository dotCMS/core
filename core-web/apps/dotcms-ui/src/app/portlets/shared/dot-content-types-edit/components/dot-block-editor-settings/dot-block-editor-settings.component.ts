import { forkJoin, of, Subject } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import {
    ChangeDetectionStrategy,
    Component,
    inject,
    input,
    OnChanges,
    OnDestroy,
    OnInit,
    output,
    SimpleChanges
} from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';

import { catchError, take, takeUntil, tap } from 'rxjs/operators';

import { getEditorBlockOptions } from '@dotcms/block-editor';
import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import {
    DotCMSContentTypeField,
    DotDialogActions,
    DotFieldVariable,
    REMOTE_BLOCK_NAME_REQUIRED_WARNING
} from '@dotcms/dotcms-models';

import { DotFieldVariablesService } from '../fields/dot-content-type-fields-variables/services/dot-field-variables.service';

/**
 * A selectable block.
 *
 * Both fields are optional because `getEditorBlockOptions` maps from `DotMenuItem`, whose `label`
 * and `id` are — its own sort already reads the label as `label ?? ''`.
 */
type BlockOption = { label?: string; code?: string };

/** One row of {@link DotBlockEditorSettingsComponent.settingsMap}. */
interface BlockEditorSetting {
    label: string;
    placeholder: string;
    options: BlockOption[];
    key: string;
    variable: DotFieldVariable | null;
}

function getCustomBlockOptions(field: DotCMSContentTypeField): BlockOption[] {
    const raw = field?.fieldVariables?.find((variable) => variable.key === 'customBlocks')?.value;

    if (!raw || raw.trim().length === 0) {
        return [];
    }

    try {
        const parsed = JSON.parse(raw) as {
            extensions?: Array<{ actions?: Array<{ menuLabel?: string; name?: string }> }>;
        };

        if (!parsed || typeof parsed !== 'object' || !Array.isArray(parsed.extensions)) {
            return [];
        }

        return (parsed.extensions || []).flatMap((extension) =>
            (extension.actions || []).flatMap((action) => {
                const name = action?.name?.trim();

                if (!name) {
                    console.warn(REMOTE_BLOCK_NAME_REQUIRED_WARNING);

                    return [];
                }

                // `menuLabel` was optional in existing payloads; when it is missing, empty,
                // or whitespace-only, fall back to the required TipTap node name so
                // preserved remote blocks remain selectable in settings. Computed after the guard
                // above, which is what makes the fallback a `string` rather than `string |
                // undefined`.
                const label = action?.menuLabel?.trim() || name;

                return [{ code: name, label }];
            })
        );
    } catch (error) {
        console.warn('[remote-extension] failed to parse customBlocks for settings', error);

        return [];
    }
}

function getAllowedBlockOptions(field: DotCMSContentTypeField): BlockOption[] {
    const builtInOptions = getEditorBlockOptions();
    const seenCodes = new Set(builtInOptions.map((option) => option.code));
    const customOptions = getCustomBlockOptions(field).filter((option) => {
        if (seenCodes.has(option.code)) {
            return false;
        }

        seenCodes.add(option.code);

        return true;
    });

    return [...builtInOptions, ...customOptions];
}

/* Uncomment this when Content Assets variable is ready
const BLOCK_EDITOR_ASSETS = [
    { label: 'Youtube Videos', code: 'videos'},
    { label: 'Images', code: 'images'},
    { label: 'PDF Files', code: 'PDF'}
]
*/

@Component({
    selector: 'dot-block-editor-settings',
    templateUrl: './dot-block-editor-settings.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class DotBlockEditorSettingsComponent implements OnInit, OnDestroy, OnChanges {
    private readonly dotHttpErrorManagerService = inject(DotHttpErrorManagerService);
    private readonly fieldVariablesService = inject(DotFieldVariablesService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly fb = inject(FormBuilder);

    readonly $changeControls = output<DotDialogActions>();
    readonly $valid = output<boolean>();
    readonly $save = output<DotFieldVariable[]>();

    readonly $field = input.required<DotCMSContentTypeField>({ alias: 'field' });
    readonly $isVisible = input<boolean>(false, { alias: 'isVisible' });
    public form!: FormGroup;
    public settingsMap: { allowedBlocks: BlockEditorSetting } = {
        allowedBlocks: {
            label: 'Allowed Blocks',
            placeholder: 'Select Blocks',
            options: [] as BlockOption[],
            key: 'allowedBlocks',
            variable: null
        }
        /* Uncomment this when Content Assets variable is ready
        contentAssets: {
            label: 'Allowed Content Assets',
            placeholder: 'Select Assets',
            options: BLOCK_EDITOR_ASSETS,
            key: 'contentAssets',
            required: true
        }
        */
    };
    private destroy$: Subject<boolean> = new Subject<boolean>();

    get settings() {
        return Object.values(this.settingsMap);
    }

    ngOnInit(): void {
        this.settingsMap.allowedBlocks.options = getAllowedBlockOptions(this.$field());
        this.form = this.fb.group({
            allowedBlocks: [null]
            /* Uncomment this when Content Assets variable is ready
            contentAssets: [null, Validators.required]
            */
        });

        this.fieldVariablesService
            .load(this.$field())
            .pipe(take(1))
            .subscribe((fieldVariables: DotFieldVariable[]) => {
                fieldVariables.forEach((variable) => {
                    const { key, value } = variable;
                    // Matched against the settings rather than indexed by key: the key comes from
                    // the saved field variable, which need not name a setting this form shows.
                    const setting = this.settings.find((item) => item.key === key);
                    const control = this.form.get(key);

                    if (setting && control) {
                        setting.variable = variable;
                        control.setValue(value.split(','));
                    }
                });
            });

        this.form.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.$valid.emit(this.form.valid);
        });
    }

    ngOnChanges(changes: SimpleChanges) {
        const { $field, $isVisible } = changes;
        if ($field?.currentValue) {
            this.settingsMap.allowedBlocks.options = getAllowedBlockOptions($field.currentValue);
        }

        if ($isVisible?.currentValue) {
            this.$changeControls.emit(this.dialogActions());
        }
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    saveSettings(): void {
        forkJoin(
            this.settings.map((setting) => {
                const { variable, key } = setting;
                const value = this.form.get(key)?.value?.join(',');
                const fieldVariable = {
                    ...variable,
                    key,
                    value
                };

                // This is to prevent endpoints from breaking.
                // fieldVariablesService.save -> breaks if there is not current value.
                // fieldVariablesService.delete -> breaks if there is not previus exinting variable.
                if (!value && !variable) {
                    return of({});
                }

                return (
                    value
                        ? this.fieldVariablesService.save(this.$field(), fieldVariable)
                        : this.fieldVariablesService.delete(this.$field(), fieldVariable)
                ).pipe(tap((saved) => (setting.variable = saved))); // Update Variable Reference
            })
        )
            .pipe(
                take(1),
                catchError((err: HttpErrorResponse) =>
                    this.dotHttpErrorManagerService.handle(err).pipe(take(1))
                )
            )
            .subscribe((value) => {
                this.$save.emit(value as DotFieldVariable[]);
                this.form.markAsPristine();
            });
    }

    private dialogActions() {
        return {
            accept: {
                action: () => this.saveSettings(),
                label: this.dotMessageService.get('contenttypes.dropzone.action.save'),
                disabled: this.form.invalid || this.form.pristine
            },
            cancel: {
                label: this.dotMessageService.get('contenttypes.dropzone.action.cancel')
            }
        };
    }
}
