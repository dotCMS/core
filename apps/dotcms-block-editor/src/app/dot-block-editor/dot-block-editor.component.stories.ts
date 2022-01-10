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
    DotImageService
} from '@dotcms/block-editor';

export default {
    title: 'Block Editor'
};

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
                                    asset:
                                        'https://media.istockphoto.com/vectors/costa-rica-vector-id652225694?s=170667a',
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
                    getContentTypes() {
                        return of([
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
                        ]);
                    },
                    getContentlets(type) {
                        if (type === 'empty') {
                            return of([]).pipe(delay(800));
                        }

                        return of([
                            {
                                title: 'Lorem ipsum dolor sit amet, consectetuer adipiscing elit.',
                                inode: '123'
                            },
                            {
                                title: 'Aliquam tincidunt mauris eu risus.',
                                inode: '456'
                            },
                            {
                                title: 'Vestibulum auctor dapibus neque.',
                                inode: '789'
                            },
                            {
                                title: 'Nunc dignissim risus id metus.',
                                inode: '1011'
                            },
                            {
                                title: 'Cras ornare tristique elit.',
                                inode: '1213'
                            },
                            {
                                title: 'Vivamus vestibulum ntulla nec ante.',
                                inode: '1415'
                            },
                            {
                                title: 'Lorem ipsum dolor sit amet, consectetuer adipiscing elit.',
                                inode: '123'
                            },
                            {
                                title: 'Aliquam tincidunt mauris eu risus.',
                                inode: '456'
                            },
                            {
                                title: 'Vestibulum auctor dapibus neque.',
                                inode: '789'
                            },
                            {
                                title: 'Nunc dignissim risus id metus.',
                                inode: '1011'
                            },
                            {
                                title: 'Cras ornare tristique elit.',
                                inode: '1213'
                            },
                            {
                                title: 'Vivamus vestibulum ntulla nec ante.',
                                inode: '1415'
                            },
                            {
                                title: 'Lorem ipsum dolor sit amet, consectetuer adipiscing elit.',
                                inode: '123'
                            },
                            {
                                title: 'Aliquam tincidunt mauris eu risus.',
                                inode: '456'
                            },
                            {
                                title: 'Vestibulum auctor dapibus neque.',
                                inode: '789'
                            },
                            {
                                title: 'Nunc dignissim risus id metus.',
                                inode: '1011'
                            },
                            {
                                title: 'Cras ornare tristique elit.',
                                inode: '1213'
                            },
                            {
                                title: 'Vivamus vestibulum ntulla nec ante.',
                                inode: '1415'
                            },
                            {
                                title: 'Lorem ipsum dolor sit amet, consectetuer adipiscing elit.',
                                inode: '123'
                            },
                            {
                                title: 'Aliquam tincidunt mauris eu risus.',
                                inode: '456'
                            },
                            {
                                title: 'Vestibulum auctor dapibus neque.',
                                inode: '789'
                            },
                            {
                                title: 'Nunc dignissim risus id metus.',
                                inode: '1011'
                            },
                            {
                                title: 'Cras ornare tristique elit.',
                                inode: '1213'
                            },
                            {
                                title: 'Vivamus vestibulum ntulla nec ante.',
                                inode: '1415'
                            }
                        ]).pipe(delay(800));
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
    component: DotBlockEditorComponent
});
