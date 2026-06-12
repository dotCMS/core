import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';

import { map } from 'rxjs/operators';

import { DotEmptyContainerComponent, PrincipalConfiguration } from '@dotcms/ui';

import { DotVelocityPlaygroundPageComponent } from '../dot-velocity-playground-page/dot-velocity-playground-page.component';

@Component({
    selector: 'dot-velocity-playground-shell',
    imports: [DotVelocityPlaygroundPageComponent, DotEmptyContainerComponent],
    templateUrl: './dot-velocity-playground-shell.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0' }
})
export class DotVelocityPlaygroundShellComponent {
    readonly #route = inject(ActivatedRoute);

    readonly $isLicensed = toSignal(
        this.#route.data.pipe(map((data) => data['isEnterprise'] ?? false)),
        { initialValue: false }
    );

    readonly unlicensedConfig: PrincipalConfiguration = {
        title: 'velocityPlayground.unlicensed.title',
        icon: 'pi-lock',
        subtitle: 'velocityPlayground.unlicensed.description'
    };
}
