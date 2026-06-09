import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { ActivatedRoute } from '@angular/router';

import { DotMessageService } from '@dotcms/data-access';

import { DotVelocityPlaygroundShellComponent } from './dot-velocity-playground-shell.component';

import { DotVelocityPlaygroundPageComponent } from '../dot-velocity-playground-page/dot-velocity-playground-page.component';

describe('DotVelocityPlaygroundShellComponent', () => {
    const createComponent = createComponentFactory({
        component: DotVelocityPlaygroundShellComponent,
        overrideComponents: [
            [
                DotVelocityPlaygroundShellComponent,
                {
                    remove: { imports: [DotVelocityPlaygroundPageComponent] },
                    add: {}
                }
            ]
        ],
        providers: [mockProvider(DotMessageService, { get: jest.fn().mockReturnValue('') })]
    });

    const setup = (isEnterprise: boolean): Spectator<DotVelocityPlaygroundShellComponent> =>
        createComponent({
            providers: [
                {
                    provide: ActivatedRoute,
                    useValue: { data: of({ isEnterprise }) }
                }
            ]
        });

    it('renders the playground page when the instance is enterprise-licensed', () => {
        const spectator = setup(true);
        expect(spectator.query(byTestId('velocity-playground-licensed-content'))).toBeTruthy();
        expect(spectator.query(byTestId('velocity-playground-unlicensed'))).toBeFalsy();
    });

    it('renders the unlicensed empty container when the instance is not licensed', () => {
        const spectator = setup(false);
        expect(spectator.query(byTestId('velocity-playground-licensed-content'))).toBeFalsy();
        expect(spectator.query(byTestId('velocity-playground-unlicensed'))).toBeTruthy();
    });
});
