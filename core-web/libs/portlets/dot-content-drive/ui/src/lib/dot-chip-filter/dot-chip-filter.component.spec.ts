import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotChipFilterComponent } from './dot-chip-filter.component';

describe('DotChipFilterComponent', () => {
    let spectator: Spectator<DotChipFilterComponent>;

    const createComponent = createComponentFactory({
        component: DotChipFilterComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({ and: 'And', more: 'More' })
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({ props: { title: 'Type' } });
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    describe('label', () => {
        it('should show just the title when there are no selections', () => {
            expect(spectator.query('span')?.textContent?.trim()).toBe('Type');
        });

        it('should show title and one selection', () => {
            spectator.setInput('selections', ['Blog']);
            expect(spectator.query('span')?.textContent?.trim()).toBe('Type: Blog');
        });

        it('should show title and two selections joined by comma', () => {
            spectator.setInput('selections', ['Blog', 'Activities']);
            expect(spectator.query('span')?.textContent?.trim()).toBe('Type: Blog, Activities');
        });

        it.each([
            [['Blog', 'News', 'Events'], 'Type: Blog and 2 more...'],
            [['Blog', 'News', 'Events', 'Sports'], 'Type: Blog and 3 more...']
        ])(
            'should show first selection and remaining count for %s',
            (selections: string[], expected: string) => {
                spectator.setInput('selections', selections);
                expect(spectator.query('span')?.textContent?.trim()).toBe(expected);
            }
        );
    });

    describe('active state', () => {
        it('should show chevron-down icon when there are no selections', () => {
            expect(spectator.query('.pi-chevron-down')).toBeTruthy();
            expect(spectator.query('.pi-times')).toBeFalsy();
        });

        it('should show close icon when there are selections', () => {
            spectator.setInput('selections', ['Blog']);
            expect(spectator.query('.pi-times')).toBeTruthy();
            expect(spectator.query('.pi-chevron-down')).toBeFalsy();
        });

        it('should apply the active class to the host when selections are present', () => {
            spectator.setInput('selections', ['Blog']);
            expect(spectator.element).toHaveClass('active');
        });

        it('should not apply the active class to the host when there are no selections', () => {
            expect(spectator.element).not.toHaveClass('active');
        });
    });

    describe('outputs', () => {
        it('should emit clicked on host click', () => {
            const handler = jest.fn();
            spectator.output('clicked').subscribe(handler);
            spectator.click(spectator.element);
            expect(handler).toHaveBeenCalled();
        });

        it('should emit removed when the close icon is clicked', () => {
            spectator.setInput('selections', ['Blog']);
            spectator.detectChanges();

            const handler = jest.fn();
            spectator.output('removed').subscribe(handler);
            spectator.click('.pi-times');
            expect(handler).toHaveBeenCalled();
        });

        it('should not emit clicked when the close icon is clicked', () => {
            spectator.setInput('selections', ['Blog']);
            spectator.detectChanges();

            const clickedHandler = jest.fn();
            spectator.output('clicked').subscribe(clickedHandler);
            spectator.click('.pi-times');
            expect(clickedHandler).not.toHaveBeenCalled();
        });
    });
});
