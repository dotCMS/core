import { exposeComponent } from '@hashbrownai/angular';
import { s } from '@hashbrownai/core';

import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';

import { DotRouterService } from '@dotcms/data-access';

@Component({
    selector: 'app-favorite-page-card',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    imports: [CardModule, ButtonModule, TagModule],
    host: {
        class: 'block w-[18rem] shrink-0 snap-start'
    },
    template: `
        @let screenshotUrl = getScreenshotUrl();

        <p-card class="h-full w-full">
            <ng-template #header>
                @if (screenshotUrl) {
                    <img
                        [src]="screenshotUrl"
                        [alt]="title()"
                        class="h-40 w-full rounded-t-xl object-cover"
                        loading="lazy" />
                } @else {
                    <div
                        class="flex h-40 w-full items-center justify-center rounded-t-xl bg-surface-100 text-sm text-surface-600">
                        No screenshot available
                    </div>
                }
            </ng-template>

            <ng-template pTemplate="title">
                <h3 class="m-0 truncate text-base font-semibold">
                    {{ title() || 'Untitled page' }}
                </h3>
            </ng-template>

            <ng-template pTemplate="subtitle">
                <span class="block truncate text-xs text-surface-600">{{ url() }}</span>
            </ng-template>

            <ng-template pTemplate="content">
                <div class="flex items-center justify-between gap-2">
                    <p-tag value="Favorite" severity="secondary" />
                    <button
                        pButton
                        type="button"
                        size="small"
                        severity="secondary"
                        label="Edit page"
                        [disabled]="!url()"
                        (click)="openEditPage()"></button>
                </div>
            </ng-template>
        </p-card>
    `
})
export class FavoritePageCardComponent {
    readonly #dotRouterService = inject(DotRouterService);

    readonly title = input.required<string>();
    readonly url = input.required<string>();
    readonly screenshot = input<string>('');
    readonly languageId = input<number>(1);

    getScreenshotUrl(): string {
        const screenshot = this.screenshot();
        if (!screenshot) {
            return '';
        }

        const languageId = this.languageId() || 1;
        return `${screenshot}?language_id=${languageId}`;
    }

    openEditPage(): void {
        const pageUrl = this.url();
        if (!pageUrl) {
            return;
        }

        const [url, search = ''] = pageUrl.split('?');
        const queryParams: Record<string, string> = { url };
        const searchParams = new URLSearchParams(search);

        for (const [key, value] of searchParams.entries()) {
            queryParams[key] = value;
        }

        void this.#dotRouterService.goToEditPage(queryParams);
    }
}

export const AiFavoritePageCardComponent = exposeComponent(FavoritePageCardComponent, {
    description: 'Displays one favorite page card and opens the page in edit mode',
    input: {
        title: s.string('Favorite page title'),
        url: s.string('Favorite page URL including optional query params'),
        screenshot: s.string('Optional screenshot URL for the page card'),
        languageId: s.number('Language id used to render screenshot correctly')
    }
});
