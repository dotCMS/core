import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';

import { of } from 'rxjs';

import { DotAiService, DotMessageService } from '@dotcms/data-access';
import {
    DotAICompletionsConfig,
    DotAIImageOrientation,
    DotGeneratedAIImage,
    PromptType
} from '@dotcms/dotcms-models';

import { AiImagePromptFormComponent } from './ai-image-prompt-form.component';

import { DotMessagePipe } from '../../../../dot-message/dot-message.pipe';
import { DotClipboardUtil } from '../../../../services/clipboard/ClipboardUtil';
import { DotCopyButtonComponent } from '../../../dot-copy-button/dot-copy-button.component';

/** All 4 form controls — used for form.setValue() calls */
const MOCK_FORM_CONTROLS = {
    text: 'Test',
    type: PromptType.INPUT,
    orientation: DotAIImageOrientation.SQUARE,
    size: '1024x1024'
};

/** Shape emitted by valueChange — excludes orientation (internal control) */
const MOCK_FORM_VALUE = {
    text: 'Test',
    type: PromptType.INPUT,
    size: '1024x1024'
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
        // Prevent loadImageModelConfig() from making real HTTP calls in unit tests
        jest.spyOn(spectator.inject(DotAiService), 'getConfig').mockReturnValue(
            of({} as DotAICompletionsConfig)
        );
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
        expect(spectator.component.form.get('orientation').value).toEqual(
            DotAIImageOrientation.SQUARE
        );
        expect(spectator.component.form.get('size').value).toMatch(/^\d+x\d+$/); // Should be a size string like "1024x1024"
    });

    it('should emit value when form value change', () => {
        const emitSpy = jest.spyOn(spectator.component.valueChange, 'emit');
        spectator.component.form.setValue(MOCK_FORM_CONTROLS);

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
        spectator.component.form.setValue(MOCK_FORM_CONTROLS);
        spectator.detectChanges();

        expect(generateButton.disabled).toEqual(false);
    });

    it('should emit generate when the form is submitted', () => {
        const valueSpy = jest.spyOn(spectator.component.generate, 'emit');
        spectator.setInput('isLoading', false);
        spectator.component.form.setValue(MOCK_FORM_CONTROLS);
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

    it('should emit updated size via valueChange when config loads after orientation is already selected', () => {
        // Simulate the race condition: user selects landscape before config loads,
        // then config resolves with a different model that maps to a different size.
        const dotAiService = spectator.inject(DotAiService);
        const emitSpy = jest.spyOn(spectator.component.valueChange, 'emit');

        // Step 1: select landscape orientation while still using dall-e-3 default
        spectator.component.form.get('orientation').setValue(DotAIImageOrientation.LANDSCAPE);
        spectator.detectChanges();
        emitSpy.mockClear();

        // Step 2: config resolves with gpt-image-1 as the current IMAGE model
        jest.spyOn(dotAiService, 'getConfig').mockReturnValue(
            of({
                availableModels: [{ name: 'gpt-image-1', type: 'IMAGE', current: true }]
            } as DotAICompletionsConfig)
        );
        // Trigger loadImageModelConfig manually to simulate async resolution
        (spectator.component as unknown as { loadImageModelConfig: () => void }).loadImageModelConfig();
        spectator.detectChanges();

        // The size should now be gpt-image-1 landscape ('1536x1024'), not dall-e-3 ('1792x1024')
        expect(spectator.component.form.get('size').value).toBe('1536x1024');
        expect(emitSpy).toHaveBeenCalledWith(
            expect.objectContaining({ size: '1536x1024' })
        );
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
