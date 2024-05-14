import { Spectator, createComponentFactory } from '@ngneat/spectator';

import { RouterOutlet } from '@angular/router';

import { DotLanguagesShellComponent } from './dot-languages-shell.component';

describe('DotLanguagesShellComponent', () => {
    let spectator: Spectator<DotLanguagesShellComponent>;
    const createComponent = createComponentFactory({
        component: DotLanguagesShellComponent,
        imports: [RouterOutlet]
    });

    beforeEach(() => (spectator = createComponent()));

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should have RouterOutlet', () => {
        const routerOutletElement = spectator.query(RouterOutlet);
        expect(routerOutletElement).not.toBeNull();
    });
});
