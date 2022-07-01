import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { ListboxModule } from 'primeng/listbox';
import { OrderListModule } from 'primeng/orderlist';
import { MenuModule } from 'primeng/menu';

import { delay } from 'rxjs/operators';
import { DotBlockEditorComponent } from './dot-block-editor.component';
import { BubbleMenuLinkFormComponent } from '@dotcms/block-editor';
import {
    ActionButtonComponent,
    ContentletBlockComponent,
    NgxTiptapModule,
    SuggestionsComponent,
    SuggestionsService,
    ImageBlockComponent,
    DragHandlerComponent,
    LoaderComponent,
    DotImageService,
    DotLanguageService
} from '@dotcms/block-editor';

export default {
    title: 'Block Editor'
}component: DotBlockEditorComponent,;

export const primary = () => ({
    moduleMetadata: {
        imports: [
            MenuModule,
            CommonModule,
            FormsModule,
            NgxTiptapModule,
            OrderListModule,
            ListboxModule,
            BrowserAnimationsModule
        ],
        providers: [
            {
                provide: DotImageService,
                useValue: {
                    publishContent() {
                        return of([
                            {
                                cd769844de530f7b5d3434b1b5cfdd62: {
                                    asset: 'https://media.istockphoto.com/vectors/costa-rica-vector-id652225694?s=170667a',
                                    mimeType: 'image/png',
                                    name: 'costarica.png'
                                }
                            }
                        ]).pipe(delay(800));
                    }
                }
            },
            {
                provide: SuggestionsService,
                useValue: {
                    getContentTypes(filter = '') {
                        const items = [
                            {
                                name: 'Empty Content',
                                icon: 'hourglass_disabled',
                                variable: 'empty'
                            },
                            {
                                name: 'Blog',
                                icon: 'article',
                                variable: 'blog'
                            },
                            {
                                name: 'Persona',
                                icon: 'face',
                                variable: 'persona'
                            },
                            {
                                name: 'News Item',
                                icon: 'mic',
                                variable: 'news_item'
                            },
                            {
                                name: 'Banner',
                                icon: 'view_carousel',
                                variable: 'banner'
                            },
                            {
                                name: 'Product in the store',
                                icon: 'inventory_2',
                                variable: 'inventory'
                            },
                            {
                                name: 'Reatil information',
                                icon: 'storefront',
                                variable: 'retail'
                            }
                        ];
                        return of(
                            filter
                                ? items.filter((item) => item.name.match(new RegExp(filter, 'i')))
                                : items
                        );
                    },
                    getContentlets(type, filter = '') {
                        if (type === 'empty') {
                            return of([]).pipe(delay(800));
                        }
                        const items = [
                            {
                                modDate: '2021-10-20 14:56:53.052',
                                title: 'Easy Snowboard Tricks You can Start Using Right Away',
                                baseType: 'CONTENT',
                                inode: 'af12671a-1cff-44da-92cc-ce4fae2e4a70',
                                archived: false,
                                working: true,
                                locked: false,
                                live: true,
                                identifier: 'f1d378c9-b784-45d0-a43c-9790af678f13',
                                image: '/dA/f1d378c9-b784-45d0-a43c-9790af678f13/image/adventure-alpine-climb-240160.jpg',
                                languageId: 1,
                                titleImage: 'image',
                                hasLiveVersion: true,
                                folder: 'SYSTEM_FOLDER',
                                hasTitleImage: true,
                                __icon__: 'contentIcon',
                                contentTypeIcon: 'file_copy',
                                contentType: 'Blog'
                            },
                            {
                                title: 'Aliquam tincidunt mauris eu risus.',
                                inode: '456',
                                image: 'https://images.unsplash.com/photo-1433883669848-fa8a7ce070b2?ixlib=rb-1.2.1&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=2988&q=80',
                                languageId: 1,
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
                                title: 'Vestibulum auctor dapibus neque.',
                                inode: '789',
                                image: 'https://images.unsplash.com/photo-1433883669848-fa8a7ce070b2?ixlib=rb-1.2.1&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=2988&q=80',
                                languageId: 1,
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
                                title: 'Nunc dignissim risus id metus.',
                                inode: '1011',
                                image: 'https://images.unsplash.com/photo-1433883669848-fa8a7ce070b2?ixlib=rb-1.2.1&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=2988&q=80',
                                languageId: 1,
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
                                title: 'Cras ornare tristique elit.',
                                inode: '1213',
                                image: 'https://images.unsplash.com/photo-1433883669848-fa8a7ce070b2?ixlib=rb-1.2.1&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=2988&q=80',
                                languageId: 1,
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
                                title: 'Vivamus vestibulum ntulla nec ante.',
                                inode: '1415',
                                image: 'https://images.unsplash.com/photo-1433883669848-fa8a7ce070b2?ixlib=rb-1.2.1&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=2988&q=80',
                                languageId: 1,
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
                                title: 'Lorem ipsum dolor sit amet, consectetuer adipiscing elit.',
                                inode: '123',
                                image: 'https://images.unsplash.com/photo-1433883669848-fa8a7ce070b2?ixlib=rb-1.2.1&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=2988&q=80',
                                languageId: 1,
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
                                title: 'Aliquam tincidunt mauris eu risus.',
                                inode: '456',
                                image: 'https://images.unsplash.com/photo-1433883669848-fa8a7ce070b2?ixlib=rb-1.2.1&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=2988&q=80',
                                languageId: 1,
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
                                title: 'Vestibulum auctor dapibus neque.',
                                inode: '789',
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
                                title: 'Nunc dignissim risus id metus.',
                                inode: '1011',
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
                                title: 'Vivamus vestibulum ntulla nec ante.',
                                inode: '1415',
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
                                title: 'Lorem ipsum dolor sit amet, consectetuer adipiscing elit.',
                                inode: '123',
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
                                title: 'Aliquam tincidunt mauris eu risus.',
                                inode: '456',
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
                                title: 'Vestibulum auctor dapibus neque.',
                                inode: '789',
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
                                title: 'Nunc dignissim risus id metus.',
                                inode: '1011',
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
                                title: 'Vivamus vestibulum ntulla nec ante.',
                                inode: '1415',
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
                                title: 'Lorem ipsum dolor sit amet, consectetuer adipiscing elit.',
                                inode: '123',
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
                                title: 'Aliquam tincidunt mauris eu risus.',
                                inode: '456',
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
                                title: 'Vestibulum auctor dapibus neque.',
                                inode: '789',
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
                                title: 'Nunc dignissim risus id metus.',
                                inode: '1011',
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
                                title: 'Vivamus vestibulum ntulla nec ante.',
                                inode: '1415',
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
                        return of(
                            filter
                                ? items.filter((item) => item.title.match(new RegExp(filter, 'i')))
                                : items
                        );
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
            ImageBlockComponent,
            LoaderComponent,
            BubbleMenuLinkFormComponent
        ]
    },
    });
