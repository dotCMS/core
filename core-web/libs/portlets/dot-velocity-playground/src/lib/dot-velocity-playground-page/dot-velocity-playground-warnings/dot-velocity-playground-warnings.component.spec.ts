import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotVelocityPlaygroundWarningsComponent } from './dot-velocity-playground-warnings.component';

import { VelocityWarning } from '../../models/dot-velocity-playground.models';

const WARNING: VelocityWarning = {
    type: 'UNDEFINED_REFERENCE',
    message: "Undefined reference '$x'",
    reference: '$x',
    line: 2,
    column: 1
};

describe('DotVelocityPlaygroundWarningsComponent', () => {
    let spectator: Spectator<DotVelocityPlaygroundWarningsComponent>;

    const createComponent = createComponentFactory({
        component: DotVelocityPlaygroundWarningsComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'velocityPlayground.warnings.summary': '{0} warnings',
                    'velocityPlayground.error.location': 'line {0}, column {1}'
                })
            }
        ]
    });

    const render = (warnings: VelocityWarning[]) => {
        spectator = createComponent({ props: { warnings } });
        spectator.detectChanges();
    };

    it('renders one row per warning with its type and message', () => {
        render([WARNING, { ...WARNING, type: 'NULL_SET', message: 'Null set' }]);

        const items = spectator.queryAll(byTestId('velocity-playground-warning-item'));
        expect(items.length).toBe(2);
        expect(items[0]).toHaveText('UNDEFINED_REFERENCE');
        expect(items[0]).toHaveText("Undefined reference '$x'");
        expect(items[1]).toHaveText('Null set');
    });

    it('summarizes the warning count', () => {
        render([WARNING, WARNING, WARNING]);

        expect(spectator.query(byTestId('velocity-playground-warnings-summary'))).toHaveText(
            '3 warnings'
        );
    });

    it('renders the line and column when both are present', () => {
        render([WARNING]);

        expect(spectator.query(byTestId('velocity-playground-warning-item'))).toHaveText(
            'line 2, column 1'
        );
    });

    it('falls back to an em dash when the column is missing', () => {
        render([{ ...WARNING, column: undefined }]);

        expect(spectator.query(byTestId('velocity-playground-warning-item'))).toHaveText(
            'line 2, column —'
        );
    });

    it('omits the location entirely when the warning carries no line', () => {
        render([{ ...WARNING, line: undefined, column: undefined }]);

        const item = spectator.query(byTestId('velocity-playground-warning-item'));
        expect(item).toHaveText("Undefined reference '$x'");
        expect(item?.textContent).not.toContain('line');
    });
});
