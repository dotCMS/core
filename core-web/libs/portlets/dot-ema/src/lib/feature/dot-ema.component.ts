import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DomSanitizer } from '@angular/platform-browser';
import { ActivatedRoute, Params, Router } from '@angular/router';

import { map } from 'rxjs/operators';

@Component({
    selector: 'dot-ema',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './dot-ema.component.html',
    styleUrls: ['./dot-ema.component.scss']
})
export class DotEmaComponent {
    language_id = '';
    url = '';

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

    route = inject(ActivatedRoute);
    router = inject(Router);
    sanitizer = inject(DomSanitizer);
    path$ = this.route.queryParams.pipe(
        map((params: Params) => {
            let path;

            const queryParams = Object.keys(params)
                .map((key) => {
                    if (key === 'url') {
                        path = `${decodeURIComponent(params[key])}`;

                        this.url = `${decodeURIComponent(params[key])}`;

                        return null;
                    }

                    if (key === 'language_id') {
                        this.language_id = `${decodeURIComponent(params[key])}`;
                    }

                    return `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`;
                })
                .filter(Boolean)
                .join('&');

            return this.sanitizer.bypassSecurityTrustResourceUrl(
                `http://localhost:3000/${path}?${queryParams}`
            );
        })
    );

    onChange(e: Event) {
        const name = (e.target as HTMLSelectElement).name;
        const value = (e.target as HTMLSelectElement).value;

        this.router.navigate([], {
            queryParams: {
                [name]: value
            },
            queryParamsHandling: 'merge'
        });
    }
}
