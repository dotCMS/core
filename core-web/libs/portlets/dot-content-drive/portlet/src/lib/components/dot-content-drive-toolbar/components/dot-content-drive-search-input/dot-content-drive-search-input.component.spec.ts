import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';

import { fakeAsync, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';

import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';

import { DotContentDriveSearchInputComponent } from './dot-content-drive-search-input.component';

import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

describe('DotContentDriveSearchInputComponent', () => {
    let spectator: Spectator<DotContentDriveSearchInputComponent>;
    let mockStore: {
        setFilters: jest.Mock;
        removeFilter: jest.Mock;
        getFilterValue: jest.Mock;
    };

    const createComponent = createComponentFactory({
        component: DotContentDriveSearchInputComponent,
        imports: [ReactiveFormsModule, IconFieldModule, InputIconModule, InputTextModule],
        providers: [
            {
                provide: DotContentDriveStore,
                useFactory: () => ({
                    setFilters: jest.fn(),
                    removeFilter: jest.fn(),
                    getFilterValue: jest.fn()
                })
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        mockStore = spectator.inject(DotContentDriveStore);
        mockStore.getFilterValue.mockReturnValue(undefined);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('Component Initialization', () => {
        it('should create successfully', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should initialize with empty form control by default', () => {
            spectator.detectChanges();

            expect(spectator.component.searchControl.value).toBe('');
        });

        it('should load existing filter value from store on init', () => {
            const existingValue = 'existing search term';
            mockStore.getFilterValue.mockReturnValue(existingValue);

            spectator.detectChanges();

            expect(mockStore.getFilterValue).toHaveBeenCalledWith('title');
            expect(spectator.component.searchControl.value).toBe(existingValue);
        });
    });

    describe('Template', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should render search input element', () => {
            const input = spectator.query('input');
            expect(input).toBeTruthy();
        });

        it('should bind form control to input', () => {
            const input = spectator.query('input') as HTMLInputElement;

            spectator.component.searchControl.setValue('test value');
            spectator.detectChanges();

            expect(input.value).toBe('test value');
        });
    });

    describe('Search Action', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should call setFilters after debounce when input has value', fakeAsync(() => {
            const input = spectator.query('input') as HTMLInputElement;

            spectator.typeInElement('search term', input);
            tick(500);

            expect(mockStore.setFilters).toHaveBeenCalledWith({ title: 'search term' });
        }));

        it('should call removeFilter when input is empty', fakeAsync(() => {
            const input = spectator.query('input') as HTMLInputElement;

            spectator.typeInElement('   ', input);
            tick(500);

            expect(mockStore.removeFilter).toHaveBeenCalledWith('title');
        }));

        it('should debounce input changes by 500ms', fakeAsync(() => {
            const input = spectator.query('input') as HTMLInputElement;

            spectator.typeInElement('test', input);

            expect(mockStore.setFilters).not.toHaveBeenCalled();

            tick(499);
            expect(mockStore.setFilters).not.toHaveBeenCalled();

            tick(1);
            expect(mockStore.setFilters).toHaveBeenCalledWith({ title: 'test' });
        }));

        it('should trim whitespace from input values', fakeAsync(() => {
            const input = spectator.query('input') as HTMLInputElement;

            spectator.typeInElement('  trimmed value  ', input);
            tick(500);

            expect(mockStore.setFilters).toHaveBeenCalledWith({ title: 'trimmed value' });
        }));

        it('should handle special characters correctly', fakeAsync(() => {
            const input = spectator.query('input') as HTMLInputElement;
            const specialChars = 'test-search+term (with) special chars!';

            spectator.typeInElement(specialChars, input);
            tick(500);

            expect(mockStore.setFilters).toHaveBeenCalledWith({ title: specialChars });
        }));
    });

    describe('OnDestroy', () => {
        it('should not call store methods after component is destroyed', fakeAsync(() => {
            spectator.detectChanges();
            const input = spectator.query('input') as HTMLInputElement;

            spectator.typeInElement('test', input);
            spectator.fixture.destroy();
            tick(500);

            expect(mockStore.setFilters).not.toHaveBeenCalled();
        }));
    });
});
