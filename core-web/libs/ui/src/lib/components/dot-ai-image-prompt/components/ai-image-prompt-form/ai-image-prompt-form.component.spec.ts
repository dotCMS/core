import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';

import { DotMessageService } from '@dotcms/data-access';
import { DotAIImageOrientation, DotGeneratedAIImage, PromptType } from '@dotcms/dotcms-models';

import { AiImagePromptFormComponent } from './ai-image-prompt-form.component';

import { DotMessagePipe } from '../../../../dot-message/dot-message.pipe';
import { DotClipboardUtil } from '../../../../services/clipboard/ClipboardUtil';
import { DotCopyButtonComponent } from '../../../dot-copy-button/dot-copy-button.component';

const MOCK_FORM_VALUE = {
    text: 'Test',
    type: PromptType.INPUT,
    size: DotAIImageOrientation.HORIZONTAL
};

const MOCK_AI_VALUE = {
    request: { ...MOCK_FORM_VALUE },
    response: { revised_prompt: 'New Prompt' }
} as DotGeneratedAIImage;

describe('DotAiImagePromptFormComponent', () => {
    let spectator: Spectator<AiImagePromptFormComponent>;
    let generateButton;

    const createComponent = createComponentFactory({
        component: AiImagePromptFormComponent,
        imports: [
            HttpClientTestingModule,
            ButtonModule,
            ReactiveFormsModule,
            DotCopyButtonComponent
        ],
        providers: [DotMessageService, DotClipboardUtil],
        mocks: [DotMessagePipe]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                isLoading: true,
                hasEditorContent: true,
                value: { ...MOCK_AI_VALUE }
            }
        });
        generateButton = spectator.query('button');
    });

    it('should create the component', () => {
        spectator.detectChanges();
        expect(spectator.component).toBeTruthy();
    });

    it('should initialize the form properly', () => {
        spectator.detectChanges();
        expect(spectator.component.form.get('text').value).toEqual('');
        expect(spectator.component.form.get('type').value).toEqual(PromptType.INPUT);
        expect(spectator.component.form.get('size').value).toEqual(
            DotAIImageOrientation.HORIZONTAL
        );
    });

    it('should emit value when form value change', () => {
        const emitSpy = jest.spyOn(spectator.component.valueChange, 'emit');
        spectator.component.form.setValue(MOCK_FORM_VALUE);

        spectator.detectChanges();

        expect(emitSpy).toHaveBeenCalledWith(MOCK_FORM_VALUE);
    });

    it('should clear validators for text control when type is auto', () => {
        spectator.component.form.get('type').setValue('auto');
        expect(spectator.component.form.get('text').validator).toBeNull();
    });

    it('should disable form controls when isLoading is true', () => {
        spectator.setInput('isLoading', true);
        spectator.detectChanges();
        expect(spectator.query('form').getAttribute('disabled')).toBeDefined();
    });

    it('should enable form controls when isLoading is false', () => {
        spectator.setInput('$isLoading', false);
        spectator.detectChanges();
        expect(spectator.query('form').getAttribute('disabled')).toBeNull();
    });

    it('should disable button when form is invalid or isLoading is true', () => {
        spectator.setInput('$isLoading', false);
        spectator.component.form.setErrors({ invalid: true });
        spectator.detectChanges();

        expect(generateButton.disabled).toEqual(true);
    });

    it('should enable button when form is valid and isLoading is false', () => {
        spectator.setInput('isLoading', false);
        spectator.component.form.setValue(MOCK_FORM_VALUE);
        spectator.detectChanges();

        expect(generateButton.disabled).toEqual(false);
    });

    it('should emit generate when the form is submitted', () => {
        const valueSpy = jest.spyOn(spectator.component.generate, 'emit');
        spectator.setInput('isLoading', false);
        spectator.component.form.setValue(MOCK_FORM_VALUE);
        spectator.detectChanges();

        spectator.click(generateButton);
        expect(valueSpy).toHaveBeenCalled();
    });

    it('should make the prompt label as required in the UI', () => {
        const REQUIRED_CLASS = 'p-label-input-required';
        spectator.setInput('value', { ...MOCK_AI_VALUE });
        spectator.setInput('isLoading', false);
        spectator.detectChanges();

        expect(spectator.query(byTestId('prompt-label')).classList).toContain(REQUIRED_CLASS);

        spectator.setInput('value', {
            request: { ...MOCK_FORM_VALUE, type: PromptType.AUTO },
            response: { revised_prompt: 'New Prompt' }
        } as DotGeneratedAIImage);
        spectator.detectChanges();

        expect(spectator.query(byTestId('prompt-label')).classList).not.toContain(REQUIRED_CLASS);
    });

    it('should not show the AI option when hasEditorContent is false', () => {
        spectator.setInput('hasEditorContent', false);
        spectator.detectChanges();

        const aiOption = spectator.query(byTestId('ai-existing-content'));

        expect(aiOption).toBeFalsy();
    });

    it('should show the AI option when hasEditorContent is true', () => {
        spectator.setInput('hasEditorContent', true);
        spectator.detectChanges();

        const aiOption = spectator.query(byTestId('ai-existing-content'));

        expect(aiOption).toBeTruthy();
    });
});
