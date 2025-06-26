/* eslint-disable @typescript-eslint/no-explicit-any */
import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Component, EventEmitter, inject, Output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { ListboxModule } from 'primeng/listbox';

import { pluck } from 'rxjs/operators';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-bubble-link-form',
    templateUrl: './bubble-link-form.component.html',
    styleUrls: ['./bubble-link-form.component.scss'],
    standalone: true,
    imports: [FormsModule, ListboxModule, AutoCompleteModule, ButtonModule]
})
export class BubbleLinkFormComponent {
    @Output() add = new EventEmitter<string | { name: string; value: string }>();
    protected readonly link = '';
    protected readonly value: any;
    protected items = signal<{ name: string; value: string }[]>([]);
    protected inputValue = signal<string>('');

    private httpClient = inject(HttpClient);

    addLink(value: any) {
        if (typeof value === 'string' && value.trim()) {
            this.add.emit(value.trim());
            this.inputValue.set('');
        } else if (value && value.name) {
            this.add.emit(value);
            this.inputValue.set('');
        }
    }

    search({ query }: { query: string }) {
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

    onEnter(event: KeyboardEvent) {
        if (typeof this.inputValue() === 'string' && this.inputValue().trim()) {
            this.addLink(this.inputValue());
            event.preventDefault();
        }
    }

    private getContentletsByLink(query: string): Observable<DotCMSContentlet[]> {
        return this.httpClient
            .post('/api/content/_search', {
                query: `+languageId:1 +deleted:false +working:true +(urlmap:*${query}* OR title:*${query}* OR (contentType:(dotAsset OR htmlpageasset OR fileAsset) AND +path:*${query}*))`,
                sort: 'modDate desc',
                offset: 0,
                limit: 5
            })
            .pipe(pluck('entity', 'jsonObjectView', 'contentlets'));
    }
}
