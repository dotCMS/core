import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { ButtonModule, ButtonDirective } from 'primeng/button';
import { ChipModule, Chip } from 'primeng/chip';

import { DotMessageService } from '@dotcms/data-access';

import { DotCategoryFieldChipsComponent } from './dot-category-field-chips.component';

import { MAX_CHIPS } from '../../dot-edit-content-category-field.const';
import { CATEGORIES_KEY_VALUE, CATEGORY_MESSAGE_MOCK } from '../../mocks/category-field.mocks';

describe('DotCategoryFieldChipsComponent', () => {
    let spectator: Spectator<DotCategoryFieldChipsComponent>;

    const createComponent = createComponentFactory({
        component: DotCategoryFieldChipsComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: CATEGORY_MESSAGE_MOCK
            }
        ],
        imports: [ChipModule, ButtonModule]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false,
            props: {
                categories: CATEGORIES_KEY_VALUE
            } as unknown as DotCategoryFieldChipsComponent
        });
    });

    it('should be created', () => {
        spectator.detectChanges();
        expect(spectator.component).toBeTruthy();
    });

    it('should the max input be equal to constant by default', () => {
        spectator.detectChanges();
        expect(spectator.component.$max()).toBe(MAX_CHIPS);
    });

    it('should be show a max of categories', () => {
        spectator.setInput('max', 2);
        spectator.detectChanges();
        const chips = spectator.queryAll(Chip);
        expect(chips.length).toBe(2);
    });

    it('should be show all categories', () => {
        spectator.setInput('max', 2);
        spectator.component.$showAll.set(true);
        spectator.detectChanges();
        const chips = spectator.queryAll(Chip);
        expect(chips.length).toBe(CATEGORIES_KEY_VALUE.length);
    });

    it('should be show the more btn with the proper label', () => {
        spectator.setInput('max', 2);
        spectator.detectChanges();
        const showBtn = spectator.query(ButtonDirective);
        const size = spectator.component.$categories().length - spectator.component.$max();
        expect(showBtn.label).toBe(`${size} More`);
    });

    it('should be show the less btn with the proper label', () => {
        spectator.setInput('max', 2);
        spectator.component.$showAll.set(true);
        spectator.detectChanges();
        const showBtn = spectator.query(ButtonDirective);
        expect(showBtn.label).toBe(`Less`);
    });

    it('should not show a btn and the label be null', () => {
        spectator.setInput('max', CATEGORIES_KEY_VALUE.length + 1);
        spectator.detectChanges();
        const showBtn = spectator.query(ButtonDirective);
        const label = spectator.component.$btnLabel();
        expect(showBtn).toBeNull();
        expect(label).toBeNull();
    });

    describe('toogleShowAll', () => {
        it('should set showAll to true', () => {
            spectator.component.$showAll.set(false);
            spectator.component.toogleShowAll();
            expect(spectator.component.$showAll()).toBe(true);
        });

        it('should set showAll to false', () => {
            spectator.component.$showAll.set(true);
            spectator.component.toogleShowAll();
            expect(spectator.component.$showAll()).toBe(false);
        });
    });
});
