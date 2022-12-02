import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { ListboxModule } from 'primeng/listbox';
import { OrderListModule } from 'primeng/orderlist';
import { MenuModule } from 'primeng/menu';

import { debounceTime, delay, tap } from 'rxjs/operators';
import { BubbleLinkFormComponent } from '@dotcms/block-editor';

import {
    ActionButtonComponent,
    ContentletBlockComponent,
    BlockEditorModule,
    SuggestionsComponent,
    SuggestionsService,
    DragHandlerComponent,
    LoaderComponent,
    DotImageService,
    DotLanguageService,
    DotBlockEditorComponent,
    FileStatus
} from '@dotcms/block-editor';

export default {
    title: 'Block Editor'
};

const contentletsMock = [
    {
        name: 'Empty Content',
        variable: 'empty',
        icon: 'hourglass_disabled',
        url: '/empty/empty-content',
        path: '/empty/empty-content',
        title: 'Cras ornare tristique elit.',
        inode: '1213',
        image: 'https://images.unsplash.com/photo-1433883669848-fa8a7ce070b2?ixlib=rb-1.2.1&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=2988&q=80',
        languageId: 2,
        modDate: '2021-10-20 14:56:53.052',
        baseType: 'CONTENT',
        archived: false,
        working: true,
        locked: false,
        live: true,
        identifier: 'f1d378c9-b784-45d0-a43c-9790af678f13',
        titleImage: 'image',
        hasLiveVersion: true,
        folder: 'SYSTEM_FOLDER',
        hasTitleImage: true,
        __icon__: 'contentIcon',
        contentTypeIcon: 'file_copy',
        contentType: 'empty'
    },
    {
        name: 'Blog',
        icon: 'article',
        url: '/blog/Blog',
        variable: 'blog',
        title: 'Cras ornare tristique elit.',
        inode: '1213',
        image: 'https://images.unsplash.com/photo-1433883669848-fa8a7ce070b2?ixlib=rb-1.2.1&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=2988&q=80',
        languageId: 2,
        modDate: '2021-10-20 14:56:53.052',
        baseType: 'CONTENT',
        archived: false,
        working: true,
        locked: false,
        live: true,
        identifier: 'f1d378c9-b784-45d0-a43c-9790af678f13',
        titleImage: 'image',
        hasLiveVersion: true,
        folder: 'SYSTEM_FOLDER',
        hasTitleImage: true,
        __icon__: 'contentIcon',
        contentTypeIcon: 'file_copy',
        contentType: 'Blog'
    },
    {
        name: 'Persona',
        icon: 'face',
        url: '/persona/persona',
        path: '/persona/persona',
        variable: 'persona',
        title: 'Cras ornare tristique elit.',
        inode: '1213',
        image: 'https://images.unsplash.com/photo-1433883669848-fa8a7ce070b2?ixlib=rb-1.2.1&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=2988&q=80',
        languageId: 2,
        modDate: '2021-10-20 14:56:53.052',
        baseType: 'CONTENT',
        archived: false,
        working: true,
        locked: false,
        live: true,
        identifier: 'f1d378c9-b784-45d0-a43c-9790af678f13',
        titleImage: 'image',
        hasLiveVersion: true,
        folder: 'SYSTEM_FOLDER',
        hasTitleImage: true,
        __icon__: 'contentIcon',
        contentTypeIcon: 'file_copy',
        contentType: 'Blog'
    },
    {
        name: 'News Item',
        icon: 'mic',
        url: '/news_item/news-item',
        path: '/news_item/news-item',
        variable: 'news_item',
        title: 'Cras ornare tristique elit.',
        inode: '1213',
        image: 'https://images.unsplash.com/photo-1433883669848-fa8a7ce070b2?ixlib=rb-1.2.1&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=2988&q=80',
        languageId: 2,
        modDate: '2021-10-20 14:56:53.052',
        baseType: 'CONTENT',
        archived: false,
        working: true,
        locked: false,
        live: true,
        identifier: 'f1d378c9-b784-45d0-a43c-9790af678f13',
        titleImage: 'image',
        hasLiveVersion: true,
        folder: 'SYSTEM_FOLDER',
        hasTitleImage: true,
        __icon__: 'contentIcon',
        contentTypeIcon: 'file_copy',
        contentType: 'Blog'
    },
    {
        name: 'Banner',
        icon: 'view_carousel',
        url: '/banner/banner',
        path: '/banner/banner',
        variable: 'banner',
        title: 'Cras ornare tristique elit.',
        inode: '1213',
        image: 'https://images.unsplash.com/photo-1433883669848-fa8a7ce070b2?ixlib=rb-1.2.1&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=2988&q=80',
        languageId: 2,
        modDate: '2021-10-20 14:56:53.052',
        baseType: 'CONTENT',
        archived: false,
        working: true,
        locked: false,
        live: true,
        identifier: 'f1d378c9-b784-45d0-a43c-9790af678f13',
        titleImage: 'image',
        hasLiveVersion: true,
        folder: 'SYSTEM_FOLDER',
        hasTitleImage: true,
        __icon__: 'contentIcon',
        contentTypeIcon: 'file_copy',
        contentType: 'Blog'
    },
    {
        name: 'Product in the store',
        icon: 'inventory_2',
        url: '/inventory/product-in-the-store',
        path: '/inventory/product-in-the-store',
        variable: 'inventory',
        title: 'Cras ornare tristique elit.',
        inode: '1213',
        image: 'https://images.unsplash.com/photo-1433883669848-fa8a7ce070b2?ixlib=rb-1.2.1&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=2988&q=80',
        languageId: 2,
        modDate: '2021-10-20 14:56:53.052',
        baseType: 'CONTENT',
        archived: false,
        working: true,
        locked: false,
        live: true,
        identifier: 'f1d378c9-b784-45d0-a43c-9790af678f13',
        titleImage: 'image',
        hasLiveVersion: true,
        folder: 'SYSTEM_FOLDER',
        hasTitleImage: true,
        __icon__: 'contentIcon',
        contentTypeIcon: 'file_copy',
        contentType: 'Blog'
    },
    {
        name: 'Reatil information',
        icon: 'storefront',
        url: '/retail/reatil-information',
        path: '/retail/reatil-information',
        variable: 'retail',
        title: 'Cras ornare tristique elit.',
        inode: '1213',
        image: 'https://images.unsplash.com/photo-1433883669848-fa8a7ce070b2?ixlib=rb-1.2.1&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=2988&q=80',
        languageId: 2,
        modDate: '2021-10-20 14:56:53.052',
        baseType: 'CONTENT',
        archived: false,
        working: true,
        locked: false,
        live: true,
        identifier: 'f1d378c9-b784-45d0-a43c-9790af678f13',
        titleImage: 'image',
        hasLiveVersion: true,
        folder: 'SYSTEM_FOLDER',
        hasTitleImage: true,
        __icon__: 'contentIcon',
        contentTypeIcon: 'file_copy',
        contentType: 'Blog'
    }
];

export const primary = () => ({
    moduleMetadata: {
        imports: [
            MenuModule,
            CommonModule,
            FormsModule,
            BlockEditorModule,
            OrderListModule,
            ListboxModule,
            BrowserAnimationsModule
        ],
        providers: [
            {
                provide: DotImageService,
                useValue: {
                    publishContent({
                        data: _data,
                        statusCallback = (_status) => {
                            /* */
                        }
                    }) {
                        statusCallback(FileStatus.IMPORT);

                        return of([
                            {
                                cd769844de530f7b5d3434b1b5cfdd62: {
                                    asset: 'https://media.istockphoto.com/vectors/costa-rica-vector-id652225694?s=170667a',
                                    mimeType: 'image/png',
                                    name: 'costarica.png',
                                    icon: 'inventory_2',
                                    url: '/inventory/product-in-the-store',
                                    path: '/inventory/product-in-the-store',
                                    variable: 'inventory',
                                    title: 'Cras ornare tristique elit.',
                                    inode: '1213',
                                    image: 'https://images.unsplash.com/photo-1433883669848-fa8a7ce070b2?ixlib=rb-1.2.1&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=2988&q=80',
                                    languageId: 2,
                                    modDate: '2021-10-20 14:56:53.052',
                                    baseType: 'CONTENT',
                                    archived: false,
                                    working: true,
                                    locked: false,
                                    live: true,
                                    identifier: 'f1d378c9-b784-45d0-a43c-9790af678f13',
                                    titleImage: 'image',
                                    hasLiveVersion: true,
                                    folder: 'SYSTEM_FOLDER',
                                    hasTitleImage: true,
                                    __icon__: 'contentIcon',
                                    contentTypeIcon: 'file_copy',
                                    contentType: 'Blog'
                                }
                            }
                        ]).pipe(
                            delay(800),
                            tap(() => statusCallback(FileStatus.IMPORT))
                        );
                    }
                }
            },
            {
                provide: SuggestionsService,
                useValue: {
                    getContentTypes(filter = '') {
                        return of(
                            filter
                                ? contentletsMock.filter((item) =>
                                      item.name.match(new RegExp(filter, 'i'))
                                  )
                                : contentletsMock
                        );
                    },
                    getContentlets({ contentType, filter = '' }) {
                        if (contentType === 'empty') {
                            return of([]).pipe(delay(800));
                        }

                        return of(
                            filter
                                ? contentletsMock.filter((item) =>
                                      item.title.match(new RegExp(filter, 'i'))
                                  )
                                : contentletsMock
                        );
                    },
                    getContentletsUrlMap({ filter }) {
                        return of(
                            contentletsMock.filter((item) =>
                                item.url.match(new RegExp(filter, 'i'))
                            )
                        ).pipe(debounceTime(400));
                    }
                }
            },
            {
                provide: DotLanguageService,
                useValue: {
                    getLanguages() {
                        return of({
                            1: {
                                country: 'United States',
                                countryCode: 'US',
                                defaultLanguage: true,
                                id: 1,
                                language: 'English',
                                languageCode: 'en'
                            },
                            2: {
                                country: 'Espana',
                                countryCode: 'ES',
                                defaultLanguage: false,
                                id: 2,
                                language: 'Espanol',
                                languageCode: 'es'
                            }
                        });
                    }
                }
            }
        ],
        // We need these here because they are dynamically rendered
        entryComponents: [
            SuggestionsComponent,
            ContentletBlockComponent,
            ActionButtonComponent,
            DragHandlerComponent,
            LoaderComponent,
            BubbleLinkFormComponent
        ]
    },
    component: DotBlockEditorComponent
});
