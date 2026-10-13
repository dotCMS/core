import { of, Subscription } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    computed,
    inject,
    signal
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { BadgeModule } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { FileSelectEvent, FileUploadModule } from 'primeng/fileupload';
import { MessageModule } from 'primeng/message';
import { MultiSelectModule } from 'primeng/multiselect';
import { SelectModule } from 'primeng/select';
import { TooltipModule } from 'primeng/tooltip';

import { catchError, finalize, map, switchMap } from 'rxjs/operators';

import {
    DotAlertConfirmService,
    DotContentImportService,
    DotFieldService,
    DotMessageService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import {
    DotCMSClazzes,
    DotCMSContentTypeField,
    DotContentImportForm,
    DotImportContentType,
    DotImportJob,
    DotImportValidationMessage
} from '@dotcms/dotcms-models';
import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';
import { getDownloadLink } from '@dotcms/utils';

import { DotContentDriveStore } from '../../../store/dot-content-drive.store';

/** The language value that asks for a multilingual file, as Content Search's import sends it. */
export const MULTILINGUAL_LANGUAGE = '-1';

/**
 * Field kinds that can't be key fields, as Content Search's import leaves them out: relationships,
 * the relationship and permission tabs, binaries kept as files or images, and layout fields.
 */
const NON_KEY_FIELD_CLAZZES: ReadonlySet<string> = new Set([
    DotCMSClazzes.RELATIONSHIP,
    DotCMSClazzes.RELATIONSHIPS_TAB,
    DotCMSClazzes.PERMISSION_TAB,
    DotCMSClazzes.FILE,
    DotCMSClazzes.IMAGE,
    DotCMSClazzes.LINE_DIVIDER,
    DotCMSClazzes.TAB_DIVIDER,
    DotCMSClazzes.ROW,
    DotCMSClazzes.COLUMN
]);

/** Where the dialog is: filling the form, waiting for the preview, or reading it. */
type DotImportStep = 'form' | 'previewing' | 'preview';

/**
 * Orders preview messages for reading: messages about the whole file first, then by line.
 *
 * @param messages warnings or errors as the server returns them
 */
const byLine = (messages: DotImportValidationMessage[] = []): DotImportValidationMessage[] =>
    [...messages].sort(
        (a, b) => (a.row ?? Number.NEGATIVE_INFINITY) - (b.row ?? Number.NEGATIVE_INFINITY)
    );

/** A labelled choice for a select. */
interface DotImportOption {
    label: string;
    value: string;
}

/**
 * Imports content from a CSV into the Drive (#37896).
 *
 * The form offers only what an import can actually use: the content types the user can write to,
 * the workflow actions Content Search's import offers for that type, and its indexed fields as key
 * fields. Preview is never disabled; trying it with something missing says what is missing instead.
 */
@Component({
    selector: 'dot-content-drive-dialog-import',
    imports: [
        ReactiveFormsModule,
        BadgeModule,
        ButtonModule,
        CardModule,
        FileUploadModule,
        MessageModule,
        MultiSelectModule,
        SelectModule,
        TooltipModule,
        DotFieldRequiredDirective,
        DotMessagePipe
    ],
    templateUrl: './dot-content-drive-dialog-import.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'block' }
})
export class DotContentDriveDialogImportComponent {
    readonly #formBuilder = inject(FormBuilder);
    readonly #store = inject(DotContentDriveStore);
    readonly #importService = inject(DotContentImportService);
    readonly #workflowsService = inject(DotWorkflowsActionsService);
    readonly #fieldService = inject(DotFieldService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #alertConfirmService = inject(DotAlertConfirmService);
    readonly #destroyRef = inject(DestroyRef);
    #previewRequest?: Subscription;

    readonly importForm = this.#formBuilder.group({
        contentType: this.#formBuilder.control<string | null>(null, Validators.required),
        language: this.#formBuilder.control<string | null>(
            this.#workingLanguage(),
            Validators.required
        ),
        workflowActionId: this.#formBuilder.control<string | null>(null, Validators.required),
        fields: this.#formBuilder.nonNullable.control<string[]>([]),
        file: this.#formBuilder.control<File | null>(null, Validators.required)
    });

    /** The content types the user can import into, once loaded. */
    readonly $contentTypes = signal<DotImportContentType[]>([]);
    /**
     * The content types grouped under their base type, in the order the server sorted them, for a
     * single-select dropdown that keeps the Drive filter's base type → content type reading.
     */
    readonly $contentTypeGroups = computed(() => {
        const groups = new Map<string, DotImportContentType[]>();
        for (const type of this.$contentTypes()) {
            groups.set(type.baseType, [...(groups.get(type.baseType) ?? []), type]);
        }

        return [...groups.entries()].map(([baseType, items]) => ({
            label: this.#dotMessageService.get(`content-drive.base-type.${baseType.toLowerCase()}`),
            value: baseType,
            items
        }));
    });
    /** Whether the content type list has arrived, so an empty list can be told from a loading one. */
    readonly $contentTypesLoaded = signal(false);
    /** The picked type's workflow actions, labelled "action ( scheme )" as in Content Search. */
    readonly $workflowActions = signal<DotImportOption[]>([]);
    /** The picked type's fields that can serve as key fields. */
    readonly $keyFields = signal<DotCMSContentTypeField[]>([]);
    /** Whether the user has tried to preview, which is when missing inputs start being reported. */
    readonly $submitted = signal(false);
    /** Where the dialog is. */
    readonly $step = signal<DotImportStep>('form');
    /**
     * Why the last preview or import couldn't run, in the server's words (#37896, US5). Shown in
     * the dialog, with every input kept, so the user can fix the form or the file and try again.
     */
    readonly $error = signal<string | null>(null);
    /** The finished preview job, while the preview is shown. */
    readonly $preview = signal<DotImportJob | null>(null);

    /** The preview's headline numbers. */
    readonly $summary = computed(() => {
        const result = this.$preview()?.result;
        const parsedRows = result?.data?.processed?.parsedRows ?? 0;

        return {
            // The server counts the header line as a parsed row.
            rowsRead: Math.max(parsedRows - 1, 0),
            headersMatched: result?.file?.headers?.valid?.length ?? 0,
            keyFields: (result?.keyFields ?? []).join(', '),
            toCreate: result?.data?.summary?.created ?? 0,
            toUpdate: result?.data?.summary?.updated ?? 0,
            failed: result?.data?.summary?.failed ?? 0
        };
    });

    /** Where the rows will land, worded for the preview. */
    readonly $destinations = computed(() =>
        (this.$preview()?.result?.destinations ?? []).map((destination) => ({
            text: this.#dotMessageService.get(
                'content-drive.import.preview.destination.rows',
                String(destination.rows),
                destination.path
            ),
            source:
                destination.source === 'CONTENT_TYPE_DEFAULT'
                    ? this.#dotMessageService.get(
                          'content-drive.import.preview.destination.source.content-type-default'
                      )
                    : destination.source === 'EXISTING'
                      ? this.#dotMessageService.get(
                            'content-drive.import.preview.destination.source.existing'
                        )
                      : null
        }))
    );

    /**
     * The short notes under the Summary figures: where rows land, the key fields when there are
     * any, and the server's info lines. Variable by design — the server only adds the lines that
     * apply — minus the two that repeat the figures ("Import statistics…" and the header count).
     */
    readonly $summaryNotes = computed(() => {
        const result = this.$preview()?.result;
        const headersMatched = this.$summary().headersMatched;
        const notes: { text: string; testId: string }[] = this.$destinations().map(
            (destination) => ({
                text: destination.source
                    ? `${destination.text} (${destination.source})`
                    : destination.text,
                testId: 'import-destination'
            })
        );

        if (this.$summary().keyFields) {
            notes.push({
                text: `${this.#dotMessageService.get('Key-Fields')}: ${this.$summary().keyFields}`,
                testId: 'import-summary-key-fields'
            });
        }

        (result?.info ?? [])
            .map((info) => info.message)
            .filter(
                (message) =>
                    !message.startsWith('Import statistics') &&
                    !message.startsWith(`${headersMatched} `)
            )
            .forEach((text) => notes.push({ text, testId: 'import-summary-info-item' }));

        return notes;
    });

    /** "Will import" splits into new and updated content; the tooltip says how. */
    readonly $willImportTooltip = computed(
        () =>
            `${this.#dotMessageService.get('content-drive.import.preview.will-import.tooltip')} ` +
            this.#dotMessageService.get(
                'content-drive.import.preview.will-import.split',
                String(this.$summary().toCreate),
                String(this.$summary().toUpdate)
            )
    );

    /** The preview's warnings, file-wide ones first, then by line. */
    readonly $previewWarnings = computed(() => byLine(this.$preview()?.result?.warning));
    /** The preview's errors, file-wide ones first, then by line. */
    readonly $previewErrors = computed(() => byLine(this.$preview()?.result?.error));

    /** Every language, plus the multilingual option last. */
    readonly $languageOptions = computed<DotImportOption[]>(() => [
        ...this.#store.languages().map((language) => ({
            label: language.countryCode
                ? `${language.language} (${language.countryCode})`
                : language.language,
            value: String(language.id)
        })),
        {
            label: this.#dotMessageService.get('content-drive.import.language.multilingual'),
            value: MULTILINGUAL_LANGUAGE
        }
    ]);

    protected readonly $value = toSignal(this.importForm.valueChanges, {
        initialValue: this.importForm.getRawValue()
    });

    /** The picked CSV's name, shown in the drop area. */
    readonly $fileName = computed(() => this.$value().file?.name ?? null);

    /** The picked content type's name, for the "Download {name} sample" link. */
    readonly $selectedTypeName = computed(
        () =>
            this.$contentTypes().find((type) => type.variable === this.$value().contentType)
                ?.name ?? null
    );

    /**
     * The picked CSV's size, row and column counts, as "12 KB · 2 rows · 10 columns". Counts come
     * from reading the file in the browser, so they arrive a moment after the pick; when the file
     * can't be read, only the size is shown.
     */
    readonly $fileMeta = signal<string | null>(null);

    /** Whether the user asked for a multilingual file. */
    readonly $isMultilingual = computed(() => this.$value().language === MULTILINGUAL_LANGUAGE);

    /** Which inputs to flag, once the user has tried to preview. */
    readonly $fieldErrors = computed(() => {
        const value = this.$value();
        const submitted = this.$submitted();

        return {
            contentType: submitted && !value.contentType,
            language: submitted && !value.language,
            workflowActionId: submitted && !value.workflowActionId,
            file: submitted && !value.file,
            fields:
                submitted &&
                value.language === MULTILINGUAL_LANGUAGE &&
                (value.fields ?? []).length === 0
        };
    });

    constructor() {
        this.#importService
            .getWritableContentTypes()
            .pipe(
                catchError(() => of([] as DotImportContentType[])),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((types) => {
                this.$contentTypes.set(types);
                this.$contentTypesLoaded.set(true);
            });

        this.importForm.controls.contentType.valueChanges
            .pipe(
                switchMap((contentType) => {
                    // A workflow action or key field picked for the previous type means nothing
                    // for this one, so they go before the new type's options arrive.
                    this.importForm.patchValue({ workflowActionId: null, fields: [] });
                    this.$workflowActions.set([]);
                    this.$keyFields.set([]);

                    return contentType ? this.#loadTypeOptions(contentType) : of(null);
                }),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((options) => {
                if (options) {
                    this.$workflowActions.set(options.actions);
                    this.$keyFields.set(options.keyFields);
                }
            });

        // A preview describes one exact file and set of options; any change makes it stale, and
        // so does the reason the last attempt failed.
        this.importForm.valueChanges.pipe(takeUntilDestroyed(this.#destroyRef)).subscribe(() => {
            this.$error.set(null);
            this.backToForm();
        });

        this.#destroyRef.onDestroy(() => this.#previewRequest?.unsubscribe());
    }

    /**
     * Keeps the CSV the user picked. The picker only offers `.csv` files.
     *
     * @param event the file upload's selection
     */
    onFileSelect(event: FileSelectEvent): void {
        const file = event.currentFiles?.[0] ?? null;
        this.importForm.controls.file.setValue(file);
        this.#readFileMeta(file);
    }

    /** Forgets the picked CSV. */
    onFileClear(): void {
        this.importForm.controls.file.setValue(null);
        this.$fileMeta.set(null);
    }

    /**
     * Reads the picked CSV to describe it as "12 KB · 2 rows · 10 columns": the size at once, the
     * counts once the file arrives. Rows are the non-empty lines minus the header; columns, the
     * commas of the first line plus one.
     *
     * @param file the picked CSV
     */
    #readFileMeta(file: File | null): void {
        if (!file) {
            this.$fileMeta.set(null);

            return;
        }

        const kb = `${Math.max(Math.round(file.size / 1024), 1)} KB`;
        file.text()
            .then((text) => {
                const lines = text.split(/\r\n|\r|\n/).filter((line) => line.trim().length > 0);
                const rows = String(Math.max(lines.length - 1, 0));
                const columns = String(lines.length ? lines[0].split(',').length : 0);
                this.$fileMeta.set(
                    this.#dotMessageService.get('content-drive.import.file.meta', kb, rows, columns)
                );
            })
            .catch(() => this.$fileMeta.set(kb));
    }

    /**
     * Leaves the preview, or the wait for it, and returns to the form with every input kept.
     */
    backToForm(): void {
        this.#previewRequest?.unsubscribe();
        this.importForm.enable({ emitEvent: false });
        this.$preview.set(null);
        this.$step.set('form');
    }

    /**
     * Asks the user to confirm the import. When the preview found errors, the question says those
     * rows will be skipped, as Content Search does; otherwise it is Content Search's usual question.
     */
    confirmImport(): void {
        const errorCount = this.$previewErrors().length;
        const message = errorCount
            ? this.#dotMessageService.get(
                  'content-drive.import.preview.errors-will-be-skipped',
                  String(errorCount)
              )
            : this.#dotMessageService.get('message.contentlet.proceed.importing');

        this.#alertConfirmService.confirm({
            header: this.#dotMessageService.get('Import-Content'),
            message,
            accept: () => this.#startImport()
        });
    }

    /**
     * Starts the preview, or flags what is missing.
     *
     * Nothing is sent while a required input is empty, or while a multilingual file has no key
     * field: Content Search refuses both, and saying so here is quicker than a failed job.
     */
    preview(): void {
        this.$submitted.set(true);

        if (Object.values(this.$fieldErrors()).some(Boolean)) {
            return;
        }

        const { file } = this.importForm.getRawValue();
        this.#previewRequest?.unsubscribe();
        this.$error.set(null);
        this.$step.set('previewing');
        // Locked, not hidden: the user sees what is being previewed while it runs.
        this.importForm.disable({ emitEvent: false });
        this.#previewRequest = this.#importService
            .preview(file as File, this.#requestForm())
            .pipe(finalize(() => this.importForm.enable({ emitEvent: false })))
            .subscribe({
                next: (job) => {
                    if (job.state !== 'SUCCESS') {
                        this.#fail(job.result?.error?.[0]?.message);

                        return;
                    }

                    this.$preview.set(job);
                    this.$step.set('preview');
                },
                error: (error: HttpErrorResponse) => this.#fail(this.#reasonOf(error))
            });
    }

    /**
     * Starts the import with exactly the previewed file and options, then hands the run to the
     * status toast and closes: the import runs in the background (#37896, FR-019).
     */
    #startImport(): void {
        const { file } = this.importForm.getRawValue();

        this.$error.set(null);
        this.#importService
            .importContent(file as File, this.#requestForm())
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe({
                next: ({ jobId }) => {
                    this.#store.startImportRun(jobId, (file as File).name);
                    this.#store.closeDialog();
                },
                // The dialog stays open on the preview, so the user can go back and fix it.
                error: (error: HttpErrorResponse) =>
                    this.$error.set(this.#reasonOf(error) ?? this.#genericFailure())
            });
    }

    /**
     * Downloads the sample CSV for the picked content type, as Content Search names it.
     */
    downloadSample(): void {
        const { contentType } = this.importForm.getRawValue();
        if (!contentType) {
            return;
        }

        this.#importService
            .downloadSample(contentType)
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe((blob) => getDownloadLink(blob, 'CSV_Template.csv').click());
    }

    /**
     * Returns to the form, keeping every input, and says why the attempt failed.
     *
     * @param reason the server's message, when it gave one
     */
    #fail(reason: string | undefined): void {
        this.$preview.set(null);
        this.$step.set('form');
        this.$error.set(reason ?? this.#genericFailure());
    }

    /**
     * The server's own reason for refusing a preview or an import. A refusal comes back as a
     * failed job (`entity.result.error`); anything else may carry a plain `message`.
     *
     * @param error the HTTP failure
     */
    #reasonOf(error: HttpErrorResponse): string | undefined {
        const body = error?.error;

        return body?.entity?.result?.error?.[0]?.message ?? body?.message ?? undefined;
    }

    /** The message used when the server gave no reason of its own. */
    #genericFailure(): string {
        return this.#dotMessageService.get('content-drive.import.error.generic');
    }

    /** The import options as the server takes them, with the site the Drive is browsing. */
    #requestForm(): DotContentImportForm {
        const { contentType, language, workflowActionId, fields } = this.importForm.getRawValue();

        return {
            contentType: contentType as string,
            language: language as string,
            workflowActionId: workflowActionId as string,
            fields,
            site: this.#store.currentSite()?.identifier as string
        };
    }

    /** Closes the dialog without importing anything. */
    closeDialog(): void {
        this.#store.closeDialog();
    }

    /**
     * The language the user is working in: the one language the Drive is filtered by, or the
     * default language when the filter holds none or several.
     */
    #workingLanguage(): string | null {
        const filter = this.#store.getFilterValue('languageId');
        const selected = Array.isArray(filter) ? filter : filter ? [filter] : [];

        if (selected.length === 1) {
            return selected[0];
        }

        const defaultLanguageId = this.#store.defaultLanguageId();

        return defaultLanguageId === undefined || defaultLanguageId === null
            ? null
            : String(defaultLanguageId);
    }

    /**
     * Loads what depends on the picked content type: its workflow actions and its key fields.
     *
     * @param contentType the picked content type's variable
     */
    #loadTypeOptions(contentType: string) {
        const actions$ = this.#workflowsService.getDefaultActions(contentType).pipe(
            map((actions) =>
                actions.map(({ action, scheme }) => ({
                    label: `${action.name} ( ${scheme.name} )`,
                    value: action.id
                }))
            ),
            catchError(() => of([] as DotImportOption[]))
        );

        const keyFields$ = this.#fieldService.getFields(contentType, 'SYSTEM_INDEXED').pipe(
            map((fields) =>
                fields.filter((field) => field.indexed && !NON_KEY_FIELD_CLAZZES.has(field.clazz))
            ),
            catchError(() => of([] as DotCMSContentTypeField[]))
        );

        return actions$.pipe(
            switchMap((actions) => keyFields$.pipe(map((keyFields) => ({ actions, keyFields }))))
        );
    }
}
