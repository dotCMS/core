import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Params, Router } from '@angular/router';

import { EditEmaStore } from './store/dot-ema.store';

@Component({
    selector: 'dot-ema',
    standalone: true,
    imports: [CommonModule, FormsModule],
    providers: [EditEmaStore],
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
            name: 'Page One',
            value: 'page-one'
        },
        {
            name: 'Page Two',
            value: 'page-two'
        }
    ];

    store = inject(EditEmaStore);
    route = inject(ActivatedRoute);
    router = inject(Router);

    iframeUrl$ = this.store.iframeUrl$;
    url$ = this.store.url$;
    language_id$ = this.store.language_id$;

    ngOnInit(): void {
        this.route.queryParams.subscribe(({ language_id, url }: Params) => {
            this.store.load({
                language_id,
                url
            });
        });
    }

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
