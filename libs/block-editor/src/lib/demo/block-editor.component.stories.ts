import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { ListboxModule } from 'primeng/listbox';
import { OrderListModule } from 'primeng/orderlist';
import { MenuModule } from 'primeng/menu';

import { NgxTiptapModule } from '../ngx-tiptap.module';
import { SuggestionsService } from '../services/suggestions.service';
import { BlockEditorComponent } from './block-editor.component';
import { SuggestionsComponent } from '../suggestions/suggestions.component';
import { ContentletBlockComponent } from '../extentions/contentlet-block/contentlet-block.component';
import { ActionButtonComponent } from '../extentions/action-button/action-button.component';

export default {
    title: 'BlockEditorComponent'
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
                                name: 'Blog'
                            },
                            {
                                name: 'Persona'
                            },
                            {
                                name: 'News Item'
                            },
                            {
                                name: 'Banner'
                            },
                            {
                                name: 'Product in the store'
                            },
                            {
                                name: 'Reatil information'
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
    component: BlockEditorComponent
});
