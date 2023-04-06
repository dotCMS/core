import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotContentCompareTableData } from '@components/dot-content-compare/store/dot-content-compare.store';
import { DotDiffPipeModule } from '@dotcms/app/view/pipes/dot-diff/dot-diff.pipe.module';
import { BlockEditorModule } from '@dotcms/block-editor';
import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentCompareEditorComponent } from './dot-content-compare-editor.component';

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
                chartCount: 16,
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
                        data: {
                            URL_MAP_FOR_CONTENT: '/activities/snowboarding',
                            __icon__: 'contentIcon',
                            altTag: 'Snowboarding',
                            archived: false,
                            baseType: 'CONTENT',
                            body: "<p>As with skiing, there are different styles of riding. Free-riding is all-mountain snowboarding on the slopes, in the trees, down the steeps and through the moguls. Freestyle is snowboarding in a pipe or park filled with rails, fun boxes and other features.<br /><br />Snowboarding parks are designed for specific skill levels, from beginner parks with tiny rails hugging the ground to terrain parks with roller-coaster rails, fun boxes and tabletops for more experienced snowboarders.<br /><br />Whether you're a first-timer or already comfortable going lip-to-lip in a pipe, there are classes and special clinics for you at our ski and snowboard resorts. Our resorts offer multiday clinics, so if you're headed to ski this winter, consider wrapping your vacation dates around a snowboarding clinic.</p>",
                            contentType: 'Activity',
                            contentTypeIcon: 'paragliding',
                            description:
                                "Snowboarding, once a prime route for teen rebellion, today is definitely mainstream. Those teens — both guys and Shred Bettys, who took up snowboarding in the late '80s and '90s now are riding with their kids.",
                            folder: 'SYSTEM_FOLDER',
                            hasLiveVersion: true,
                            hasTitleImage: true,
                            host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                            hostName: 'demo.dotcms.com',
                            identifier: '574f0aec-185a-4160-9c17-6d037b298318',
                            image: '/dA/574f0aec-185a-4160-9c17-6d037b298318/image/box-info-2-270x270.jpg',
                            imageContentAsset: '574f0aec-185a-4160-9c17-6d037b298318/image',
                            imageMetaData: {
                                contentType: 'image/jpeg',
                                fileSize: 15613,
                                height: 270,
                                isImage: true,
                                length: 15613,
                                modDate: 1680040290173,
                                name: 'box-info-2-270x270.jpg',
                                sha256: '01bed04a0807b45245d38188da3bece44e42fcdd0cf8e8bfe0585e8bd7a61913',
                                title: 'box-info-2-270x270.jpg',
                                version: 20220201,
                                width: 270
                            },
                            imageVersion:
                                '/dA/d77576ce-6e3a-4cf3-b412-8e5209f56cae/image/box-info-2-270x270.jpg',
                            inode: 'd77576ce-6e3a-4cf3-b412-8e5209f56cae',
                            language: 'en-US',
                            languageId: 1,
                            live: true,
                            locked: false,
                            modDate: '2021-04-08 13:53:32.618',
                            modUser: 'dotcms.org.1',
                            modUserName: 'Admin User',
                            owner: 'dotcms.org.1',
                            publishDate: '2021-04-08 13:53:32.618',
                            sortOrder: 0,
                            stInode: '778f3246-9b11-4a2a-a101-e7fdf111bdad',
                            tags: 'snowboarding,winterenthusiast:persona',
                            title: 'Snowboarding',
                            titleImage: 'image',
                            url: '/content.2f6fe5b8-a2cc-4ecb-a868-db632d695fca',
                            urlMap: '/activities/snowboarding',
                            urlTitle: 'snowboarding',
                            variantId: 'DEFAULT',
                            working: true
                        }
                    },
                    type: 'dotContent'
                },
                {
                    attrs: {
                        textAlign: 'left'
                    },
                    type: 'paragraph'
                },
                {
                    attrs: {
                        alt: 'shark-feeding.jpeg',
                        data: {
                            __icon__: 'jpegIcon',
                            archived: false,
                            asset: '/dA/1acf2998-b36d-4dd7-bb73-06ce2531ee09/asset/shark-feeding.jpeg',
                            assetContentAsset: '1acf2998-b36d-4dd7-bb73-06ce2531ee09/asset',
                            assetMetaData: {
                                contentType: 'image/jpeg',
                                fileSize: 98885,
                                height: 413,
                                isImage: true,
                                length: 98885,
                                modDate: 1680040289210,
                                name: 'shark-feeding.jpeg',
                                sha256: '03fc29afd486f484b22d5b19408d61bde01314d5164f36f02bacb0d140780065',
                                title: 'shark-feeding.jpeg',
                                version: 20220201,
                                width: 780
                            },
                            assetVersion:
                                '/dA/7f71124d-8dd8-4081-b6a0-eb036464e940/asset/shark-feeding.jpeg',
                            baseType: 'DOTASSET',
                            contentType: 'PDF',
                            contentTypeIcon: 'picture_as_pdf',
                            extension: 'jpeg',
                            folder: 'SYSTEM_FOLDER',
                            hasLiveVersion: true,
                            hasTitleImage: true,
                            host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                            hostName: 'demo.dotcms.com',
                            identifier: '1acf2998-b36d-4dd7-bb73-06ce2531ee09',
                            inode: '7f71124d-8dd8-4081-b6a0-eb036464e940',
                            isContentlet: true,
                            language: 'en-US',
                            languageId: 1,
                            live: true,
                            locked: false,
                            mimeType: 'image/jpeg',
                            modDate: '2022-04-12 20:18:32.787',
                            modUser: 'dotcms.org.1',
                            modUserName: 'Admin User',
                            name: 'shark-feeding.jpeg',
                            owner: 'dotcms.org.1',
                            path: '/content.7f71124d-8dd8-4081-b6a0-eb036464e940',
                            publishDate: '2022-04-12 20:18:32.787',
                            size: 98885,
                            sortOrder: 0,
                            stInode: '657897dfb36ef211ebfb4128de818787',
                            statusIcons:
                                "<span class='greyDotIcon' style='opacity:.4'></span><span class='liveIcon'></span>",
                            title: 'shark-feeding.jpeg',
                            titleImage: 'asset',
                            type: 'dotasset',
                            url: '/content.7f71124d-8dd8-4081-b6a0-eb036464e940',
                            variantId: 'DEFAULT',
                            working: true
                        },
                        href: null,
                        src: '/dA/1acf2998-b36d-4dd7-bb73-06ce2531ee09/asset/shark-feeding.jpeg',
                        style: null,
                        textAlign: 'left',
                        title: 'shark-feeding.jpeg'
                    },
                    type: 'dotImage'
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
        html: '',
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

describe('DotContentCompareEditorComponent', () => {
    let component: DotContentCompareEditorComponent;
    let fixture: ComponentFixture<DotContentCompareEditorComponent>;

    const messageServiceMock = new MockDotMessageService({
        diff: 'Diff',
        plain: 'Plain'
    });

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotContentCompareEditorComponent],
            schemas: [CUSTOM_ELEMENTS_SCHEMA],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }],
            imports: [DotDiffPipeModule, BlockEditorModule]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotContentCompareEditorComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
        component.data = dotContentCompareTableDataMock;
        component.field = 'html';
    });

    describe('Get working data for the field', () => {
        it('should get working data', () => {
            expect(component.data.working).toBeDefined();
        });
    });
});
