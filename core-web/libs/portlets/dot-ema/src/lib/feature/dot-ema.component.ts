import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Params, Router } from '@angular/router';

import { SafeUrlPipe } from '@dotcms/ui';

import { EditEmaStore } from './store/dot-ema.store';

import { DotPageApiService } from '../services/dot-page-api.service';

@Component({
    selector: 'dot-ema',
    standalone: true,
    imports: [CommonModule, FormsModule, SafeUrlPipe],
    providers: [EditEmaStore, DotPageApiService],
    templateUrl: './dot-ema.component.html',
    styleUrls: ['./dot-ema.component.scss']
})
export class DotEmaComponent implements OnInit {
    languages = [
        {
            name: 'English',
            value: '1'
        },
        {
            name: 'Spanish',
            value: '2'
        }
    ];

    pages = [
        {
            name: 'Home',
            value: 'index'
        },
        {
            name: 'Page One',
            value: 'page-one'
        },
        {
            name: 'Page Two',
            value: 'page-two'
        }
    ];

    route = inject(ActivatedRoute);
    router = inject(Router);
    store = inject(EditEmaStore);

    iframeUrl$ = this.store.iframeUrl$;
    language_id$ = this.store.language_id$;
    title$ = this.store.pageTitle$;
    url$ = this.store.url$;

    ngOnInit(): void {
        this.route.queryParams.subscribe(({ language_id, url }: Params) => {
            const queryParams = {};

            if (!language_id) {
                queryParams['language_id'] = '1';
            }

            if (!url) {
                queryParams['url'] = 'index';
            }

            if (Object.keys(queryParams).length > 0) {
                this.router.navigate([], {
                    queryParams,
                    queryParamsHandling: 'merge'
                });
            }

            this.store.load({
                language_id: language_id || '1',
                url: url || 'index'
            });
        });
    }

    /**
     * Updates store value and navigates with updated query parameters on select element change event.
     *
     * @param {Event} e
     * @memberof DotEmaComponent
     */
    onChange(e: Event) {
        const name = (e.target as HTMLSelectElement).name;
        const value = (e.target as HTMLSelectElement).value;

        switch (name) {
            case 'language_id':
                this.store.setLanguage(value);
                break;

            case 'url':
                this.store.setUrl(value);
                break;
        }

        this.router.navigate([], {
            queryParams: {
                [name]: value
            },
            queryParamsHandling: 'merge'
        });
    }
}
