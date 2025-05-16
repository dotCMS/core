import { NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DotCMSBasicContentlet } from '@dotcms/types';

interface ActivityContentlet extends DotCMSBasicContentlet {
    image?: {
        identifier: string;
    };
    title: string;
    description: string;
    urlTitle: string;
}

@Component({
    selector: 'app-activity',
    standalone: true,
    imports: [RouterLink, NgOptimizedImage],
    template: ` <article class="overflow-hidden p-4 bg-white rounded-sm shadow-lg my-2">
        @if (contentlet().image?.identifier; as imageIdentifier) {
        <div class="relative w-full h-56 overflow-hidden">
            <img
                class="object-cover w-full h-full"
                [ngSrc]="imageIdentifier"
                width="100"
                height="100"
                alt="Activity Image" />
        </div>
        }
        <div class="px-6 py-4">
            <p class="mb-2 text-xl font-bold">{{ contentlet().title }}</p>
            <p class="text-base line-clamp-3">{{ contentlet().description }}</p>
        </div>
        <div class="px-6 pt-4 pb-2">
            <a
                [routerLink]="'/activities/' + contentlet().urlTitle || '#'"
                class="inline-block px-4 py-2 font-bold text-white bg-red-400 rounded-full hover:bg-red-500">
                Link to detail →
            </a>
        </div>
    </article>`,
    styleUrl: './activity.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ActivityComponent {
    contentlet = input.required<ActivityContentlet>();
}
