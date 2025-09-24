import { SpectatorHost, createHostFactory } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component } from '@angular/core';

import { DotContentTypeService, DotESContentService } from '@dotcms/data-access';
import { DotCMSPageAssetContainers } from '@dotcms/types';

import { EditEmaPaletteContentTypeComponent } from './components/edit-ema-palette-content-type/edit-ema-palette-content-type.component';
import { EditEmaPaletteContentletsComponent } from './components/edit-ema-palette-contentlets/edit-ema-palette-contentlets.component';
import { EditEmaPaletteComponent } from './edit-ema-palette.component';
import {
    DotPaletteState,
    DotPaletteStore,
    EditEmaPaletteStoreStatus,
    PALETTE_TYPES
} from './store/edit-ema-palette.store';

export const CONTENTLETS_MOCK = [
    {
        hostName: 'demo.dotcms.com',
        modDate: '2021-04-08 13:53:32.618',
        imageMetaData: {
            modDate: 1703020595125,
            sha256: '01bed04a0807b45245d38188da3bece44e42fcdd0cf8e8bfe0585e8bd7a61913',
            length: 15613,
            title: 'box-info-2-270x270.jpg',
            version: 20220201,
            isImage: true,
            fileSize: 15613,
            name: 'box-info-2-270x270.jpg',
            width: 270,
            contentType: 'image/jpeg',
            height: 270
        },
        publishDate: '2021-04-08 13:53:32.681',
        description:
            "Snowboarding, once a prime route for teen rebellion, today is definitely mainstream. Those teens — both guys and Shred Bettys, who took up snowboarding in the late '80s and '90s now are riding with their kids.",
        title: 'Snowboarding',
        body: "<p>As with skiing, there are different styles of riding. Free-riding is all-mountain snowboarding on the slopes, in the trees, down the steeps and through the moguls. Freestyle is snowboarding in a pipe or park filled with rails, fun boxes and other features.<br /><br />Snowboarding parks are designed for specific skill levels, from beginner parks with tiny rails hugging the ground to terrain parks with roller-coaster rails, fun boxes and tabletops for more experienced snowboarders.<br /><br />Whether you're a first-timer or already comfortable going lip-to-lip in a pipe, there are classes and special clinics for you at our ski and snowboard resorts. Our resorts offer multiday clinics, so if you're headed to ski this winter, consider wrapping your vacation dates around a snowboarding clinic.</p>",
        baseType: 'CONTENT',
        inode: 'd77576ce-6e3a-4cf3-b412-8e5209f56cae',
        archived: false,
        host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
        working: true,
        locked: false,
        stInode: '778f3246-9b11-4a2a-a101-e7fdf111bdad',
        contentType: 'Activity',
        live: true,
        altTag: 'Snowboarding',
        owner: 'dotcms.org.1',
        imageVersion: '/dA/d77576ce-6e3a-4cf3-b412-8e5209f56cae/image/box-info-2-270x270.jpg',
        identifier: '574f0aec-185a-4160-9c17-6d037b298318',
        image: '/dA/574f0aec-185a-4160-9c17-6d037b298318/image/box-info-2-270x270.jpg',
        imageContentAsset: '574f0aec-185a-4160-9c17-6d037b298318/image',
        urlTitle: 'snowboarding',
        languageId: 1,
        URL_MAP_FOR_CONTENT: '/activities/snowboarding',
        url: '/content.2f6fe5b8-a2cc-4ecb-a868-db632d695fca',
        tags: 'snowboarding,winterenthusiast',
        titleImage: 'image',
        modUserName: 'Admin User',
        urlMap: '/activities/snowboarding',
        hasLiveVersion: true,
        folder: 'SYSTEM_FOLDER',
        hasTitleImage: true,
        sortOrder: 0,
        modUser: 'dotcms.org.1',
        __icon__: 'contentIcon',
        contentTypeIcon: 'paragliding',
        variant: 'DEFAULT'
    },
    {
        hostName: 'demo.dotcms.com',
        modDate: '2020-09-02 16:42:10.049',
        imageMetaData: {
            modDate: 1703020594791,
            sha256: '7e1cf9d3c8c144f592af72658456031c8283bfe4a5ecce3e188c71aa7b1e590e',
            length: 37207,
            title: 'zip-line.jpg',
            version: 20220201,
            isImage: true,
            fileSize: 37207,
            name: 'zip-line.jpg',
            width: 270,
            contentType: 'image/jpeg',
            height: 270
        },
        publishDate: '2020-09-02 16:42:10.101',
        description:
            'Ever wondered what it is like to fly through the forest canopy? Zip-lining ais the best way to explore the forest canopy, where thick branches serve as platforms for the adventurous traveler, more than 100 feet above the forest floor.',
        title: 'Zip-Lining',
        body: '<p>Ever wondered what it is a monkey finds so fascinating about the forest canopy? Costa Rica is a pioneer in canopy exploration, where thick branches serve as platforms for the adventurous traveler, more than 100 feet above the jungle floor. If you&rsquo;re wondering why you&rsquo;d want to head to the top of a tree just to look around, remember that 90% of Costa Rica animals and 50% of plant species in rainforests live in the upper levels of the trees. When you explore that far off the ground, the view is something you&rsquo;ll never forget! A Costa Rica zip line tour, hanging bridges hike, or aerial tram tour are all fantastic ways to take advantage of Costa Rica&rsquo;s stunning forest canopy views.</p>\n<p>Almost anyone of any age and physical condition can enjoy a Costa Rica zip line adventure as it is not strenuous. Secured into a harness and attached to a sturdy cable, you will have the opportunity to fly through the rainforest canopy and experience a bird&rsquo;s eye view of the lively forest below. A Costa Rica zip line is about a five hour adventure that operates rain or shine all year and is led by bilingual guides. For the non-adrenaline junkie, the aerial tram can take you through the rainforest in comfort and safety. Prefer to linger? Hanging bridges offer panoramic views for acres, and an experienced guide will be happy to point out a variety of birds and animals.</p>',
        baseType: 'CONTENT',
        inode: '8df9a375-0386-401c-b5d6-da21c1c5c301',
        archived: false,
        host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
        working: true,
        locked: false,
        stInode: '778f3246-9b11-4a2a-a101-e7fdf111bdad',
        contentType: 'Activity',
        live: true,
        altTag: 'Zip-line',
        owner: 'dotcms.org.1',
        imageVersion: '/dA/8df9a375-0386-401c-b5d6-da21c1c5c301/image/zip-line.jpg',
        identifier: '50757fb4-75df-4e2c-8335-35d36bdb944b',
        image: '/dA/50757fb4-75df-4e2c-8335-35d36bdb944b/image/zip-line.jpg',
        imageContentAsset: '50757fb4-75df-4e2c-8335-35d36bdb944b/image',
        urlTitle: 'zip-lining',
        languageId: 1,
        URL_MAP_FOR_CONTENT: '/activities/zip-lining',
        url: '/content.c17f9c4c-ad14-4777-ae61-334ee6c9fcbf',
        tags: 'ecoenthusiast,zip-lining',
        titleImage: 'image',
        modUserName: 'Admin User',
        urlMap: '/activities/zip-lining',
        hasLiveVersion: true,
        folder: 'SYSTEM_FOLDER',
        hasTitleImage: true,
        sortOrder: 0,
        modUser: 'dotcms.org.1',
        __icon__: 'contentIcon',
        contentTypeIcon: 'paragliding',
        variant: 'DEFAULT'
    }
];

export const INITIAL_STATE_PALETTE_CONTENTTYPE_MOCK: DotPaletteState = {
    contentlets: {
        filter: {
            query: '',
            contentTypeVarName: ''
        },
        items: [],
        totalRecords: 0,
        itemsPerPage: 10
    },
    contenttypes: {
        items: [],
        filter: ''
    },
    status: EditEmaPaletteStoreStatus.LOADED,
    currentPaletteType: PALETTE_TYPES.CONTENTTYPE,
    allowedTypes: []
};

export const INITIAL_STATE_PALETTE_CONTENTLET_MOCK: DotPaletteState = {
    contentlets: {
        filter: {
            query: '',
            contentTypeVarName: 'Test'
        },
        items: CONTENTLETS_MOCK,
        totalRecords: CONTENTLETS_MOCK.length,
        itemsPerPage: 10
    },
    contenttypes: {
        items: [],
        filter: ''
    },
    status: EditEmaPaletteStoreStatus.LOADED,
    currentPaletteType: PALETTE_TYPES.CONTENTLET,
    allowedTypes: []
};

const HOST_COMPONENT_MOCK = `<dot-edit-ema-palette 
    [languageId]="languageId" 
    [variantId]="variantId" 
    [containers]="containers">
</dot-edit-ema-palette>`;

@Component({
    standalone: false,
    selector: 'dot-custom-host',
    template: ''
})
export class MockHostComponent {
    // Host Props
    languageId: number;
    variantId: string;
    containers: DotCMSPageAssetContainers;
}

const createEditEmaPaletteHost = () => {
    return createHostFactory({
        component: EditEmaPaletteComponent,
        host: MockHostComponent,
        imports: [HttpClientTestingModule],
        detectChanges: false,
        componentMocks: [EditEmaPaletteContentTypeComponent, EditEmaPaletteContentletsComponent],
        providers: [
            {
                provide: DotContentTypeService,
                useValue: {
                    filterContentTypes: () => of([]),
                    getContentTypes: () => of([])
                }
            },
            {
                provide: DotESContentService,
                useValue: {
                    get: () =>
                        of({
                            jsonObjectView: { contentlets: CONTENTLETS_MOCK },
                            resultSize: CONTENTLETS_MOCK.length
                        })
                }
            }
        ]
    });
};

describe('EditEmaPaletteComponent', () => {
    describe('ContentTypes', () => {
        let spectator: SpectatorHost<EditEmaPaletteComponent, MockHostComponent>;
        let store: DotPaletteStore;
        const createHost = createEditEmaPaletteHost();

        beforeEach(() => {
            spectator = createHost(HOST_COMPONENT_MOCK, {
                hostProps: {
                    languageId: 1,
                    variantId: 'DEFAULT',
                    containers: {}
                },
                providers: [
                    {
                        provide: DotPaletteStore,
                        useValue: {
                            vm$: of(INITIAL_STATE_PALETTE_CONTENTTYPE_MOCK),
                            isContentTypeView$: of(true),
                            loadContentlets: () => of({}),
                            changeView: () => of({}),
                            resetContentlets: () => ({}),
                            refreshContentlets: () => ({}),
                            loadAllowedContentTypes: () => ({})
                        }
                    }
                ]
            });
            store = spectator.inject(DotPaletteStore);
        });

        it('should render Content Types', () => {
            expect(spectator.query(EditEmaPaletteContentTypeComponent)).toBeDefined();
        });

        it('should not render Contentlets', () => {
            spectator.detectChanges();
            expect(spectator.query(EditEmaPaletteContentletsComponent)).toBeNull();
        });

        it('should show contentlets from content type', () => {
            spectator.detectChanges();
            const storeSpy = jest.spyOn(store, 'loadContentlets');
            spectator.triggerEventHandler(
                EditEmaPaletteContentTypeComponent,
                'showContentlets',
                'TestNameContentType'
            );
            expect(storeSpy).toHaveBeenCalledWith({
                filter: '',
                languageId: '1',
                contenttypeName: 'TestNameContentType',
                variantId: 'DEFAULT'
            });
        });

        it('should show contentlets from content type when a variant is present', () => {
            spectator.setHostInput('variantId', 'cool-variant');
            spectator.detectChanges();

            const storeSpy = jest.spyOn(store, 'loadContentlets');
            spectator.triggerEventHandler(
                EditEmaPaletteContentTypeComponent,
                'showContentlets',
                'TestNameContentType'
            );
            expect(storeSpy).toHaveBeenCalledWith({
                filter: '',
                languageId: '1',
                contenttypeName: 'TestNameContentType',
                variantId: 'cool-variant'
            });
        });

        it('should not refresh contentlets when in content type view', () => {
            const storeSpy = jest.spyOn(store, 'refreshContentlets');
            spectator.detectChanges();
            expect(storeSpy).not.toHaveBeenCalled();
        });
    });

    describe('Contentlets', () => {
        let spectator: SpectatorHost<EditEmaPaletteComponent, MockHostComponent>;
        let store: DotPaletteStore;
        const createHost = createEditEmaPaletteHost();

        beforeEach(() => {
            spectator = createHost(HOST_COMPONENT_MOCK, {
                hostProps: {
                    languageId: 1,
                    variantId: 'DEFAULT',
                    containers: {}
                },
                providers: [
                    {
                        provide: DotPaletteStore,
                        useValue: {
                            vm$: of(INITIAL_STATE_PALETTE_CONTENTLET_MOCK),
                            isContentTypeView$: of(false),
                            loadContentlets: () => of({}),
                            changeView: () => of({}),
                            resetContentlets: () => ({}),
                            refreshContentlets: () => ({}),
                            loadAllowedContentTypes: () => ({})
                        }
                    }
                ]
            });
            store = spectator.inject(DotPaletteStore);
        });

        it('should load allowed contentTypes on init', () => {
            spectator.detectChanges();
            const storeSpy = jest.spyOn(store, 'loadAllowedContentTypes');
            spectator.component.ngOnInit();
            expect(storeSpy).toHaveBeenCalledWith({ containers: {} });
        });

        it('should render Contentlets', () => {
            spectator.detectChanges();
            expect(spectator.query(EditEmaPaletteContentletsComponent)).toBeDefined();
        });

        it('should not render ContentTypes', () => {
            spectator.detectChanges();
            expect(spectator.query(EditEmaPaletteContentTypeComponent)).toBeNull();
        });

        it('should load contentlets on paginate', () => {
            spectator.detectChanges();
            const storeSpy = jest.spyOn(store, 'loadContentlets');
            spectator.triggerEventHandler(EditEmaPaletteContentletsComponent, 'paginate', {
                contentTypeVarName: 'TestNameContentType',
                page: 1
            });

            expect(storeSpy).toHaveBeenCalledWith({
                filter: '',
                languageId: '1',
                contenttypeName: 'TestNameContentType',
                page: 1,
                variantId: 'DEFAULT'
            });
        });

        it('should load contentlets on paginate when a variant is present', () => {
            spectator.setHostInput('variantId', 'cool-variant');
            spectator.detectChanges();

            const storeSpy = jest.spyOn(store, 'loadContentlets');
            spectator.triggerEventHandler(EditEmaPaletteContentletsComponent, 'paginate', {
                contentTypeVarName: 'TestNameContentType',
                page: 1
            });

            expect(storeSpy).toHaveBeenCalledWith({
                filter: '',
                languageId: '1',
                contenttypeName: 'TestNameContentType',
                page: 1,
                variantId: 'cool-variant'
            });
        });

        it('shoul refresh contentlets when in content type view', () => {
            spectator.detectChanges();
            const storeSpy = jest.spyOn(store, 'refreshContentlets');

            spectator.setHostInput('languageId', 2);
            spectator.setHostInput('variantId', 'test-variant');
            spectator.detectChanges();
            expect(storeSpy).toHaveBeenCalled();
        });
    });
});
