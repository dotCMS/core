import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { of, Subject, throwError } from 'rxjs';
import { Mocked, describe, expect, it, vi } from 'vitest';

// Saving a file goes through a DOM link; the spec only needs to know it was asked for.
const downloadClick = vi.fn();
vi.mock('@dotcms/utils', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@dotcms/utils')>()),
    getDownloadLink: vi.fn(() => ({ click: downloadClick }))
}));

import { HttpErrorResponse } from '@angular/common/http';

import { Accordion, AccordionHeader } from 'primeng/accordion';
import { FileUpload } from 'primeng/fileupload';

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
    DotCMSContentType,
    DotCMSContentTypeField,
    DotImportJob,
    DotPagination
} from '@dotcms/dotcms-models';
import { DotContentTypeComponent } from '@dotcms/ui';
import { getDownloadLink } from '@dotcms/utils';
import {
    createFakeLanguage,
    createFakeSite,
    createFakeTextField,
    MockDotMessageService
} from '@dotcms/utils-testing';

import { DotContentDriveDialogImportComponent } from './dot-content-drive-dialog-import.component';

import { DotContentDriveStore } from '../../../store/dot-content-drive.store';

const ENGLISH = createFakeLanguage({ id: 1, language: 'English', countryCode: 'US' });
const SPANISH = createFakeLanguage({ id: 2, language: 'Spanish', countryCode: 'ES' });

const CONTENT_TYPES = [
    { id: 'ct-blog', variable: 'Blog', name: 'Blog', baseType: 'CONTENT' },
    { id: 'ct-doc', variable: 'Document', name: 'Document', baseType: 'FILEASSET' },
    { id: 'ct-news', variable: 'News', name: 'News', baseType: 'CONTENT' }
] as DotCMSContentType[];

const CONTENT_TYPES_PAGE: { contentTypes: DotCMSContentType[]; pagination: DotPagination } = {
    contentTypes: CONTENT_TYPES,
    pagination: { currentPage: 1, perPage: 40, totalEntries: CONTENT_TYPES.length }
};

const workflowAction = (id: string, name: string, scheme: string) =>
    ({
        action: { id, name },
        scheme: { id: `scheme-${scheme}`, name: scheme },
        firstStep: {}
    }) as unknown as DotCMSContentletWorkflowActions;

const field = (variable: string, clazz: string, indexed = true): DotCMSContentTypeField =>
    createFakeTextField({ variable, name: variable, clazz, indexed } as never);

/** Every field kind the import can't use as a key, next to two it can. */
const BLOG_FIELDS: DotCMSContentTypeField[] = [
    field('title', DotCMSClazzes.TEXT),
    field('urlTitle', DotCMSClazzes.TEXT),
    field('related', DotCMSClazzes.RELATIONSHIP),
    field('relTab', DotCMSClazzes.RELATIONSHIPS_TAB),
    field('permTab', DotCMSClazzes.PERMISSION_TAB),
    field('attachment', DotCMSClazzes.FILE),
    field('hero', DotCMSClazzes.IMAGE),
    field('divider', DotCMSClazzes.LINE_DIVIDER),
    field('tab', DotCMSClazzes.TAB_DIVIDER),
    field('row', DotCMSClazzes.ROW),
    field('column', DotCMSClazzes.COLUMN)
];

describe('DotContentDriveDialogImportComponent', () => {
    let spectator: Spectator<DotContentDriveDialogImportComponent>;
    let importService: Mocked<DotContentImportService>;
    let workflowsService: Mocked<DotWorkflowsActionsService>;
    let fieldService: Mocked<DotFieldService>;

    const storeMock = (languageFilter: string[] | undefined) => ({
        currentSite: vi.fn().mockReturnValue(createFakeSite({ hostname: 'demo.dotcms.com' })),
        languages: vi.fn().mockReturnValue([ENGLISH, SPANISH]),
        defaultLanguageId: vi.fn().mockReturnValue(ENGLISH.id),
        getFilterValue: vi.fn((key: string) => (key === 'languageId' ? languageFilter : undefined)),
        closeDialog: vi.fn(),
        startImportRun: vi.fn()
    });

    const createComponent = createComponentFactory({
        component: DotContentDriveDialogImportComponent,
        providers: [
            mockProvider(DotContentImportService, {
                getWritableContentTypes: vi.fn().mockReturnValue(of(CONTENT_TYPES_PAGE)),
                preview: vi.fn(),
                importContent: vi.fn(),
                importPreviewed: vi.fn(),
                downloadSample: vi.fn()
            }),
            mockProvider(DotAlertConfirmService, { confirm: vi.fn() }),
            // The shared selector reads the picked type back by variable to pin it in the list.
            mockProvider(DotContentTypeService, {
                getContentType: vi.fn((variable: string) =>
                    of(CONTENT_TYPES.find((type) => type.variable === variable))
                ),
                getContentTypesWithPagination: vi.fn()
            }),
            mockProvider(DotWorkflowsActionsService, {
                getDefaultActions: vi.fn((contentType: string) =>
                    of(
                        contentType === 'Blog'
                            ? [workflowAction('publish', 'Publish', 'Blogs')]
                            : [workflowAction('save', 'Save', 'System Workflow')]
                    )
                )
            }),
            mockProvider(DotFieldService, {
                getFields: vi.fn().mockReturnValue(of(BLOG_FIELDS))
            }),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'content-drive.import.language.multilingual': 'Multilingual file',
                    'content-drive.import.preview.destination.rows': '{0} rows → {1}',
                    'content-drive.import.preview.line': 'Line {0}',
                    'content-drive.import.preview.errors-will-be-skipped':
                        'content-drive.import.preview.errors-will-be-skipped ({0})'
                })
            }
        ]
    });

    /** `'none'` means the Drive has no language filter at all. */
    const setup = (languageFilter: string[] | 'none' = ['2']) => {
        spectator = createComponent({
            providers: [
                mockProvider(
                    DotContentDriveStore,
                    storeMock(languageFilter === 'none' ? undefined : languageFilter)
                )
            ]
        });
        importService = spectator.inject(DotContentImportService);
        workflowsService = spectator.inject(DotWorkflowsActionsService);
        fieldService = spectator.inject(DotFieldService);
        spectator.detectChanges();
    };

    const form = () => spectator.component.importForm.controls;

    const pickContentType = (variable: string) => {
        form().contentType.setValue(variable);
        spectator.detectChanges();
    };

    const csv = () => new File(['title\nHello'], 'blogs.csv', { type: 'text/csv' });

    /**
     * #37896 FR-004: Content Type is the shared selector from the UI library (paged, lazy loaded,
     * searched on the server), fed by the import's own list: only the types the user can write
     * to, never Host. The general content type listing is never asked.
     */
    describe('content type (FR-004)', () => {
        const selector = () => spectator.query(DotContentTypeComponent);

        it('uses the shared content type selector', () => {
            setup();

            expect(selector()).toBeTruthy();
            expect(spectator.query(byTestId('import-content-type'))).toBeTruthy();
        });

        it('feeds the selector pages of the types the user can write to', () => {
            setup();
            importService.getWritableContentTypes.mockClear();

            let page: unknown;
            selector()
                ?.pageLoader()?.({ page: 2, per_page: 40, filter: 'blo' })
                .subscribe((value) => (page = value));

            expect(importService.getWritableContentTypes).toHaveBeenCalledWith({
                page: 2,
                per_page: 40,
                filter: 'blo'
            });
            expect(page).toEqual(CONTENT_TYPES_PAGE);
        });

        it('never asks the general content type listing', () => {
            setup();

            expect(
                spectator.inject(DotContentTypeService).getContentTypesWithPagination
            ).not.toHaveBeenCalled();
            expect(importService.getWritableContentTypes).toHaveBeenCalled();
        });

        /** Spec Edge Cases: a user who can write to no content type is told so. */
        it('says there is nothing to import into when the list is empty', () => {
            setup();

            expect(selector()?.emptyMessage()).toBe('content-drive.import.content-type.empty');
        });

        it('puts the picked type in the form', () => {
            setup();

            selector()?.onContentTypeChange(CONTENT_TYPES[0]);
            // The selector reports its value to the form from an effect.
            spectator.detectChanges();

            expect(form().contentType.value).toBe('Blog');
        });
    });

    describe('workflow action (FR-006)', () => {
        it('loads the initial actions of the picked type, labelled with their scheme', () => {
            setup();
            pickContentType('Blog');

            expect(workflowsService.getDefaultActions).toHaveBeenCalledWith('Blog');
            expect(spectator.component.$workflowActions()).toEqual([
                { label: 'Publish ( Blogs )', value: 'publish' }
            ]);
        });
    });

    describe('key fields (FR-007)', () => {
        it('asks only for indexed fields and drops the kinds that cannot be keys', () => {
            setup();
            pickContentType('Blog');

            expect(fieldService.getFields).toHaveBeenCalledWith('Blog', 'SYSTEM_INDEXED');
            expect(spectator.component.$keyFields().map((f) => f.variable)).toEqual([
                'title',
                'urlTitle'
            ]);
        });

        it('never lists a field the server reports as not indexed', () => {
            setup();
            fieldService.getFields.mockReturnValue(
                of([field('title', DotCMSClazzes.TEXT), field('body', DotCMSClazzes.TEXT, false)])
            );
            pickContentType('Blog');

            expect(spectator.component.$keyFields().map((f) => f.variable)).toEqual(['title']);
        });
    });

    describe('changing the content type (FR-008)', () => {
        it('clears the workflow action and key fields picked for the previous type', () => {
            setup();
            pickContentType('Blog');
            form().workflowActionId.setValue('publish');
            form().fields.setValue(['title']);

            pickContentType('News');

            expect(form().workflowActionId.value).toBeNull();
            expect(form().fields.value).toEqual([]);
            expect(spectator.component.$workflowActions()).toEqual([
                { label: 'Save ( System Workflow )', value: 'save' }
            ]);
        });
    });

    describe('language (FR-005)', () => {
        it('offers every language plus a multilingual option', () => {
            setup();

            expect(spectator.component.$languageOptions().map((o) => o.value)).toEqual([
                '1',
                '2',
                '-1'
            ]);
            expect(spectator.component.$languageOptions().at(-1)?.label).toBe('Multilingual file');
        });

        it('is prefilled with the one language the Drive is filtered by', () => {
            setup(['2']);

            expect(form().language.value).toBe('2');
        });

        it('falls back to the default language when the Drive is filtered by several', () => {
            setup(['1', '2']);

            expect(form().language.value).toBe('1');
        });

        it('falls back to the default language when the Drive has no language filter', () => {
            setup('none');

            expect(form().language.value).toBe('1');
        });

        it('explains the extra columns and the key field when the file is multilingual', () => {
            setup();
            expect(spectator.query(byTestId('import-multilingual-note'))).toBeFalsy();

            form().language.setValue('-1');
            spectator.detectChanges();

            expect(spectator.query(byTestId('import-multilingual-note'))).toBeTruthy();
        });
    });

    describe('file (FR-010)', () => {
        it('only lets the user choose .csv files', () => {
            setup();

            const input = spectator
                .query(byTestId('import-file-input'))
                ?.querySelector('input[type="file"]');
            expect(input?.getAttribute('accept')).toContain('.csv');
        });

        it('keeps the CSV the user chose', () => {
            setup();
            const file = csv();

            spectator.triggerEventHandler(FileUpload, 'onSelect', {
                originalEvent: new Event('change'),
                files: [file],
                currentFiles: [file]
            });

            expect(form().file.value).toBe(file);
        });
    });

    describe('placement notice (FR-011)', () => {
        it('tells the user where rows land', () => {
            setup();

            expect(spectator.query(byTestId('import-placement-notice'))).toBeTruthy();
        });
    });

    describe('Preview with missing inputs (FR-009)', () => {
        const clickPreview = () => {
            const button = spectator.query(byTestId('import-preview-btn'));
            spectator.click(button?.querySelector('button') ?? (button as HTMLElement));
            spectator.detectChanges();
        };

        it('is never disabled', () => {
            setup();

            const button = spectator.query(byTestId('import-preview-btn'));
            const native = button?.querySelector('button') ?? button;
            expect(native?.hasAttribute('disabled')).toBe(false);
        });

        it('starts nothing and shows a message on each missing input', () => {
            setup('none');
            form().language.setValue(null);

            clickPreview();

            expect(importService.preview).not.toHaveBeenCalled();
            expect(spectator.query(byTestId('import-error-contentType'))).toBeTruthy();
            expect(spectator.query(byTestId('import-error-language'))).toBeTruthy();
            expect(spectator.query(byTestId('import-error-workflowActionId'))).toBeTruthy();
            expect(spectator.query(byTestId('import-error-file'))).toBeTruthy();
        });

        it('asks for a key field when the file is multilingual', () => {
            setup();
            pickContentType('Blog');
            form().workflowActionId.setValue('publish');
            form().file.setValue(csv());
            form().language.setValue('-1');

            clickPreview();

            expect(importService.preview).not.toHaveBeenCalled();
            expect(spectator.query(byTestId('import-error-fields'))).toBeTruthy();
        });

        it('shows no message before the user has tried to preview', () => {
            setup('none');

            expect(spectator.query(byTestId('import-error-contentType'))).toBeFalsy();
            expect(spectator.query(byTestId('import-error-workflowActionId'))).toBeFalsy();
        });
    });

    /**
     * #37896 US3: Preview runs the server's dry run and shows what will happen in the Drive's own
     * components — never the legacy plain-text lists.
     */
    describe('preview', () => {
        let previewJob: Subject<DotImportJob>;

        const PREVIEW: DotImportJob = {
            id: 'job-1',
            state: 'SUCCESS',
            result: {
                type: 'PREVIEW',
                keyFields: ['title'],
                file: { totalRows: 5, headers: { valid: ['title', 'date', 'location'] } },
                data: {
                    processed: { parsedRows: 5, failedRows: 1 },
                    summary: { created: 3, updated: 1, failed: 1 }
                },
                destinations: [
                    { path: '//demo.dotcms.com/news/', rows: 2, source: 'CSV' },
                    { path: '//demo.dotcms.com/blog/', rows: 1, source: 'CONTENT_TYPE_DEFAULT' },
                    { path: '//demo.dotcms.com/old/', rows: 1, source: 'EXISTING' }
                ],
                warning: [{ code: 'UNKNOWN', message: 'Unknown column color', field: 'color' }],
                error: [
                    { code: 'DATE', message: 'Bad date', row: 7, field: 'date', value: '13/40' },
                    { code: 'GENERIC', message: 'Something about the file' },
                    { code: 'DATE', message: 'Bad date', row: 3, field: 'date', value: 'x' }
                ]
            }
        };

        const fillForm = () => {
            pickContentType('Blog');
            form().workflowActionId.setValue('publish');
            form().fields.setValue(['title']);
            form().file.setValue(csv());
            spectator.detectChanges();
        };

        const clickTestId = (testId: string) => {
            const element = spectator.query(byTestId(testId));
            spectator.click(element?.querySelector('button') ?? (element as HTMLElement));
            spectator.detectChanges();
        };

        const runPreview = (job: DotImportJob = PREVIEW) => {
            clickTestId('import-preview-btn');
            previewJob.next(job);
            previewJob.complete();
            spectator.detectChanges();
        };

        const text = (testId: string) => spectator.query(byTestId(testId))?.textContent ?? '';

        beforeEach(() => {
            setup(['1']);
            previewJob = new Subject<DotImportJob>();
            importService.preview.mockReturnValue(previewJob.asObservable());
            // The factory's confirm mock is shared by every test; start each one clean.
            vi.mocked(spectator.inject(DotAlertConfirmService).confirm).mockClear();
            fillForm();
        });

        it('sends the file and the form, with the Drive site', () => {
            clickTestId('import-preview-btn');

            expect(importService.preview).toHaveBeenCalledWith(form().file.value, {
                contentType: 'Blog',
                language: '1',
                workflowActionId: 'publish',
                fields: ['title'],
                site: spectator.inject(DotContentDriveStore).currentSite().identifier
            });
        });

        /**
         * No separate loading screen: the form stays in place, locked, and Go to Preview shows its
         * own loading state, as the folder dialog's submit does.
         */
        it('keeps the form visible and locked while the preview runs', () => {
            clickTestId('import-preview-btn');

            const previewButton = spectator
                .query(byTestId('import-preview-btn'))
                ?.querySelector('button');
            expect(previewButton?.classList.contains('p-button-loading')).toBe(true);
            expect(form().contentType.disabled).toBe(true);
            expect(spectator.query(byTestId('import-content-type'))).toBeTruthy();
            expect(spectator.query(byTestId('import-preview'))).toBeFalsy();
        });

        it('unlocks the form once the preview arrives', () => {
            runPreview();

            expect(form().contentType.disabled).toBe(false);
        });

        it('shows the summary counts', () => {
            runPreview();

            expect(text('import-summary-rows-read')).toContain('4');
            expect(text('import-summary-headers')).toContain('3');
            expect(text('import-summary-key-fields')).toContain('title');
            expect(text('import-summary-will-import')).toContain('4');
            expect(text('import-summary-failed')).toContain('1');
        });

        it('shows where the rows will land, naming defaults and current locations', () => {
            runPreview();

            const rows = spectator.queryAll(byTestId('import-destination'));
            expect(rows.length).toBe(3);
            expect(rows[0].textContent).toContain('//demo.dotcms.com/news/');
            expect(rows[0].textContent).toContain('2');
            expect(rows[1].textContent).toContain(
                'content-drive.import.preview.destination.source.content-type-default'
            );
            expect(rows[2].textContent).toContain(
                'content-drive.import.preview.destination.source.existing'
            );
        });

        it('lists warnings and errors apart, errors without a line first, then by line', () => {
            runPreview();

            expect(text('import-warnings-count')).toContain('1');
            expect(text('import-errors-count')).toContain('3');
            // Warnings come first, Errors after.
            const preview = spectator.query(byTestId('import-preview')) as HTMLElement;
            expect(preview.innerHTML.indexOf('import-warnings-group')).toBeLessThan(
                preview.innerHTML.indexOf('import-errors-group')
            );
            const errors = spectator
                .queryAll(byTestId('import-error-item'))
                .map((item) => item.textContent ?? '');
            expect(errors[0]).toContain('Something about the file');
            expect(errors[1]).toContain('3');
            expect(errors[1]).toContain('date');
            expect(errors[1]).toContain('x');
            expect(errors[2]).toContain('7');
            expect(errors[2]).toContain('13/40');
        });

        /** #37896 FR-012 (UI review): collapsible sections, both open when the preview appears. */
        it('shows warnings and errors as sections that both start open', () => {
            runPreview();

            const accordion = spectator.query(Accordion);
            expect(accordion?.multiple()).toBe(true);
            expect(accordion?.value()).toEqual(['warnings', 'errors']);
        });

        it('collapses and expands each section on its own', () => {
            runPreview();
            const [warningsHeader, errorsHeader] = spectator.queryAll(AccordionHeader);

            warningsHeader.onClick();
            spectator.detectChanges();
            expect(spectator.query(Accordion)?.value()).toEqual(['errors']);

            errorsHeader.onClick();
            spectator.detectChanges();
            expect(spectator.query(Accordion)?.value()).toEqual([]);

            warningsHeader.onClick();
            spectator.detectChanges();
            expect(spectator.query(Accordion)?.value()).toEqual(['warnings']);
        });

        it('hides a section that has no entries', () => {
            runPreview({ ...PREVIEW, result: { ...PREVIEW.result, warning: [] } });

            expect(spectator.query(byTestId('import-warnings-group'))).toBeFalsy();
            expect(spectator.query(byTestId('import-errors-group'))).toBeTruthy();
        });

        it('shows no warnings and errors box when there are neither', () => {
            runPreview({
                ...PREVIEW,
                result: { ...PREVIEW.result, warning: [], error: [] }
            });

            expect(spectator.query(byTestId('import-messages'))).toBeFalsy();
            expect(spectator.query(byTestId('import-summary'))).toBeTruthy();
        });

        it('discards the preview when an input changes', () => {
            runPreview();
            expect(spectator.query(byTestId('import-preview'))).toBeTruthy();

            form().fields.setValue([]);
            spectator.detectChanges();

            expect(spectator.query(byTestId('import-preview'))).toBeFalsy();
            expect(spectator.query(byTestId('import-preview-btn'))).toBeTruthy();
        });

        it('goes back to the form with the inputs kept', () => {
            runPreview();

            clickTestId('import-preview-back');

            expect(spectator.query(byTestId('import-preview'))).toBeFalsy();
            expect(form().contentType.value).toBe('Blog');
            expect(form().workflowActionId.value).toBe('publish');
            expect(form().file.value).toBeTruthy();
        });

        it('warns that rows with errors will be skipped before importing', () => {
            runPreview();

            clickTestId('import-confirm-btn');

            const confirm = spectator.inject(DotAlertConfirmService).confirm as unknown as Mocked<
                (options: { message: string }) => void
            >;
            expect(confirm).toHaveBeenCalledTimes(1);
            expect(confirm.mock.calls[0][0].message).toContain(
                'content-drive.import.preview.errors-will-be-skipped'
            );
        });

        it('asks the usual confirmation when the preview has no errors', () => {
            runPreview({ ...PREVIEW, result: { ...PREVIEW.result, error: [] } });

            clickTestId('import-confirm-btn');

            const confirm = spectator.inject(DotAlertConfirmService).confirm as unknown as Mocked<
                (options: { message: string }) => void
            >;
            expect(confirm.mock.calls[0][0].message).toContain(
                'message.contentlet.proceed.importing'
            );
        });
    });

    /**
     * #37896 US1: once the user confirms, the import starts with exactly the previewed file and
     * options, plus the Drive's site; the dialog then closes and the status toast takes over.
     */
    describe('import', () => {
        const PREVIEWED: DotImportJob = {
            id: 'job-1',
            state: 'SUCCESS',
            result: { error: [], warning: [], data: { summary: { created: 1, updated: 0 } } }
        };

        const clickTestId = (testId: string) => {
            const element = spectator.query(byTestId(testId));
            spectator.click(element?.querySelector('button') ?? (element as HTMLElement));
            spectator.detectChanges();
        };

        beforeEach(() => {
            setup(['1']);
            importService.preview.mockReturnValue(of(PREVIEWED));
            importService.importContent.mockReturnValue(of({ jobId: 'import-1' }));
            vi.mocked(spectator.inject(DotAlertConfirmService).confirm).mockImplementation(
                (options) => {
                    options.accept?.();

                    return undefined;
                }
            );
            pickContentType('Blog');
            form().workflowActionId.setValue('publish');
            form().file.setValue(csv());
            spectator.detectChanges();
            clickTestId('import-preview-btn');
        });

        it('starts the import with the previewed file, options and the Drive site', () => {
            clickTestId('import-confirm-btn');

            expect(importService.importContent).toHaveBeenCalledWith(form().file.value, {
                contentType: 'Blog',
                language: '1',
                workflowActionId: 'publish',
                fields: [],
                site: spectator.inject(DotContentDriveStore).currentSite().identifier
            });
        });

        /** PR #38054 review: the CSV goes up once, and the import reads what was previewed. */
        it('imports the previewed file by its temp file id, without uploading it again', () => {
            // The service mocks are shared by every test; start this one clean.
            importService.importContent.mockClear();
            importService.importPreviewed.mockReturnValue(of({ jobId: 'import-2' }));
            importService.preview.mockReturnValue(
                of({ ...PREVIEWED, parameters: { cmd: 'preview', tempFileId: 'temp_123' } })
            );
            // A new preview for the new answer.
            form().fields.setValue(['title']);
            spectator.detectChanges();
            clickTestId('import-preview-btn');

            clickTestId('import-confirm-btn');

            expect(importService.importPreviewed).toHaveBeenCalledWith('temp_123', {
                contentType: 'Blog',
                language: '1',
                workflowActionId: 'publish',
                fields: ['title'],
                site: spectator.inject(DotContentDriveStore).currentSite().identifier
            });
            expect(importService.importContent).not.toHaveBeenCalled();
        });

        it('hands the run to the status toast and closes the dialog', () => {
            clickTestId('import-confirm-btn');

            const store = spectator.inject(DotContentDriveStore);
            expect(store.startImportRun).toHaveBeenCalledWith('import-1', 'blogs.csv');
            expect(store.closeDialog).toHaveBeenCalled();
        });
    });

    /**
     * #37896 US4 (FR-025): the sample needs a content type to know which columns to list, so its
     * download is only offered once one is picked.
     */
    describe('sample CSV', () => {
        const sampleButton = () => {
            const host = spectator.query(byTestId('import-sample-link'));

            return (host?.querySelector('button') ?? host) as HTMLButtonElement | null;
        };

        it('is not offered until a content type is picked', () => {
            setup();

            expect(sampleButton()).toBeFalsy();

            pickContentType('Blog');

            expect(sampleButton()).toBeTruthy();
            expect(sampleButton()?.disabled).toBe(false);
        });

        it("downloads the picked type's sample as CSV_Template.csv", () => {
            setup();
            const blob = new Blob(['title'], { type: 'text/csv' });
            importService.downloadSample.mockReturnValue(of(blob));
            pickContentType('Blog');

            spectator.click(sampleButton() as HTMLButtonElement);

            expect(importService.downloadSample).toHaveBeenCalledWith('Blog');
            expect(getDownloadLink).toHaveBeenCalledWith(blob, 'CSV_Template.csv');
            expect(downloadClick).toHaveBeenCalled();
        });
    });

    /**
     * #37896 US5: a preview or an import that can't run says why in the dialog, keeps everything
     * the user filled in, and lets them fix the form or the file and try again. There is no retry
     * on the server: trying again means submitting again.
     */
    describe('failures', () => {
        const clickTestId = (testId: string) => {
            const element = spectator.query(byTestId(testId));
            spectator.click(element?.querySelector('button') ?? (element as HTMLElement));
            spectator.detectChanges();
        };

        const fill = () => {
            pickContentType('Blog');
            form().workflowActionId.setValue('publish');
            form().file.setValue(csv());
            spectator.detectChanges();
        };

        /** The 400 the import endpoints answer with: a failed job whose result names the error. */
        const refused = (message: string) =>
            new HttpErrorResponse({
                status: 400,
                error: { entity: { state: 'FAILED', result: { error: [{ message }] } } }
            });

        beforeEach(() => setup(['1']));

        it("shows the server's reason when the preview is refused, and keeps the form", () => {
            importService.preview.mockReturnValue(
                throwError(() => refused('The file has no data rows'))
            );
            fill();

            clickTestId('import-preview-btn');

            expect(spectator.query(byTestId('import-error'))?.textContent).toContain(
                'The file has no data rows'
            );
            expect(spectator.query(byTestId('import-preview'))).toBeFalsy();
            expect(form().contentType.value).toBe('Blog');
            expect(form().file.value).toBeTruthy();
        });

        it("shows the job's error when the preview job itself failed", () => {
            importService.preview.mockReturnValue(
                of({
                    id: 'job-1',
                    state: 'FAILED_PERMANENTLY',
                    result: { error: [{ message: 'Language not found' }] }
                } as DotImportJob)
            );
            fill();

            clickTestId('import-preview-btn');

            expect(spectator.query(byTestId('import-error'))?.textContent).toContain(
                'Language not found'
            );
            expect(spectator.query(byTestId('import-preview'))).toBeFalsy();
        });

        it('stays open with the reason when the import cannot be started', () => {
            importService.preview.mockReturnValue(
                of({ id: 'job-1', state: 'SUCCESS', result: { error: [] } } as DotImportJob)
            );
            importService.importContent.mockReturnValue(
                throwError(() => refused('Workflow action not found'))
            );
            vi.mocked(spectator.inject(DotAlertConfirmService).confirm).mockImplementation(
                (options) => {
                    options.accept?.();

                    return undefined;
                }
            );
            fill();
            clickTestId('import-preview-btn');

            clickTestId('import-confirm-btn');

            const store = spectator.inject(DotContentDriveStore);
            expect(spectator.query(byTestId('import-error'))?.textContent).toContain(
                'Workflow action not found'
            );
            expect(store.closeDialog).not.toHaveBeenCalled();
            expect(store.startImportRun).not.toHaveBeenCalled();
        });

        it('lets the user try again after changing the file', () => {
            importService.preview.mockReturnValueOnce(
                throwError(() => refused('The file has no data rows'))
            );
            importService.preview.mockReturnValueOnce(
                of({ id: 'job-2', state: 'SUCCESS', result: { error: [] } } as DotImportJob)
            );
            fill();
            clickTestId('import-preview-btn');

            form().file.setValue(new File(['title\nHello'], 'fixed.csv', { type: 'text/csv' }));
            spectator.detectChanges();
            clickTestId('import-preview-btn');

            expect(spectator.query(byTestId('import-error'))).toBeFalsy();
            expect(spectator.query(byTestId('import-preview'))).toBeTruthy();
        });
    });
});
