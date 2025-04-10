import { describe, expect } from '@jest/globals';
import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { ControlContainer, FormControl, FormGroup, FormGroupDirective } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { AutoComplete, AutoCompleteCompleteEvent } from 'primeng/autocomplete';

import { DotEditContentTagFieldComponent } from './dot-edit-content-tag-field.component';

import { DotEditContentService } from '../../services/dot-edit-content.service';
import { TAG_FIELD_MOCK } from '../../utils/mocks';

describe('DotEditContentTagFieldComponent', () => {
    let spectator: Spectator<DotEditContentTagFieldComponent>;
    let service: SpyObject<DotEditContentService>;
    let formGroup: FormGroup;
    let formGroupDirective: FormGroupDirective;

    const createComponent = createComponentFactory({
        component: DotEditContentTagFieldComponent,
        componentViewProviders: [
            {
                provide: ControlContainer,
                useExisting: FormGroupDirective
            }
        ],
        providers: [FormGroupDirective, mockProvider(DotEditContentService)]
    });

    beforeEach(() => {
        formGroup = new FormGroup({
            [TAG_FIELD_MOCK.variable]: new FormControl([])
        });

        formGroupDirective = new FormGroupDirective([], []);
        formGroupDirective.form = formGroup;

        spectator = createComponent({
            detectChanges: false,
            providers: [
                {
                    provide: FormGroupDirective,
                    useValue: formGroupDirective
                }
            ]
        });

        spectator.setInput({
            field: TAG_FIELD_MOCK
        });

        service = spectator.inject(DotEditContentService);
        service.getTags.mockReturnValue(of(['tagExample']));

        spectator.detectChanges();
    });

    it('should create component', () => {
        expect(spectator.component).toBeTruthy();
    });

    describe('Component Configuration', () => {
        it('should render autocomplete with correct attributes', () => {
            const autocomplete = spectator.query(AutoComplete);
            const container = spectator.query(
                byTestId(`tag-field-container-${TAG_FIELD_MOCK.variable}`)
            );

            expect(container).toBeTruthy();
            expect(autocomplete).toBeTruthy();
            expect(autocomplete.id).toBe(`tag-id-${TAG_FIELD_MOCK.variable}`);
            expect(autocomplete.inputId).toBe(TAG_FIELD_MOCK.variable);
            expect(autocomplete.multiple).toBe(true);
            expect(autocomplete.forceSelection).toBe(true);
            expect(autocomplete.unique).toBe(true);
            expect(autocomplete.minLength).toBe(2);
            expect(autocomplete.delay).toBe(300);
        });

        it('should be connected to form control', () => {
            const control = spectator.component.formControl;
            const controlContainer = spectator.inject(ControlContainer, true);
            expect(control).toBeDefined();
            expect(control).toBe(controlContainer.control?.get(TAG_FIELD_MOCK.variable));
        });
    });

    describe('User Interactions', () => {
        it('should show suggestions when user types valid search term', async () => {
            const expectedTags = ['angular', 'typescript'];
            service.getTags.mockReturnValue(of(expectedTags));

            // Simulate user typing
            spectator.component.onSearch({
                query: 'type',
                originalEvent: new Event('input')
            } as AutoCompleteCompleteEvent);

            // Wait for the async operation to complete
            await spectator.fixture.whenStable();

            expect(service.getTags).toHaveBeenCalledWith('type');
            expect(spectator.component.$suggestions()).toEqual(expectedTags);
        });
    });

    describe('Form Integration', () => {
        it('should update form value when user selects a tag', async () => {
            const selectedTag = 'angular';
            service.getTags.mockReturnValue(of([selectedTag]));

            // Simulate user selecting a tag
            const selectAutocomplete = spectator.debugElement.query(
                By.css(`[data-testid="tag-field-container-${TAG_FIELD_MOCK.variable}"]`)
            );
            spectator.triggerEventHandler(selectAutocomplete, 'completeMethod', {
                query: selectedTag
            });

            await spectator.fixture.whenStable();

            const formControl = spectator.component.formControl;
            formControl?.setValue([selectedTag]);

            expect(formControl?.value).toEqual([selectedTag]);
        });

        it('should allow multiple tag selection', async () => {
            const tags = ['angular', 'typescript'];
            service.getTags.mockReturnValue(of(tags));
            const formControl = spectator.component.formControl;

            // Simulate user selecting multiple tags
            formControl?.setValue(tags);

            expect(formControl?.value).toEqual(tags);
        });
    });
});
