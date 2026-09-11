import { Observable, Subject, forkJoin, of } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import {
    ChangeDetectionStrategy,
    Component,
    OnChanges,
    OnDestroy,
    SimpleChanges,
    computed,
    inject,
    input,
    output,
    signal
} from '@angular/core';

import { catchError, finalize, take, takeUntil, tap } from 'rxjs/operators';

import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import {
    CUSTOM_FIELD_OPTIONS_KEY,
    DotCMSClazzes,
    DotCMSContentTypeField,
    DotDialogActions,
    DotFieldVariable,
    HIDE_LABEL_VARIABLE_KEY
} from '@dotcms/dotcms-models';
import { DotKeyValueComponent } from '@dotcms/ui';

import { DotFieldVariablesService } from './services/dot-field-variables.service';

import { DotKeyValue } from '../../../../../../shared/models/dot-key-value-ng/dot-key-value-ng.model';

/**
 * Displays and manages free-form field variables for a content-type field.
 * Filters out reserved keys that are managed by dedicated settings sections
 * (e.g. `customFieldOptions`, `hideLabel` for Custom Fields; `allowedBlocks` for Block Editor).
 *
 * Edits are held here until Save. The tab used to write every add, edit and remove
 * straight to the server, which left no way back from a mistake and no way to walk
 * away from a half-finished set of changes — Cancel simply closed a dialog whose work
 * was already done. Nothing leaves this component now until {@link saveChanges} runs,
 * so Cancel means what it says.
 */
@Component({
    selector: 'dot-content-type-fields-variables',
    templateUrl: './dot-content-type-fields-variables.component.html',
    imports: [DotKeyValueComponent],
    providers: [DotFieldVariablesService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentTypeFieldsVariablesComponent implements OnChanges, OnDestroy {
    private dotHttpErrorManagerService = inject(DotHttpErrorManagerService);
    #dotMessageService = inject(DotMessageService);
    private fieldVariablesService = inject(DotFieldVariablesService);

    /** The content-type field whose variables are loaded and managed. */
    readonly $field = input<DotCMSContentTypeField>(undefined, { alias: 'field' });

    /** When `false`, hides the key-value table (used to embed without the table UI). */
    readonly $showTable = input<boolean>(true, { alias: 'showTable' });

    /** Hands the dialog its footer buttons while this tab is the one on screen. */
    readonly changeControls = output<DotDialogActions>();

    /** Raised once every pending change has been written. */
    readonly save = output<void>();

    /** Local snapshot of the field, updated on every `$field` change. */
    field: DotCMSContentTypeField;

    /** Signal holding the list of variables currently shown in the table. */
    $fieldVariables = signal<DotFieldVariable[]>([]);

    /** What the server last gave us, to diff the pending edits against. */
    #stored = signal<DotFieldVariable[]>([]);

    /** True while a save is in flight, so the button cannot be pressed twice. */
    #saving = signal(false);

    /**
     * Whether anything on screen differs from what is stored.
     *
     * Compared by key and value rather than by reference: re-adding a pair that was
     * just removed leaves the field exactly as it was, and that should not count as a
     * change to save.
     */
    readonly $hasChanges = computed(() => {
        const asText = (variables: DotKeyValue[]) =>
            JSON.stringify(variables.map(({ key, value }) => [key, value]));

        return asText(this.$fieldVariables()) !== asText(this.#stored());
    });

    /**
     * Per-field-type map of variable keys that must be hidden from the table.
     * These keys are owned by dedicated settings sections and should not be edited here.
     */
    blackList = {
        'com.dotcms.contenttype.model.field.ImmutableStoryBlockField': {
            allowedBlocks: true
            // contentAssets: true
        },
        'com.dotcms.contenttype.model.field.ImmutableBinaryField': {
            accept: true,
            systemOptions: true
        },
        [DotCMSClazzes.CUSTOM_FIELD]: {
            [CUSTOM_FIELD_OPTIONS_KEY]: true,
            [HIDE_LABEL_VARIABLE_KEY]: true
        }
    };

    private destroy$: Subject<boolean> = new Subject<boolean>();

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.$field?.currentValue) {
            this.field = this.$field();
            this.initTableData();
        }

        // The dialog owns the footer, so the buttons are handed over whenever this tab
        // becomes the visible one — the same way the Settings tabs do it.
        if (changes.$showTable?.currentValue) {
            this.#emitDialogActions();
        }
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Takes the whole list from the editor after any change.
     *
     * One channel rather than the per-row `save`/`update`/`delete` outputs: with
     * nothing being written yet, all this needs is the list as it now stands.
     */
    onVariablesChanged(variables: DotKeyValue[]): void {
        this.$fieldVariables.set(variables as DotFieldVariable[]);
        this.#emitDialogActions();
    }

    /**
     * Writes every pending change, then reports back so the dialog can close.
     *
     * Removals go out alongside the writes: a key that is gone from the list but still
     * stored has to be deleted, and a key whose value changed is saved over. Untouched
     * pairs are left alone rather than re-sent.
     */
    saveChanges(): void {
        const stored = this.#stored();
        const current = this.$fieldVariables();

        const removed = stored.filter(({ key }) => !current.some((item) => item.key === key));
        const written = current.filter((item) => {
            const previous = stored.find(({ key }) => key === item.key);

            return !previous || previous.value !== item.value;
        });

        if (!removed.length && !written.length) {
            this.save.emit();

            return;
        }

        this.#saving.set(true);
        this.#emitDialogActions();

        /*
         * Each write is tracked on its own so a partial failure stays recoverable.
         * `forkJoin` fails the whole batch on the first error, and the earlier version
         * left `#stored` untouched when that happened — but some deletes had already
         * gone through. The next Save then re-issued a DELETE for a variable that was
         * gone, which the endpoint answers with 404 (verified against the API), so the
         * tab could never save again.
         *
         * Advancing `#stored` per succeeded operation means a retry only sends what is
         * still outstanding.
         */
        const succeeded: DotFieldVariable[] = [];
        const failed: HttpErrorResponse[] = [];

        const track = (
            source: Observable<DotFieldVariable>,
            onDone: (result: DotFieldVariable) => void
        ) =>
            source.pipe(
                take(1),
                tap(onDone),
                catchError((err: HttpErrorResponse) => {
                    failed.push(err);

                    return of(null);
                })
            );

        const operations = [
            ...removed.map((variable) =>
                track(this.fieldVariablesService.delete(this.field, variable), () =>
                    succeeded.push(variable)
                )
            ),
            /*
             * What is recorded is the *response*, not the request: a pair added in this
             * session has no `id` until the server assigns one. Recording the request
             * would leave `#stored` holding an id-less pair, and deleting it after a
             * partial failure would go out as `.../variables/id/undefined`.
             */
            ...written.map((variable) =>
                track(this.fieldVariablesService.save(this.field, variable), (saved) =>
                    succeeded.push({ ...variable, ...saved })
                )
            )
        ];

        forkJoin(operations)
            .pipe(
                take(1),
                takeUntil(this.destroy$),
                finalize(() => {
                    this.#saving.set(false);
                    this.#emitDialogActions();
                })
            )
            .subscribe(() => {
                this.#stored.set(this.#storedAfter(stored, current, succeeded));

                if (failed.length) {
                    this.dotHttpErrorManagerService.handle(failed[0]).pipe(take(1)).subscribe();

                    return;
                }

                this.save.emit();
            });
    }

    /**
     * What the server holds once the operations that landed have been applied.
     *
     * Built from what is on screen rather than from the previous snapshot, so an added
     * pair that landed is recorded too — otherwise a retry after a partial failure
     * re-sent it. Anything whose write failed keeps its previous value, and a removal
     * the server refused stays, so a retry sends exactly what is still outstanding.
     *
     * A pair that landed is taken from `succeeded` rather than from the screen, because
     * only the former carries the `id` the server assigned it.
     */
    #storedAfter(
        stored: DotFieldVariable[],
        current: DotFieldVariable[],
        succeeded: DotFieldVariable[]
    ): DotFieldVariable[] {
        const landed = new Map(succeeded.map((item) => [item.key, item]));

        const onScreen = current
            .map(
                (item) => landed.get(item.key) ?? stored.find(({ key }) => key === item.key) ?? null
            )
            .filter((item): item is DotFieldVariable => item !== null);

        const notRemoved = stored.filter(
            ({ key }) => !current.some((item) => item.key === key) && !landed.has(key)
        );

        return [...onScreen, ...notRemoved];
    }

    /**
     * Rebuilds the dialog's Save button for the current state.
     *
     * Only while this tab is the one on screen. The dialog has a single Save, so
     * handing it over from a hidden tab replaces the Overview one — its button then
     * saved variables instead of the field, and the field was never written.
     */
    #emitDialogActions(): void {
        if (!this.$showTable()) {
            return;
        }

        this.changeControls.emit({
            accept: {
                label: this.#dotMessageService.get('contenttypes.dropzone.action.save'),
                action: () => this.saveChanges(),
                disabled: this.#saving() || !this.$hasChanges()
            }
        });
    }

    private initTableData(): void {
        if (!this.field?.contentTypeId || !this.field?.id) {
            this.$fieldVariables.set([]);
            this.#stored.set([]);

            return;
        }

        this.fieldVariablesService
            .load(this.field)
            .pipe(takeUntil(this.destroy$))
            .subscribe(($fieldVariables: DotFieldVariable[]) => {
                const visible = $fieldVariables.filter((item) => {
                    const fieldBlackList = this.blackList[this.field.clazz];
                    if (fieldBlackList) {
                        return !fieldBlackList[item?.key];
                    }

                    return true;
                });

                this.$fieldVariables.set(visible);
                this.#stored.set(visible);
                this.#emitDialogActions();
            });
    }
}
