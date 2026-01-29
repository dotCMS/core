import { moduleMetadata } from '@storybook/angular';
import { of } from 'rxjs';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { Listbox } from 'primeng/listbox';
import { Menu } from 'primeng/menu';
import { OrderList } from 'primeng/orderlist';

import { debounceTime, delay, tap } from 'rxjs/operators';

import {
    DotAiService,
    DotContentSearchService,
    DotMessageService,
    DotPropertiesService,
    DotUploadFileService,
    FileStatus
} from '@dotcms/data-access';
import { DotSpinnerComponent } from '@dotcms/ui';

import { DotBlockEditorComponent } from './dot-block-editor.component';

import { BlockEditorModule } from '../../block-editor.module';
import {
    AssetFormComponent,
    BubbleLinkFormComponent,
    DragHandlerComponent,
    UploadPlaceholderComponent
} from '../../extensions';
import { ContentletBlockComponent } from '../../nodes';
import {
    ASSET_MOCK,
    CONTENTLETS_MOCK,
    SuggestionsComponent,
    SuggestionsService
} from '../../shared';
import { DotAiServiceMock } from '../../shared/mocks/dot-ai-service.mock';
import { DotMessageServiceMock } from '../../shared/mocks/dot-message.service.mock';

export default {
    title: 'Library/Block Editor'
};

export const Primary = () => ({
    title: 'Library/Block Editor',
    component: DotBlockEditorComponent,
    decorators: [
        moduleMetadata({
            imports: [
                Menu,
                CommonModule,
                FormsModule,
                BlockEditorModule,
                OrderList,
                Listbox,
                DotSpinnerComponent
            ],
            providers: [
                {
                    provide: DotPropertiesService,
                    useValue: {
                        getKey(_key) {
                            return of('true');
                        }
                    }
                },
                {
                    provide: DotUploadFileService,
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
                                delay(1500),
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
                                    ? CONTENTLETS_MOCK.filter((item) =>
                                          item.name.match(new RegExp(filter, 'i'))
                                      )
                                    : CONTENTLETS_MOCK
                            );
                        },
                        getContentlets({ contentType, filter = '' }) {
                            if (contentType === 'empty') {
                                return of([]).pipe(delay(800));
                            }

                            return of(
                                filter
                                    ? CONTENTLETS_MOCK.filter((item) =>
                                          item.title.match(new RegExp(filter, 'i'))
                                      )
                                    : CONTENTLETS_MOCK
                            );
                        },
                        getContentletsUrlMap({ filter }) {
                            return of(
                                CONTENTLETS_MOCK.filter((item) =>
                                    item.url.match(new RegExp(filter, 'i'))
                                )
                            ).pipe(debounceTime(400));
                        }
                    }
                },
                {
                    provide: DotContentSearchService,
                    useValue: {
                        get(params) {
                            const query = params.query.match(new RegExp(/(?<=:)(.*?)(?=\*)/))[0];
                            const contenttype = params.query.match(
                                new RegExp(/(?<=(contenttype:))(.*?)(?=\/)/)
                            )[0];

                            const mock = ASSET_MOCK[contenttype];
                            const contentlets = query
                                ? mock.filter(({ fileName }) => fileName.includes(query))
                                : mock;

                            return of({
                                jsonObjectView: {
                                    contentlets: contentlets.slice(
                                        params.offset,
                                        params.offset + 20
                                    )
                                },
                                resultsSize: query ? contentlets?.length : mock.length
                            }).pipe(delay(1000));
                        }
                    }
                },
                {
                    provide: DotAiService,
                    useClass:
                        process.env.USE_MIDDLEWARE === 'true' ? DotAiService : DotAiServiceMock
                },
                {
                    provide: DotMessageService,
                    useClass:
                        process.env.USE_MIDDLEWARE === 'true'
                            ? DotMessageService
                            : DotMessageServiceMock
                }
            ], // We need these here because they are dynamically rendered
            entryComponents: [
                SuggestionsComponent,
                ContentletBlockComponent,
                DragHandlerComponent,
                UploadPlaceholderComponent,
                BubbleLinkFormComponent,
                AssetFormComponent
            ]
        })
    ]
});
