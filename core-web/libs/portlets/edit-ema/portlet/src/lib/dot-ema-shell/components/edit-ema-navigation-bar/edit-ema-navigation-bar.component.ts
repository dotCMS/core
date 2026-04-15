import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { filter, map } from 'rxjs/operators';

import { DotMessagePipe } from '@dotcms/ui';

import { NavigationBarItem } from '../../../shared/models';
import { UVEStore } from '../../../store/dot-uve.store';

@Component({
    selector: 'dot-edit-ema-navigation-bar',
    templateUrl: './edit-ema-navigation-bar.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ButtonModule, DotMessagePipe, TooltipModule],
    host: { class: 'flex items-center h-full border-l border-gray-200' }
})
export class EditEmaNavigationBarComponent {
    items = input<NavigationBarItem[]>([]);
    action = output<string>();

    readonly #uveStore = inject(UVEStore);
    readonly #router = inject(Router);

    $params = this.#uveStore.pageParams;

    // Emits the current URL string on every NavigationEnd — drives OnPush re-evaluation
    readonly #currentUrl = toSignal(
        this.#router.events.pipe(
            filter((e) => e instanceof NavigationEnd),
            map((e: NavigationEnd) => e.urlAfterRedirects)
        ),
        { initialValue: this.#router.url }
    );

    // Computed set of active hrefs — recalculates whenever the URL signal changes
    readonly $activeHref = computed<string>(() => {
        const currentUrl = this.#currentUrl().split('?').shift(); // track the signal

        for (const item of this.items()) {
            if (!item.href) continue;

            const url = item.href.split('/').shift();

            if (currentUrl.includes(url)) {
                return item.href;
            }
        }

        return '';
    });

    navigate(item: NavigationBarItem): void {
        if (item.isDisabled) return;

        if (item.href) {
            const params = this.$params();

            const urlFragments = item.href.split('/');
            this.#router.navigate(['edit-page'].concat(urlFragments), {
                queryParams: params,
                queryParamsHandling: 'merge'
            });
        } else {
            this.action.emit(item.id);
        }
    }
}
