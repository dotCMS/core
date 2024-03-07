import { createComponentFactory, Spectator } from '@ngneat/spectator';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { SimpleChange } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { AiImagePromptFormComponent } from './ai-image-prompt-form.component';

describe('AiImagePromptFormComponent', () => {
    let spectator: Spectator<AiImagePromptFormComponent>;
    let generateButton;
    const formValue = { text: 'Test', type: 'input', size: '1792x1024' };
    const createComponent = createComponentFactory({
        component: AiImagePromptFormComponent,
        imports: [HttpClientTestingModule, ButtonModule, ReactiveFormsModule],
        providers: [DotMessageService],
        mocks: [DotMessagePipe]
    });

    beforeEach(() => {
        spectator = createComponent();
        generateButton = spectator.query('button');
    });

    it('should initialize the form properly', () => {
        expect(spectator.component.form.get('text').value).toEqual('');
        expect(spectator.component.form.get('type').value).toEqual('input');
        expect(spectator.component.form.get('size').value).toEqual('1792x1024');
    });

    it('should emit value on form submission', () => {
        const emitSpy = jest.spyOn(spectator.component.value, 'emit');
        spectator.component.form.setValue(formValue);
        spectator.component.submitForm();
        expect(emitSpy).toHaveBeenCalledWith(formValue);
    });

    it('should emit orientation on size control value change', () => {
        const emitSpy = jest.spyOn(spectator.component.orientation, 'emit');
        spectator.component.form.get('size').setValue('1024x1024');
        expect(emitSpy).toHaveBeenCalledWith('1024x1024');
    });

    it('should clear validators for text control when type is auto', () => {
        spectator.component.form.get('type').setValue('auto');
        expect(spectator.component.form.get('text').validator).toBeNull();
    });

    it('should update form when changes come', () => {
        const newGeneratedValue = {
            request: formValue,
            response: { revised_prompt: 'New Prompt' }
        };
        const changes = {
            generatedValue: new SimpleChange(undefined, newGeneratedValue, false),
            isLoading: new SimpleChange(true, false, true)
        };

        spectator.component.ngOnChanges(changes);

        expect(spectator.component.form.value).toEqual(newGeneratedValue.request);
        expect(spectator.component.aiProcessedPrompt).toBe(
            newGeneratedValue.response.revised_prompt
        );
    });

    it('should disable form controls when isLoading is true', () => {
        spectator.setInput('isLoading', true);
        expect(spectator.query('form').getAttribute('disabled')).toBeDefined();
    });

    it('should enable form controls when isLoading is false', () => {
        spectator.setInput('isLoading', false);
        expect(spectator.query('form').getAttribute('disabled')).toBeNull();
    });

    it('should disable button when form is invalid or isLoading is true', () => {
        spectator.setInput('isLoading', false);
        spectator.component.form.setErrors({ invalid: true });
        spectator.detectChanges();

        expect(generateButton.disabled).toEqual(true);
    });

    it('should enable button when form is valid and isLoading is false', () => {
        spectator.setInput({ isLoading: false });
        spectator.component.form.setValue(formValue);
        spectator.detectChanges();

        expect(generateButton.disabled).toEqual(false);
    });

    it('should call submitForm method on button click', () => {
        const valueSpy = jest.spyOn(spectator.component.value, 'emit');
        spectator.setInput({ isLoading: false });
        spectator.component.form.setValue(formValue);
        spectator.detectChanges();

        spectator.click(generateButton);
        expect(valueSpy).toHaveBeenCalled();
    });
});
