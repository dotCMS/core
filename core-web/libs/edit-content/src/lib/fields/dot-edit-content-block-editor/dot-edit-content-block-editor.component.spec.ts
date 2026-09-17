import { SpectatorHost, createHostFactory, mockProvider } from '@openng/spectator/vitest';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { DotPropertiesService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotCMSEditorComponent } from '@dotcms/new-block-editor';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { DotEditContentBlockEditorComponent } from './dot-edit-content-block-editor.component';

import { DotEditContentStore } from '../../store/edit-content.store';

@Component({
    standalone: false,
    selector: 'dot-custom-host',
    template: ''
})
export class MockFormComponent {
    // Host Props
    formGroup: FormGroup;
    field: DotCMSContentTypeField;
    contentlet: DotCMSContentlet;
}

const BLOCK_EDITOR_FIELD_MOCK: DotCMSContentTypeField = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableStoryBlockField',
    contentTypeId: 'test-content-type',
    dataType: 'LONG_TEXT',
    defaultValue: '',
    fieldType: 'STORY_BLOCK',
    fieldTypeLabel: 'Story Block',
    fieldVariables: [],
    fixed: false,
    hint: '',
    iDate: Date.now(),
    id: 'test-field-id',
    indexed: false,
    listed: false,
    modDate: Date.now(),
    name: 'Story Block Field',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 1,
    unique: false,
    variable: 'storyBlock'
};

describe('DotEditContentBlockEditorComponent', () => {
    let spectator: SpectatorHost<DotEditContentBlockEditorComponent, MockFormComponent>;

    const createHost = createHostFactory({
        component: DotEditContentBlockEditorComponent,
        host: MockFormComponent,
        imports: [ReactiveFormsModule],
        overrideComponents: [
            [
                DotEditContentBlockEditorComponent,
                {
                    remove: { imports: [DotCMSEditorComponent] },
                    add: { imports: [MockComponent(DotCMSEditorComponent)] }
                }
            ]
        ],
        providers: [
            {
                provide: DotEditContentStore,
                useValue: {
                    hasAttemptedSubmit: signal(false),
                    currentLocale: signal({ id: 2, language: 'Spanish', country: 'Spain' })
                }
            },
            mockProvider(DotPropertiesService, {
                getFeatureFlag: vi.fn().mockReturnValue(of(true))
            }),
            // The real DotMessageService reaches for /api/v2/languages/default/keys as
            // soon as something injects it, and in jsdom that XHR fails with status 0.
            // The testing backend parks the request instead: nothing asserts on it, it
            // just must not become an unhandled HttpErrorResponse (five of them here).
            provideHttpClient(),
            provideHttpClientTesting()
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createHost(
            `<form [formGroup]="formGroup">
                <dot-edit-content-block-editor [field]="field" [contentlet]="contentlet" />
            </form>`,
            {
                hostProps: {
                    formGroup: new FormGroup({
                        [BLOCK_EDITOR_FIELD_MOCK.variable]: new FormControl('')
                    }),
                    field: BLOCK_EDITOR_FIELD_MOCK,
                    contentlet: createFakeContentlet({
                        [BLOCK_EDITOR_FIELD_MOCK.variable]: ''
                    })
                },
                providers: [
                    {
                        provide: DotEditContentStore,
                        useValue: {
                            hasAttemptedSubmit: signal(false),
                            currentLocale: signal({ id: 2, language: 'Spanish', country: 'Spain' })
                        }
                    }
                ]
            }
        );
    });

    it('should pass the correct languageId to dot-block-editor', () => {
        spectator.detectChanges();

        const blockEditorElement = spectator.query('dot-block-editor');
        expect(blockEditorElement).toBeTruthy();

        // Access the component instance to verify the languageId property
        const blockEditorDebugElement = spectator.debugElement.query(By.css('dot-block-editor'));
        const blockEditorComponent = blockEditorDebugElement?.componentInstance as Record<
            string,
            unknown
        >;
        expect(blockEditorComponent.languageId).toBe(2);
    });

    it('should pass the correct field to dot-block-editor', () => {
        spectator.detectChanges();

        const blockEditorDebugElement = spectator.debugElement.query(By.css('dot-block-editor'));
        const blockEditorComponent = blockEditorDebugElement?.componentInstance as Record<
            string,
            unknown
        >;
        expect(blockEditorComponent.field).toEqual(BLOCK_EDITOR_FIELD_MOCK);
    });

    it('should pass the correct contentlet to dot-block-editor', () => {
        spectator.detectChanges();

        const blockEditorDebugElement = spectator.debugElement.query(By.css('dot-block-editor'));
        const blockEditorComponent = blockEditorDebugElement?.componentInstance as Record<
            string,
            unknown
        >;
        const contentlet = blockEditorComponent.contentlet as DotCMSContentlet;
        expect(contentlet).toBeTruthy();
        expect(contentlet[BLOCK_EDITOR_FIELD_MOCK.variable]).toBe('');
    });

    it('should pass hasFieldError to dot-block-editor', () => {
        spectator.detectChanges();

        const blockEditorDebugElement = spectator.debugElement.query(By.css('dot-block-editor'));
        const blockEditorComponent = blockEditorDebugElement?.componentInstance as Record<
            string,
            unknown
        >;
        // Initially should be false (no errors)
        expect(blockEditorComponent.hasError).toBe(false);
    });

    it('should use formControlName from field variable', () => {
        spectator.detectChanges();

        const blockEditorElement = spectator.query('dot-block-editor');
        expect(blockEditorElement).toBeTruthy();

        const formGroup = spectator.hostComponent.formGroup;
        expect(formGroup.get(BLOCK_EDITOR_FIELD_MOCK.variable)).toBeTruthy();
    });
});

/**
 * T-02 — the required asterisk on a Block Editor (AC-108).
 *
 * This field is the trap the whole `dotFieldRequired` decision turns on. Every other required
 * field carries `Validators.required`, so the directive's `checkIsRequiredControl` mode would
 * find it. A required Block Editor does NOT: `getFieldValidators` pushes
 * `blockEditorRequiredValidator()` instead (dot-edit-content-form.component.ts:773), so that mode
 * reads no `Validators.required`, removes the class, and the asterisk silently disappears.
 *
 * The source of truth is the content type's `field.required`, surfaced by BaseWrapperField's
 * `isRequired` getter. Hence BARE mode. This test is what stops a future refactor from "tidying"
 * the directive into checkIsRequiredControl.
 */
describe('DotEditContentBlockEditorComponent — required indicator', () => {
    let spectator: SpectatorHost<DotEditContentBlockEditorComponent, MockFormComponent>;

    const createHost = createHostFactory({
        component: DotEditContentBlockEditorComponent,
        host: MockFormComponent,
        imports: [ReactiveFormsModule],
        overrideComponents: [
            [
                DotEditContentBlockEditorComponent,
                {
                    remove: { imports: [DotCMSEditorComponent] },
                    add: { imports: [MockComponent(DotCMSEditorComponent)] }
                }
            ]
        ],
        providers: [
            {
                provide: DotEditContentStore,
                useValue: {
                    hasAttemptedSubmit: signal(false),
                    currentLocale: signal({ id: 2, language: 'Spanish', country: 'Spain' })
                }
            },
            mockProvider(DotPropertiesService, {
                getFeatureFlag: vi.fn().mockReturnValue(of(true))
            }),
            provideHttpClient(),
            provideHttpClientTesting()
        ],
        detectChanges: false
    });

    const renderWith = (field: DotCMSContentTypeField) => {
        spectator = createHost(
            `<form [formGroup]="formGroup">
                <dot-edit-content-block-editor [field]="field" [contentlet]="contentlet" />
            </form>`,
            {
                hostProps: {
                    formGroup: new FormGroup({ [field.variable]: new FormControl('') }),
                    field,
                    contentlet: createFakeContentlet({ [field.variable]: '' })
                }
            }
        );
        spectator.detectChanges();
    };

    it('should render the asterisk when the content type marks the field required', () => {
        renderWith({ ...BLOCK_EDITOR_FIELD_MOCK, required: true });

        const label = spectator.query('label');

        expect(label).toBeTruthy();
        expect(label.classList.contains('p-label-input-required')).toBe(true);
    });

    it('should NOT render the asterisk when the field is not required', () => {
        renderWith({ ...BLOCK_EDITOR_FIELD_MOCK, required: false });

        expect(spectator.query('label').classList.contains('p-label-input-required')).toBe(false);
    });

    /**
     * T-09 / AC-204 — the hint is plain text below the control, never a tooltip.
     *
     * Block Editor is the LAST caller of `dot-card-field-label`'s `hint` input, which rendered a
     * `pi pi-info-circle` next to the label and put the text behind a hover. A hint that has to be
     * discovered is not a hint.
     */
    it('should render the hint as text below the control, not as a tooltip icon', () => {
        renderWith({ ...BLOCK_EDITOR_FIELD_MOCK, hint: 'Tell the story, not the summary' });

        expect(spectator.query('i.pi-info-circle')).toBeNull();
        expect(spectator.query('.p-field-hint')?.textContent.trim()).toBe(
            'Tell the story, not the summary'
        );
    });

    it('should render no hint element at all when the field has no hint (AC-205)', () => {
        renderWith({ ...BLOCK_EDITOR_FIELD_MOCK, hint: '' });

        expect(spectator.query('.p-field-hint')).toBeNull();
    });
});
