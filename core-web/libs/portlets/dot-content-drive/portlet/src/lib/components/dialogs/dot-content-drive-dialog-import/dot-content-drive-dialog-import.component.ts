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

import { AccordionModule } from 'primeng/accordion';
import { ButtonModule } from 'primeng/button';
import { FileSelectEvent, FileUploadModule } from 'primeng/fileupload';
import { MessageModule } from 'primeng/message';
import { MultiSelectModule } from 'primeng/multiselect';
import { SelectModule } from 'primeng/select';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';

import {
    catchError,
    distinctUntilChanged,
    finalize,
    map,
    startWith,
    switchMap
} from 'rxjs/operators';

import {
    DotAlertConfirmService,
    DotContentImportService,
    DotContentTypeService,
    DotFieldService,
    DotMessageService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import {
    DotCMSClazzes,
    DotCMSContentletWorkflowActions,
    DotCMSContentTypeField,
    DotContentImportForm,
    DotImportJob,
    DotImportValidationMessage
} from '@dotcms/dotcms-models';
import {
    DotContentTypeComponent,
    DotContentTypePageLoader,
    DotFieldRequiredDirective,
    DotMessagePipe
} from '@dotcms/ui';
import { getDownloadLink } from '@dotcms/utils';

import { ROOT_PATH, SYSTEM_HOST_PATH } from '../../../shared/constants';
import { DotContentDriveStore } from '../../../store/dot-content-drive.store';

/** The Host content type's variable: sites are not imported from a CSV, as in Content Search. */
const HOST_CONTENT_TYPE = 'Host';

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
 * A workflow action choice. `scheme` names the workflow scheme the action belongs to, which is
 * what tells two actions of the same name apart.
 */
interface DotImportActionOption extends DotImportOption {
    scheme: string;
}

/**
 * Labels workflow actions with their name and their scheme, in the order the server lists them.
 * Actions without a Save step are left out: they only run on content that already exists, so
 * every row an import would create fails with them ("The content must be saved").
 */
const toActionOptions = (actions: DotCMSContentletWorkflowActions[]): DotImportActionOption[] =>
    actions
        .filter(({ action }) => action.hasSaveActionlet !== false)
        .map(({ action, scheme }) => ({
            label: action.name,
            value: action.id,
            scheme: scheme.name
        }));

/**
 * Imports content from a CSV into the Drive (#37896).
 *
 * The form offers only what an import can actually use: the content types the user can write to
 * (in the shared content type selector, paged by the server),
 * the workflow actions Content Search's import offers for that type, and its indexed fields as key
 * fields. Preview is never disabled; trying it with something missing says what is missing instead.
 */
@Component({
    selector: 'dot-content-drive-dialog-import',
    imports: [
        ReactiveFormsModule,
        AccordionModule,
        ButtonModule,
        FileUploadModule,
        MessageModule,
        MultiSelectModule,
        SelectModule,
        TagModule,
        TooltipModule,
        DotContentTypeComponent,
        DotFieldRequiredDirective,
        DotMessagePipe
    ],
    templateUrl: './dot-content-drive-dialog-import.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex min-h-0 flex-1 flex-col' }
})
export class DotContentDriveDialogImportComponent {
    readonly #formBuilder = inject(FormBuilder);
    readonly #store = inject(DotContentDriveStore);
    readonly #importService = inject(DotContentImportService);
    readonly #contentTypeService = inject(DotContentTypeService);
    readonly #workflowsService = inject(DotWorkflowsActionsService);
    readonly #fieldService = inject(DotFieldService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #alertConfirmService = inject(DotAlertConfirmService);
    readonly #destroyRef = inject(DestroyRef);
    #previewRequest?: Subscription;
    /**
     * The last successful preview and what it was asked for, so previewing the same file and
     * options again shows it without a second wait. Compared by value: going back to the form makes
     * the shared selector hand its value back, which must not count as a change.
     */
    #lastPreview: { file: File; form: string; job: DotImportJob } | null = null;

    readonly importForm = this.#formBuilder.group({
        contentType: this.#formBuilder.control<string | null>(
            this.#workingContentType(),
            Validators.required
        ),
        language: this.#formBuilder.control<string | null>(
            this.#workingLanguage(),
            Validators.required
        ),
        workflowActionId: this.#formBuilder.control<string | null>(null, Validators.required),
        fields: this.#formBuilder.nonNullable.control<string[]>([]),
        file: this.#formBuilder.control<File | null>(null, Validators.required)
    });

    /**
     * Pages of the content types the user can import into, for the shared content type selector:
     * only those they can write to, never Host, as Content Search's import (FR-004).
     */
    readonly contentTypePageLoader: DotContentTypePageLoader = (options) =>
        this.#contentTypeService.getWritableContentTypes({
            page: options.page,
            per_page: options.per_page,
            filter: options.filter,
            exclude: [HOST_CONTENT_TYPE]
        });
    /**
     * Keeps Key Fields as tall as the other selects whatever it holds. With chips, the theme halves
     * the label's padding and the chip sets the height, so the field shrank once a chip was added
     * and grew back when the last one was removed; this floor is the empty label's own height.
     */
    readonly keyFieldsLabelClass = 'min-h-[calc(2*var(--p-multiselect-padding-y)+1lh)]';
    /**
     * The preview's error and warning sections, both closed when it appears (FR-012): their
     * headers and counts stay side by side, so a long first list can't hide the second.
     */
    readonly openSections: string[] = [];
    /** The picked type's workflow actions that can create content, each with its scheme. */
    readonly $workflowActions = signal<DotImportActionOption[]>([]);
    /** The picked type's fields that can serve as key fields. */
    readonly $keyFields = signal<DotCMSContentTypeField[]>([]);
    /** Whether the user has tried to preview, which is when missing inputs start being reported. */
    readonly $submitted = signal(false);
    /** Where the dialog is. */
    readonly $step = signal<DotImportStep>('form');
    /** Whether a preview is running, during which the file can't be replaced or removed. */
    readonly $previewing = computed(() => this.$step() === 'previewing');
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

    /**
     * Where the rows will land, worded for the preview: the path, how many rows, and why they go
     * there when it isn't the file's own site/folder column. A path with no folder below the site
     * is a site root.
     */
    readonly $destinations = computed(() =>
        (this.$preview()?.result?.destinations ?? []).map((destination) => ({
            path: destination.path,
            isSiteRoot: /^\/\/[^/]+\/?$/.test(destination.path),
            rows:
                destination.rows === 1
                    ? this.#dotMessageService.get('content-drive.import.preview.destination.row')
                    : this.#dotMessageService.get(
                          'content-drive.import.preview.destination.rows',
                          String(destination.rows)
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
     * Whether no row lands in the folder the Drive is showing. Rows are placed by the file, not by
     * where the user opened the dialog, so this says so when the two differ. Only a folder counts:
     * when the Drive shows a whole site there is no single folder to compare with.
     */
    readonly $destinationsSkipViewedFolder = computed(() => {
        const path = this.#store.path();
        const destinations = this.$destinations();
        if (!path || path === ROOT_PATH || path === SYSTEM_HOST_PATH || !destinations.length) {
            return false;
        }

        const folder = `//${this.#store.currentSite()?.hostname}${path.endsWith('/') ? path : `${path}/`}`;

        return !destinations.some(
            (destination) => destination.path.toLowerCase() === folder.toLowerCase()
        );
    });

    /**
     * The note under the Summary figures: the key fields the rows are matched by, when there are
     * any. The server's information lines are left out: they restate the content type ("there are
     * unique fields…", "3 relationship fields…"), not anything about this file, and the rows they
     * would affect are listed as errors already.
     */
    readonly $summaryNotes = computed(() =>
        this.$summary().keyFields
            ? [
                  {
                      text: `${this.#dotMessageService.get('content-drive.import.match-by.summary')}: ${this.$summary().keyFields}`,
                      testId: 'import-summary-key-fields'
                  }
              ]
            : []
    );

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
    /**
     * The server's information lines, minus the two the Summary figures already say: "Import
     * statistics…" and "N headers found…". Neither has a code; the header count is told from the
     * relationship line, which also starts with a number, by having no field.
     */
    readonly $previewInfo = computed(() => {
        const headersMatched = this.$summary().headersMatched;

        return (this.$preview()?.result?.info ?? []).filter(
            ({ message, field }) =>
                !message.startsWith('Import statistics') &&
                !(message.startsWith(`${headersMatched} `) && !field)
        );
    });

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

    /**
     * Whether the import will skip rows. Counted from failed rows, not from the error list: an
     * error about the whole file (a key field missing from it, for one) skips no row.
     */
    readonly $skipsRows = computed(() => this.$summary().failed > 0);

    /**
     * Why the previewed import can't do anything, so the dialog stops it instead of running an
     * import that changes nothing: the preview is a dry run of the import itself, so its counts
     * decide, not the kind of error (a key field missing from the file, for one, is reported as an
     * error yet ignored). `null` when the import can run.
     */
    readonly $importBlockedReason = computed(() => {
        const { toCreate, toUpdate } = this.$summary();

        return toCreate + toUpdate === 0
            ? this.#dotMessageService.get('content-drive.import.preview.blocked.nothing-to-import')
            : null;
    });

    constructor() {
        this.importForm.controls.contentType.valueChanges
            .pipe(
                // A type prefilled from the Drive's filter needs its options too, and an initial
                // value never reaches valueChanges.
                startWith(this.importForm.controls.contentType.value),
                // The shared selector hands back the value it is given, so showing the form again
                // after a preview reports the same type; only a different one is a change.
                distinctUntilChanged(),
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
                    this.#setKeyFields(options.keyFields);
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
        if (this.$previewing()) {
            return;
        }

        const file = event.currentFiles?.[0] ?? null;
        this.importForm.controls.file.setValue(file);
        this.#readFileMeta(file);
    }

    /**
     * Lists the type's key fields with the unique ones first: a unique field matches at most one
     * item, so it is the field to pick for updating existing content.
     *
     * @param fields the type's fields that can be keys, in server order
     */
    #setKeyFields(fields: DotCMSContentTypeField[]): void {
        this.$keyFields.set([
            ...fields.filter((field) => field.unique),
            ...fields.filter((field) => !field.unique)
        ]);
    }

    /** Forgets the picked CSV. */
    onFileClear(): void {
        if (this.$previewing()) {
            return;
        }

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
     * Asks the user to confirm the import. When rows will be skipped, the question says how many,
     * as Content Search does; otherwise it is Content Search's usual question. Rows are counted,
     * not error messages: one row can carry several errors, and an error about the whole file
     * belongs to no row.
     */
    confirmImport(): void {
        const message = this.$skipsRows()
            ? this.#dotMessageService.get(
                  'content-drive.import.preview.errors-will-be-skipped',
                  String(this.$summary().failed)
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
        const form = this.#requestForm();
        const formKey = JSON.stringify(form);
        this.$error.set(null);

        // Nothing changed since the last preview: show it again rather than wait a second time.
        const last = this.#lastPreview;
        if (last && last.file === file && last.form === formKey) {
            this.$preview.set(last.job);
            this.$step.set('preview');

            return;
        }

        this.#previewRequest?.unsubscribe();
        this.$step.set('previewing');
        // Locked, not hidden: the user sees what is being previewed while it runs.
        this.importForm.disable({ emitEvent: false });
        this.#previewRequest = this.#importService
            .preview(file as File, form)
            .pipe(finalize(() => this.importForm.enable({ emitEvent: false })))
            .subscribe({
                next: (job) => {
                    if (job.state !== 'SUCCESS') {
                        this.#fail(job.result?.error?.[0]?.message);

                        return;
                    }

                    this.#lastPreview = { file: file as File, form: formKey, job };
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
        const form = this.#requestForm();

        this.$error.set(null);
        this.#importService
            .importContent(file as File, form)
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
     * Downloads the CSV template of the picked content type, under the name Content Search gives
     * it.
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
     * The content type the user is working in: the one type the Drive is filtered by, or none when
     * the filter holds none or several, since then there is no single type to assume.
     */
    #workingContentType(): string | null {
        const filter = this.#store.getFilterValue('contentType');
        const selected = Array.isArray(filter) ? filter : filter ? [filter] : [];

        return selected.length === 1 ? selected[0] : null;
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

        const defaultLanguageId = this.#store.defaultLanguageId?.();

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
            map(toActionOptions),
            catchError(() => of([] as DotImportActionOption[]))
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
