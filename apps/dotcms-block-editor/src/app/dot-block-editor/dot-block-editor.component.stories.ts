import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { ListboxModule } from 'primeng/listbox';
import { OrderListModule } from 'primeng/orderlist';
import { MenuModule } from 'primeng/menu';
import { DotBlockEditorComponent } from './dot-block-editor.component';

import { NgxTiptapModule } from '@dotcms/block-editor';

import { SuggestionsService } from '@dotcms/block-editor';
import { SuggestionsComponent } from '@dotcms/block-editor';
import { ContentletBlockComponent } from '@dotcms/block-editor';
import { ActionButtonComponent } from '@dotcms/block-editor';

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
                provide: SuggestionsService,
                useValue: {
                    getContentTypes() {
                        return of([
                            {
                                name: 'Blog',
                                icon: 'article'
                            },
                            {
                                name: 'Persona',
                                icon: 'face'
                            },
                            {
                                name: 'News Item',
                                icon: 'mic'
                            },
                            {
                                name: 'Banner',
                                icon: 'view_carousel'
                            },
                            {
                                name: 'Product in the store',
                                icon: 'inventory_2',
                            },
                            {
                                name: 'Reatil information',
                                icon: 'storefront'
                            }
                        ]);
                    },
                    getContentlets() {
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
                            }
                        ]);
                    }
                }
            }
        ],
        // We need these here because they are dynamically rendered
        entryComponents: [SuggestionsComponent, ContentletBlockComponent, ActionButtonComponent]
    },
    component: DotBlockEditorComponent
});
