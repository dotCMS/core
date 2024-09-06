import { Spectator, createComponentFactory } from '@ngneat/spectator';

import { RouterOutlet } from '@angular/router';

import { DotLocalesShellComponent } from './dot-locales-shell.component';

describe('DotLocalesShellComponent', () => {
    let spectator: Spectator<DotLocalesShellComponent>;
    const createComponent = createComponentFactory({
        component: DotLocalesShellComponent,
        imports: [RouterOutlet]
    });

    beforeEach(() => (spectator = createComponent()));

    it('should have RouterOutlet', () => {
        const routerOutletElement = spectator.query(RouterOutlet);
        expect(routerOutletElement).not.toBeNull();
    });
});
