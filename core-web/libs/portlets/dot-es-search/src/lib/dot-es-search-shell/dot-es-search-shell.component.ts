import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';

import { map } from 'rxjs/operators';

import { DotEmptyContainerComponent, PrincipalConfiguration } from '@dotcms/ui';

import { DotEsSearchPageComponent } from '../dot-es-search-page/dot-es-search-page.component';

@Component({
    selector: 'dot-es-search-shell',
    imports: [DotEsSearchPageComponent, DotEmptyContainerComponent],
    template: `
        @if ($isLicensed()) {
            <dot-es-search-page />
        } @else {
            <dot-empty-container [configuration]="unlicensedConfig" [hideContactUsLink]="true" />
        }
    `,
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0 block' }
})
export class DotEsSearchShellComponent {
    readonly #route = inject(ActivatedRoute);

    readonly $isLicensed = toSignal(
        this.#route.data.pipe(map((data) => data['isEnterprise'] ?? false)),
        { initialValue: false }
    );

    readonly unlicensedConfig: PrincipalConfiguration = {
        title: 'esSearch.unlicensed.title',
        icon: 'pi-lock',
        subtitle: 'esSearch.unlicensed.description'
    };
}
