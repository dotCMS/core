import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { ButtonDirective, ButtonModule } from 'primeng/button';
import { Chip, ChipModule } from 'primeng/chip';

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
            detectChanges: false
        });
        spectator.setInput('categories', CATEGORIES_KEY_VALUE);
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
            spectator.setInput('max', 2);
            spectator.component.$showAll.set(true);
            spectator.detectChanges();
            const showBtn = spectator.query(byTestId('show-btn'));
            spectator.click(showBtn);
            expect(spectator.component.$showAll()).toBe(false);
        });

        it('should set showAll to false', () => {
            spectator.setInput('max', 2);
            spectator.component.$showAll.set(false);
            spectator.detectChanges();
            const showBtn = spectator.query(byTestId('show-btn'));
            spectator.click(showBtn);
            expect(spectator.component.$showAll()).toBe(true);
        });
    });

    describe('onRemove', () => {
        it('should call the output', () => {
            const removeSpy = jest.spyOn(spectator.component.remove, 'emit');
            spectator.setInput('max', 2);
            spectator.component.$showAll.set(true);
            spectator.detectChanges();
            const chips = spectator.queryAll(Chip);
            chips[0].onRemove.emit();
            expect(removeSpy).toHaveBeenCalledWith(CATEGORIES_KEY_VALUE[0].key);
        });
    });
});
