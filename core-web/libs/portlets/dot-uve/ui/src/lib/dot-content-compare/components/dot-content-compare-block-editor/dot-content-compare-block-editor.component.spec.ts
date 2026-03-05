import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

/**
 * Runs change detection in the next macrotask to avoid NG0100 (ExpressionChangedAfterItHasBeenCheckedError).
 * The component assigns observables in ngAfterViewInit and the mock emits asynchronously, so a synchronous
 * detectChanges() can see bindings change during the same cycle. Deferring to the next tick stabilizes the test.
 */
async function detectChangesNextTick(detectChanges: () => void): Promise<void> {
    await new Promise((resolve) => setTimeout(resolve, 0));
    detectChanges();
}

import { DotMessageService } from '@dotcms/data-access';
import { DotDiffPipe, DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { BlockEditorMockComponent } from './block-editor-mock/block-editor-mock.component';
import { DotContentCompareBlockEditorComponent } from './dot-content-compare-block-editor.component';

import { DotContentCompareTableData } from '../../store/dot-content-compare.store';

export const dotContentCompareTableDataMock: DotContentCompareTableData = {
    working: {
        archived: false,
        baseType: 'CONTENT',
        contentType: 'NewContentMd',
        folder: 'SYSTEM_FOLDER',
        hasLiveVersion: true,
        hasTitleImage: false,
        host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
        hostName: 'demo.dotcms.com',
        html: {
            attrs: {
                charCount: 16,
                readingTime: 1,
                wordCount: 5
            },
            content: [
                {
                    attrs: {
                        level: 2,
                        textAlign: 'left'
                    },
                    content: [
                        {
                            text: 'This is a blog',
                            type: 'text'
                        }
                    ],
                    type: 'heading'
                },
                {
                    attrs: {
                        textAlign: 'left'
                    },
                    type: 'paragraph'
                },
                {
                    attrs: {
                        textAlign: 'left'
                    },
                    type: 'paragraph'
                }
            ],
            type: 'doc'
        },
        identifier: '32c7545acfdff13b6f5d71ca16bc411d',
        inode: '7fd98306-10da-4b32-ae56-fc509892d832',
        languageId: 1,
        live: true,
        locked: false,
        modDate: '04/06/2023 - 12:40 PM',
        modUser: 'dotcms.org.1',
        modUserName: 'Admin User',
        owner: 'dotcms.org.1',
        publishDate: 1680784819148,
        sortOrder: 0,
        stInode: '564a6bdcd7e5750baacd00dc6fa54824',
        title: '32c7545acfdff13b6f5d71ca16bc411d',
        titleImage: 'TITLE_IMAGE_NOT_FOUND',
        url: '/content.32865e81-9e30-42bd-ab5b-5c98161acffc',
        variantId: 'DEFAULT',
        working: true
    },
    compare: {
        archived: false,
        baseType: 'CONTENT',
        contentType: 'NewContentMd',
        folder: 'SYSTEM_FOLDER',
        hasLiveVersion: true,
        hasTitleImage: false,
        host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
        hostName: 'demo.dotcms.com',
        html: {
            attrs: {
                charCount: 16,
                readingTime: 1,
                wordCount: 5
            },
            content: [
                {
                    attrs: {
                        level: 2,
                        textAlign: 'left'
                    },
                    content: [
                        {
                            text: 'This is a blog to compare',
                            type: 'text'
                        }
                    ],
                    type: 'heading'
                },
                {
                    attrs: {
                        textAlign: 'left'
                    },
                    type: 'paragraph'
                },
                {
                    attrs: {
                        textAlign: 'left'
                    },
                    type: 'paragraph'
                }
            ],
            type: 'doc'
        },
        identifier: '32c7545acfdff13b6f5d71ca16bc411d',
        inode: '32865e81-9e30-42bd-ab5b-5c98161acffc',
        languageId: 1,
        live: false,
        locked: false,
        modDate: '04/05/2023 - 01:08 AM',
        modUser: 'dotcms.org.1',
        modUserName: 'Admin User',
        owner: 'dotcms.org.1',
        publishDate: 1680656938847,
        sortOrder: 0,
        stInode: '564a6bdcd7e5750baacd00dc6fa54824',
        title: '32c7545acfdff13b6f5d71ca16bc411d',
        titleImage: 'TITLE_IMAGE_NOT_FOUND',
        url: '/content.32865e81-9e30-42bd-ab5b-5c98161acffc',
        variantId: 'DEFAULT',
        working: false
    },
    versions: [
        {
            archived: false,
            baseType: 'CONTENT',
            contentType: 'NewContentMd',
            folder: 'SYSTEM_FOLDER',
            hasLiveVersion: true,
            hasTitleImage: false,
            host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
            hostName: 'demo.dotcms.com',
            html: '<h1>hello</h1>',
            identifier: '32c7545acfdff13b6f5d71ca16bc411d',
            inode: '0b4a192b-2455-4ef9-95f5-f58ed1a061ae',
            languageId: 1,
            live: false,
            locked: false,
            modDate: '04/05/2023 - 01:34 PM',
            modUser: 'dotcms.org.1',
            modUserName: 'Admin User',
            owner: 'dotcms.org.1',
            publishDate: 1680701662211,
            sortOrder: 0,
            stInode: '564a6bdcd7e5750baacd00dc6fa54824',
            title: '32c7545acfdff13b6f5d71ca16bc411d',
            titleImage: 'TITLE_IMAGE_NOT_FOUND',
            url: '/content.32865e81-9e30-42bd-ab5b-5c98161acffc',
            variantId: 'DEFAULT',
            working: false
        },
        {
            archived: false,
            baseType: 'CONTENT',
            contentType: 'NewContentMd',
            folder: 'SYSTEM_FOLDER',
            hasLiveVersion: true,
            hasTitleImage: false,
            host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
            hostName: 'demo.dotcms.com',
            html: '',
            identifier: '32c7545acfdff13b6f5d71ca16bc411d',
            inode: 'ba21b4d3-0d83-41e8-82fb-e5dcd7fe6226',
            languageId: 1,
            live: false,
            locked: false,
            modDate: '04/05/2023 - 01:23 AM',
            modUser: 'dotcms.org.1',
            modUserName: 'Admin User',
            owner: 'dotcms.org.1',
            publishDate: 1680657820158,
            sortOrder: 0,
            stInode: '564a6bdcd7e5750baacd00dc6fa54824',
            title: '32c7545acfdff13b6f5d71ca16bc411d',
            titleImage: 'TITLE_IMAGE_NOT_FOUND',
            url: '/content.32865e81-9e30-42bd-ab5b-5c98161acffc',
            variantId: 'DEFAULT',
            working: false
        }
    ],
    fields: [
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableStoryBlockField',
            contentTypeId: '564a6bdcd7e5750baacd00dc6fa54824',
            dataType: 'LONG_TEXT',
            fieldType: 'Story-Block',
            fieldTypeLabel: 'Block Editor',
            fieldVariables: [],
            fixed: false,
            iDate: 1680701631000,
            id: '32768f8b238442faa224c7f1e2982234',
            indexed: false,
            listed: false,
            modDate: 1680701682000,
            name: 'html',
            readOnly: false,
            required: false,
            searchable: false,
            sortOrder: 2,
            unique: false,
            variable: 'html'
        }
    ]
};

// Mock ClipboardEvent and DragEvent to avoid tiptap implementation errors.
class ClipboardDataMock {
    getData: jest.Mock<string, [string]>;
    setData: jest.Mock<void, [string, string]>;

    constructor() {
        this.getData = jest.fn();
        this.setData = jest.fn();
    }
}

class ClipboardEventMock extends Event {
    clipboardData: ClipboardDataMock;

    constructor(type: string, options?: EventInit) {
        super(type, options);
        this.clipboardData = new ClipboardDataMock();
    }
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
(global as any).ClipboardEvent = ClipboardEventMock;

class DataTransferMock {
    data: { [key: string]: string };

    constructor() {
        this.data = {};
    }

    setData(format: string, data: string): void {
        this.data[format] = data;
    }

    getData(format: string): string {
        return this.data[format] || '';
    }
}

class DragEventMock extends Event {
    dataTransfer: DataTransferMock;

    constructor(type: string, options?: EventInit) {
        super(type, options);
        this.dataTransfer = new DataTransferMock();
    }
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
(global as any).DragEvent = DragEventMock;

describe('DotContentCompareBlockEditorComponent', () => {
    let spectator: Spectator<DotContentCompareBlockEditorComponent>;

    const messageServiceMock = new MockDotMessageService({
        diff: 'Diff',
        plain: 'Plain'
    });

    const createComponent = createComponentFactory({
        component: DotContentCompareBlockEditorComponent,
        imports: [
            DotDiffPipe,
            HttpClientTestingModule,
            CommonModule,
            BlockEditorMockComponent,
            DotSafeHtmlPipe,
            DotMessagePipe
        ],
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
        providers: [{ provide: DotMessageService, useValue: messageServiceMock }],
        overrideComponents: [
            [
                DotContentCompareBlockEditorComponent,
                {
                    set: {
                        imports: [
                            CommonModule,
                            BlockEditorMockComponent,
                            DotSafeHtmlPipe,
                            DotDiffPipe
                        ]
                    }
                }
            ]
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                data: dotContentCompareTableDataMock,
                field: 'html',
                showDiff: false,
                showAsCompare: false
            }
        });
        spectator.detectChanges();
    });

    describe('Checking if we are passing HTML to the working field', () => {
        it('Should contain same HTML for working than the Block Editor', async () => {
            await spectator.fixture.whenStable();
            spectator.detectChanges();

            await new Promise((resolve) => setTimeout(resolve, 200));
            await detectChangesNextTick(() => spectator.detectChanges());

            const blockEditor = spectator.component
                .blockEditor as unknown as BlockEditorMockComponent;
            expect(blockEditor).toBeDefined();
            expect(blockEditor.editor).toBeDefined();

            const workingEl = spectator.query(byTestId('div-working'));
            expect(workingEl).toBeTruthy();
            const workingField = workingEl?.innerHTML;
            const editorHTML = blockEditor.editor.getHTML();

            expect(workingField).toEqual(editorHTML);
        });
    });

    describe('Checking if we are passing HTML to the compare field', () => {
        beforeEach(async () => {
            spectator.setInput('showDiff', true);
            spectator.setInput('showAsCompare', true);
            spectator.detectChanges();
            await spectator.fixture.whenStable();
        });

        it('Should contain same HTML for compare than the Block Editor', async () => {
            await new Promise((resolve) => setTimeout(resolve, 200));
            await detectChangesNextTick(() => spectator.detectChanges());

            const blockEditor = spectator.component
                .blockEditor as unknown as BlockEditorMockComponent;
            const blockEditorCompare = spectator.component
                .blockEditorCompare as unknown as BlockEditorMockComponent;

            expect(blockEditor).toBeDefined();
            expect(blockEditor.editor).toBeDefined();
            expect(blockEditorCompare).toBeDefined();
            expect(blockEditorCompare.editor).toBeDefined();

            const pipe = new DotDiffPipe();
            const diff = pipe.transform(
                blockEditor.editor.getHTML(),
                blockEditorCompare.editor.getHTML()
            );

            const compareEl = spectator.query(byTestId('div-compare'));
            expect(compareEl).toBeTruthy();
            const compareField = compareEl?.innerHTML;
            expect(compareField).toEqual(diff);
        });
    });

    describe('Checking if we are comparing the plain HTML to the compare field', () => {
        beforeEach(async () => {
            spectator.setInput('showDiff', false);
            spectator.setInput('showAsCompare', true);
            spectator.detectChanges();
            await spectator.fixture.whenStable();
        });

        it('Should contain same plain HTML for compare than the Block Editor', async () => {
            await new Promise((resolve) => setTimeout(resolve, 200));
            await detectChangesNextTick(() => spectator.detectChanges());

            const blockEditorCompare = spectator.component
                .blockEditorCompare as unknown as BlockEditorMockComponent;

            expect(blockEditorCompare).toBeDefined();
            expect(blockEditorCompare.editor).toBeDefined();

            const compareEl = spectator.query(byTestId('div-compare'));
            expect(compareEl).toBeTruthy();
            const compareField = compareEl?.innerHTML;
            const editorHTML = blockEditorCompare.editor.getHTML();

            expect(compareField).toEqual(editorHTML);
        });
    });
});
