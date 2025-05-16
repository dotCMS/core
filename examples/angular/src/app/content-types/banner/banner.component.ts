import { NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { DotCMSEditableTextComponent } from '@dotcms/angular/next';
import { DotCMSBasicContentlet } from '@dotcms/types';

interface BannerContentlet extends DotCMSBasicContentlet {
    image?: {
        identifier: string;
    };
    title: string;
    caption: string;
    link: string;
    buttonText: string;
}

@Component({
    selector: 'app-banner',
    standalone: true,
    imports: [RouterLink, NgOptimizedImage, DotCMSEditableTextComponent],
    template: `<div
        class="flex overflow-hidden relative justify-center items-center w-full h-96 bg-gray-200">
        @if (contentlet().image?.identifier; as imageIdentifier) {
        <img
            class="object-cover w-full"
            [ngSrc]="imageIdentifier"
            [alt]="contentlet().title"
            fill
            priority />
        }
        <div
            class="flex absolute inset-0 flex-col justify-center items-center p-4 text-center text-white">
            <h2 class="mb-2 text-6xl font-bold text-shadow">
                <dotcms-editable-text fieldName="title" [contentlet]="contentlet()" />
            </h2>
            <p class="mb-4 text-xl text-shadow">{{ contentlet().caption }}</p>
            <a
                class="p-4 text-xl bg-red-400 rounded-sm transition duration-300 hover:bg-red-500"
                [routerLink]="contentlet().link">
                {{ contentlet().buttonText }}
            </a>
        </div>
    </div>`,
    styleUrl: './banner.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class BannerComponent {
    contentlet = input.required<BannerContentlet>();
}
