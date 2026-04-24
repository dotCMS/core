import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

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

    const getTitle = () => spectator.query(byTestId('chip-title'))?.textContent?.trim();
    const getValues = () => spectator.query(byTestId('chip-values'))?.textContent?.trim();

    beforeEach(() => {
        spectator = createComponent({ props: { title: 'Type' } });
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    describe('label', () => {
        it('should always render the title', () => {
            expect(getTitle()).toBe('Type');
        });

        it('should not render the values span when there are no selections', () => {
            expect(spectator.query(byTestId('chip-values'))).toBeFalsy();
        });

        it('should render the values span when there is at least one selection', () => {
            spectator.setInput('selections', ['Blog']);
            expect(spectator.query(byTestId('chip-values'))).toBeTruthy();
        });

        it('should render one selection', () => {
            spectator.setInput('selections', ['Blog']);
            expect(getValues()).toBe(': Blog');
        });

        it('should render two selections joined by comma', () => {
            spectator.setInput('selections', ['Blog', 'Activities']);
            expect(getValues()).toBe(': Blog, Activities');
        });

        it.each([
            [['Blog', 'News', 'Events'], ': Blog and 2 more'],
            [['Blog', 'News', 'Events', 'Sports'], ': Blog and 3 more']
        ])(
            'should render first selection and remaining count for %s',
            (selections: string[], expected: string) => {
                spectator.setInput('selections', selections);
                expect(getValues()).toBe(expected);
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
