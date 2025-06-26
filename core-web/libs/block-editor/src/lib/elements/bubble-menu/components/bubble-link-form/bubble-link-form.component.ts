/* eslint-disable @typescript-eslint/no-explicit-any */
import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Component, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { ListboxModule } from 'primeng/listbox';

import { pluck } from 'rxjs/operators';

import { Editor } from '@tiptap/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { EditorModalDirective } from '../../../../directive/editor-modal.directive';

@Component({
    selector: 'dot-bubble-link-form',
    templateUrl: './bubble-link-form.component.html',
    styleUrls: ['./bubble-link-form.component.scss'],
    standalone: true,
    hostDirectives: [
        {
            directive: EditorModalDirective,
            inputs: ['editor']
        }
    ],
    imports: [FormsModule, ListboxModule, AutoCompleteModule, ButtonModule]
})
export class BubbleLinkFormComponent {
    private editorModal = inject<EditorModalDirective>(EditorModalDirective);
    protected readonly editor = input.required<Editor>();
    protected readonly items = signal<{ name: string; value: string }[]>([]);
    protected readonly inputValue = signal<string>('');

    private readonly httpClient = inject(HttpClient);

    protected addLink(input: string | { name: string; value: string }) {
        const link = typeof input === 'string' ? input.trim() : input.value.trim();
        this.editor().chain().focus().setLink({ href: link, target: '_blank' }).run();
        this.editorModal.hide();
    }

    protected onEnter(event: KeyboardEvent) {
        if (typeof this.inputValue() === 'string' && this.inputValue().trim()) {
            this.addLink(this.inputValue());
            event.preventDefault();
        }
    }

    protected search({ query }: { query: string }) {
        this.getContentletsByLink(query).subscribe((contentlets) => {
            this.items.set(
                contentlets.map((contentlet) => ({
                    hasTitleImage: contentlet.hasTitleImage,
                    inode: contentlet.inode,
                    name: contentlet.title,
                    value: contentlet.path || contentlet.urlMap
                }))
            );
        });
    }

    private getContentletsByLink(query: string): Observable<DotCMSContentlet[]> {
        return this.httpClient
            .post('/api/content/_search', {
                query: `+languageId:1 +deleted:false +working:true  +(urlmap:* OR basetype:5)  +deleted:false +(title:${query}* OR path:*${query}* OR urlmap:*${query}*)`,
                sort: 'modDate desc',
                offset: 0,
                limit: 5
            })
            .pipe(pluck('entity', 'jsonObjectView', 'contentlets'));
    }
}
