import { describe, expect, test } from '@jest/globals';
import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { ControlContainer, FormGroupDirective } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { AutoComplete } from 'primeng/autocomplete';

import { DotEditContentTagFieldComponent } from './dot-edit-content-tag-field.component';

import { DotEditContentService } from '../../services/dot-edit-content.service';
import { createFormGroupDirectiveMock, TAG_FIELD_MOCK } from '../../utils/mocks';

describe('DotEditContentTagFieldComponent', () => {
    let spectator: Spectator<DotEditContentTagFieldComponent>;
    let autoCompleteElement: Element;
    let autoCompleteComponent: AutoComplete;

    const createComponent = createComponentFactory({
        component: DotEditContentTagFieldComponent,
        componentViewProviders: [
            {
                provide: ControlContainer,
                useValue: createFormGroupDirectiveMock()
            },
            { provide: DotEditContentService, useValue: { getTags: () => of(['tagExample']) } }
        ],
        providers: [FormGroupDirective]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                field: TAG_FIELD_MOCK
            }
        });

        autoCompleteElement = spectator.query(byTestId(TAG_FIELD_MOCK.variable));

        autoCompleteComponent = spectator.debugElement.query(
            By.css(`[data-testId="${TAG_FIELD_MOCK.variable}"]`)
        ).componentInstance;
    });

    test.each([
        {
            variable: `tag-id-${TAG_FIELD_MOCK.variable}`,
            attribute: 'id'
        },
        {
            variable: TAG_FIELD_MOCK.variable,
            attribute: 'ng-reflect-name'
        }
    ])('should have the $variable as $attribute', ({ variable, attribute }) => {
        expect(autoCompleteElement.getAttribute(attribute)).toBe(variable);
    });

    it('should has multiple as true', () => {
        expect(autoCompleteComponent.multiple).toBe(true);
    });

    it('should has unique as true', () => {
        expect(autoCompleteComponent.unique).toBe(true);
    });

    it('should has showClear as true', () => {
        expect(autoCompleteComponent.showClear).toBe(true);
    });

    it('should trigger getTags on search with 3 or more characters', () => {
        const getTagsMock = jest.spyOn(spectator.component, 'getTags');
        const autocompleteArg = {
            query: 'test'
        };
        spectator.triggerEventHandler('p-autocomplete', 'completeMethod', autocompleteArg);
        expect(getTagsMock).toBeCalledWith(autocompleteArg);
        expect(spectator.query(AutoComplete).suggestions).toBeDefined();
    });

    it('should dont have suggestions if search ir less than 3 characters', () => {
        const getTagsMock = jest.spyOn(spectator.component, 'getTags');
        const autocompleteArg = {
            query: 'te'
        };
        spectator.triggerEventHandler('p-autocomplete', 'completeMethod', autocompleteArg);
        expect(getTagsMock).toBeCalledWith(autocompleteArg);
        expect(spectator.query(AutoComplete).suggestions).toBeNull();
    });
});
