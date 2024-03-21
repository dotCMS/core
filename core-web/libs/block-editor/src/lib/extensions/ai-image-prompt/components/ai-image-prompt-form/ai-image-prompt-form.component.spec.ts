import { createComponentFactory, Spectator } from '@ngneat/spectator';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { AiImagePromptFormComponent } from './ai-image-prompt-form.component';

import {
    DotAIImageOrientation,
    DotGeneratedAIImage
} from '../../../../shared/services/dot-ai/dot-ai.models';
import { PromptType } from '../../ai-image-prompt.models';

describe('AiImagePromptFormComponent', () => {
    let spectator: Spectator<AiImagePromptFormComponent>;
    let generateButton;
    const formValue = {
        text: 'Test',
        type: PromptType.INPUT,
        size: DotAIImageOrientation.HORIZONTAL
    };
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
        expect(spectator.component.form.get('type').value).toEqual(PromptType.INPUT);
        expect(spectator.component.form.get('size').value).toEqual(
            DotAIImageOrientation.HORIZONTAL
        );
    });

    it('should emit value when form value change', () => {
        const emitSpy = jest.spyOn(spectator.component.valueChange, 'emit');
        spectator.component.form.setValue(formValue);

        spectator.detectChanges();

        expect(emitSpy).toHaveBeenCalledWith(formValue);
    });

    it('should clear validators for text control when type is auto', () => {
        spectator.component.form.get('type').setValue('auto');
        expect(spectator.component.form.get('text').validator).toBeNull();
    });

    it('should update form when changes come', () => {
        const newGeneratedValue = {
            request: formValue,
            response: { revised_prompt: 'New Prompt' }
        } as DotGeneratedAIImage;

        spectator.setInput('value', newGeneratedValue);
        spectator.setInput('isLoading', false);

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

    it('should emit generate when the form is submitted', () => {
        const valueSpy = jest.spyOn(spectator.component.generate, 'emit');
        spectator.setInput({ isLoading: false });
        spectator.component.form.setValue(formValue);
        spectator.detectChanges();

        spectator.click(generateButton);
        expect(valueSpy).toHaveBeenCalled();
    });
});
