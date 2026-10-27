import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/vitest';
import { vi } from 'vitest';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotContainerDependencies } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotContainerInUseDialogComponent } from './dot-container-in-use-dialog.component';

const messageServiceMock = new MockDotMessageService({
    Close: 'Close',
    'message.containers.in_use.explainer': 'Remove this container from the templates below.',
    'message.containers.in_use.explainer.many': 'Remove each container from its templates.',
    'message.containers.in_use.used_by': 'Used by'
});

const container = (
    identifier: string,
    templates: DotContainerDependencies['templates']
): DotContainerDependencies => ({
    inode: `inode-${identifier}`,
    identifier,
    title: `Container ${identifier}`,
    templates
});

const template = (identifier: string, siteName: string | null = 'demo.dotcms.com') => ({
    inode: `inode-${identifier}`,
    identifier,
    title: `Template ${identifier}`,
    siteName
});

describe('DotContainerInUseDialogComponent', () => {
    let spectator: Spectator<DotContainerInUseDialogComponent>;
    const ref = { close: vi.fn() };

    const createComponent = createComponentFactory({
        component: DotContainerInUseDialogComponent,
        providers: [
            { provide: DotMessageService, useValue: messageServiceMock },
            { provide: DynamicDialogRef, useValue: ref },
            { provide: DynamicDialogConfig, useValue: { data: [] } }
        ],
        detectChanges: false
    });

    const open = (data: DotContainerDependencies[]) => {
        spectator = createComponent({
            providers: [{ provide: DynamicDialogConfig, useValue: { data } }]
        });
        spectator.detectChanges();
    };

    beforeEach(() => ref.close.mockClear());

    describe('one container', () => {
        beforeEach(() => open([container('a', [template('1'), template('2', null)])]));

        it('should explain that nothing was deleted and what to do', () => {
            expect(spectator.query(byTestId('in-use-explainer'))).toHaveText(
                'Remove this container from the templates below.'
            );
        });

        it('should label the list "Used by" with the template count, not the container name', () => {
            const label = spectator.query(byTestId('in-use-container-name'));

            expect(label).toHaveText('Used by');
            expect(spectator.query('h3')).toHaveText('2');
        });

        it('should list every template and show the site only when it is readable', () => {
            const items = spectator.queryAll('li');

            expect(items).toHaveLength(2);
            expect(items[0]).toHaveText('Template 1');
            expect(items[0]).toHaveText('demo.dotcms.com');
            expect(items[1]).toHaveText('Template 2');
            expect(items[1].querySelectorAll('span')).toHaveLength(1);
        });
    });

    describe('several containers', () => {
        beforeEach(() =>
            open([container('a', [template('1')]), container('b', [template('2'), template('3')])])
        );

        it('should use the plural explainer', () => {
            expect(spectator.query(byTestId('in-use-explainer'))).toHaveText(
                'Remove each container from its templates.'
            );
        });

        it('should group the templates under the name of the container that uses them', () => {
            const sections = spectator.queryAll(byTestId('in-use-container'));

            expect(sections).toHaveLength(2);
            expect(sections[0].querySelector('h3')).toHaveText('Container a');
            expect(sections[1].querySelector('h3')).toHaveText('Container b');
            expect(sections[1].querySelectorAll('li')).toHaveLength(2);
        });
    });

    it('should render titles as text, not as markup', () => {
        open([container('a', [{ ...template('1'), title: '<b>Home</b> & more' }])]);

        expect(spectator.query(byTestId('in-use-template'))?.textContent).toContain(
            '<b>Home</b> & more'
        );
        expect(spectator.query('li b')).toBeNull();
    });

    it('should close the dialog from the Close button', () => {
        open([container('a', [template('1')])]);

        spectator.click(spectator.query(byTestId('in-use-close'))?.querySelector('button'));

        expect(ref.close).toHaveBeenCalled();
    });
});
