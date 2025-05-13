import { Component, input } from '@angular/core';

import { DatePipe, NgOptimizedImage } from '@angular/common';

import { Contentlet } from '../models';
import { DotCMSShowWhenDirective } from '@dotcms/angular/next';
import { editContentlet } from '@dotcms/uve';
import { UVE_MODE } from '@dotcms/types';

/**
 * Local component for rendering a list of contentlets outside the DotCmsLayout.
 *
 * @export
 * @class ContentletsComponent
 */
@Component({
    selector: 'app-contentlets-wrapper',
    standalone: true,
    imports: [NgOptimizedImage, DatePipe, DotCMSShowWhenDirective],
    template: `<ul class="flex flex-col gap-7">
        @for (contentlet of contentlets(); track contentlet.identifier) {

        <li class="flex gap-7 min-h-16 relative">
            <ng-template [dotCMSShowWhen]="uveMode.EDIT">
                <button
                    (click)="editContentlet(contentlet)"
                    [style]="{
                        color: 'black',
                        border: '1px solid black',
                        position: 'absolute',
                        bottom: 0,
                        right: 0,
                        width: '40px',
                        height: '20px',
                        zIndex: 1000,
                        display: 'flex',
                        justifyContent: 'center',
                        alignItems: 'center'
                    }">
                    Edit
                </button>
            </ng-template>
            <a class="relative min-w-32" [href]="contentlet.urlMap ?? contentlet.url">
                <img
                    [ngSrc]="contentlet.image.identifier"
                    [fill]="true"
                    [alt]="contentlet.urlTitle ?? contentlet.title"
                    [loaderParams]="{ languageId: contentlet.languageId || 1 }"
                    class="object-cover" />
            </a>
            <div class="flex flex-col gap-1">
                <a
                    class="text-sm font-bold text-zinc-900"
                    [href]="contentlet.urlMap ?? contentlet.url">
                    {{ contentlet.title }}
                </a>
                <time class="text-zinc-600">
                    {{ contentlet.modDate | date : 'mediumDate' }}
                </time>
            </div>
        </li>

        }
    </ul> `
})
export class ContentletsWrapperComponent {
    contentlets = input.required<Contentlet[]>();

    uveMode = UVE_MODE;

    editContentlet(contentlet: Contentlet) {
        editContentlet(contentlet);
    }
}
