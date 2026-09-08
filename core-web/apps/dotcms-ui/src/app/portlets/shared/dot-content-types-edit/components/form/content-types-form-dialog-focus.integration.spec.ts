import { mockProvider } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ApplicationRef, Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';

import { Dialog, DialogModule } from 'primeng/dialog';

import {
    DotHttpErrorManagerService,
    DotLicenseService,
    DotMessageService,
    DotSiteService,
    DotWorkflowService
} from '@dotcms/data-access';
import { DotCMSBaseTypesContentTypes, DotCMSContentType } from '@dotcms/dotcms-models';
import {
    cleanUpDialog,
    createFakeSite,
    dotcmsContentTypeBasicMock,
    DotWorkflowServiceMock
} from '@dotcms/utils-testing';

import { ContentTypesFormComponent } from './content-types-form.component';
import {
    buildActivatedRouteMock,
    createContentTypesFormMessageServiceMock,
    MockDotLicenseService
} from './content-types-form.testing';

const messageServiceMock = createContentTypesFormMessageServiceMock();

const fakeSite = createFakeSite({ hostname: 'demo.dotcms.com', archived: false });

/** Mirrors the create-mode markup: the real form rendered inside a real PrimeNG dialog. */
@Component({
    selector: 'dot-test-dialog-focus-host',
    template: `
        <p-dialog [visible]="true" [modal]="true" [focusOnShow]="focusOnShow">
            <dot-content-types-form [contentType]="contentType" />
        </p-dialog>
    `,
    imports: [DialogModule, ContentTypesFormComponent]
})
class DialogFocusHostComponent {
    focusOnShow = false;
    contentType: DotCMSContentType = {
        ...dotcmsContentTypeBasicMock,
        baseType: 'CONTENT'
    };
}

const NAME_INPUT_SELECTOR = '[data-testid="content-type-form-name"]';
// PrimeNG renders the focusable input behind p-checkbox's inputId, so target that rather than a
// data-testid on the p-checkbox host, which is not the element that receives focus.
const NEW_EDIT_CONTENT_CHECKBOX_SELECTOR = '#newEditContentLabel';

/**
 * Integration tests: the real ContentTypesFormComponent rendered inside a real PrimeNG p-dialog.
 *
 * These are not unit tests. The behavior under test only exists when both collaborate -- who wins
 * the initial focus between the form and the dialog. Neither of the sibling unit specs can cover
 * it: content-types-form.component.spec.ts renders no dialog, and
 * dot-content-types-edit.component.spec.ts stubs the form away.
 */
describe('ContentTypesFormComponent inside p-dialog - Integration Tests', () => {
    let fixture: ComponentFixture<DialogFocusHostComponent>;
    let originalOffsetParent: PropertyDescriptor | undefined;

    const queryElement = (selector: string): HTMLElement =>
        fixture.debugElement.query(By.css(selector))?.nativeElement ?? null;

    const formComponent = (): ContentTypesFormComponent =>
        fixture.debugElement.query(By.directive(ContentTypesFormComponent))
            .componentInstance as ContentTypesFormComponent;

    /**
     * Renders the dialog and settles focus.
     *
     * jsdom never fires a CSS transition end, so PrimeNG's `onAfterEnter` is invoked directly to
     * reproduce its post-animation focus call, then its pending one-shot timer is flushed. Flushing
     * pending timers rather than advancing a fixed amount keeps the test decoupled from PrimeNG's
     * transition duration.
     *
     * Passing an `id` puts the form in edit mode, the same way production does.
     */
    const openDialogAndSettleFocus = ({
        focusOnShow,
        newContentEditorEnabled,
        baseType = DotCMSBaseTypesContentTypes.CONTENT,
        id = undefined
    }: {
        focusOnShow: boolean;
        newContentEditorEnabled: boolean;
        baseType?: DotCMSBaseTypesContentTypes;
        id?: string;
    }): void => {
        TestBed.configureTestingModule({
            imports: [DialogFocusHostComponent],
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: DotWorkflowService, useClass: DotWorkflowServiceMock },
                { provide: DotLicenseService, useClass: MockDotLicenseService },
                {
                    provide: ActivatedRoute,
                    useValue: buildActivatedRouteMock(newContentEditorEnabled)
                },
                mockProvider(DotSiteService, {
                    getSites: jest.fn().mockReturnValue(
                        of({
                            sites: [fakeSite],
                            pagination: { currentPage: 1, perPage: 40, totalEntries: 1 }
                        })
                    ),
                    getSiteById: jest.fn().mockReturnValue(of(fakeSite))
                }),
                mockProvider(DotHttpErrorManagerService)
            ]
        });

        fixture = TestBed.createComponent(DialogFocusHostComponent);
        fixture.componentInstance.focusOnShow = focusOnShow;
        fixture.componentInstance.contentType = {
            ...dotcmsContentTypeBasicMock,
            baseType,
            // Empty when the dialog opens for a content type that has not been saved.
            id: id ?? ''
        };
        fixture.detectChanges();
        // The form focuses the Name input from afterNextRender, and those hooks run on the
        // application tick rather than on detectChanges.
        TestBed.inject(ApplicationRef).tick();

        const dialog: Dialog = fixture.debugElement.query(By.directive(Dialog)).componentInstance;
        dialog.onAfterEnter();

        jest.runOnlyPendingTimers();
    };

    beforeAll(() => {
        // PrimeNG only focuses elements it considers visible (DomHandler.isVisible checks
        // offsetParent). jsdom has no layout engine, so without this shim every element looks
        // hidden, PrimeNG's focus() becomes a no-op and the focus race under test never happens.
        originalOffsetParent = Object.getOwnPropertyDescriptor(
            HTMLElement.prototype,
            'offsetParent'
        );
        Object.defineProperty(HTMLElement.prototype, 'offsetParent', {
            configurable: true,
            get(): Element | null {
                return this.parentElement;
            }
        });
    });

    afterAll(() => {
        Object.defineProperty(HTMLElement.prototype, 'offsetParent', originalOffsetParent!);
    });

    beforeEach(() => {
        jest.useFakeTimers();
    });

    afterEach(() => {
        jest.useRealTimers();
        cleanUpDialog(fixture);
    });

    describe('create mode', () => {
        // The binding does not branch on baseType, but every base type reaches this same dialog
        // through create/:type — so the focus outcome is asserted for real, not just inferred.
        it.each<DotCMSBaseTypesContentTypes>([
            DotCMSBaseTypesContentTypes.CONTENT,
            DotCMSBaseTypesContentTypes.WIDGET
        ])(
            'should focus the name input instead of the new content banner checkbox for %s',
            (baseType) => {
                openDialogAndSettleFocus({
                    focusOnShow: false,
                    newContentEditorEnabled: true,
                    baseType
                });

                const nameInput = queryElement(NAME_INPUT_SELECTOR);
                const newEditContentCheckbox = queryElement(NEW_EDIT_CONTENT_CHECKBOX_SELECTOR);

                expect(newEditContentCheckbox).not.toBeNull();
                expect(document.activeElement).toBe(nameInput);
                expect(document.activeElement).not.toBe(newEditContentCheckbox);
            }
        );

        it('should focus the name input when the new content banner is hidden', () => {
            openDialogAndSettleFocus({ focusOnShow: false, newContentEditorEnabled: false });

            expect(queryElement(NEW_EDIT_CONTENT_CHECKBOX_SELECTOR)).toBeNull();
            expect(document.activeElement).toBe(queryElement(NAME_INPUT_SELECTOR));
        });

        it('should let the focused input update the name control without touching the banner', () => {
            openDialogAndSettleFocus({ focusOnShow: false, newContentEditorEnabled: true });

            const form = formComponent().form;
            const newEditContentBefore = form.get('newEditContent')!.value;
            const nameInput = document.activeElement as HTMLInputElement;

            nameInput.value = 'My Content Type';
            nameInput.dispatchEvent(new Event('input'));

            expect(form.get('name')!.value).toBe('My Content Type');
            expect(form.get('newEditContent')!.value).toBe(newEditContentBefore);
        });
    });

    describe('edit mode', () => {
        it('should not focus anything when the content type already exists', () => {
            openDialogAndSettleFocus({
                focusOnShow: false,
                newContentEditorEnabled: true,
                id: '1234-5678-edit'
            });

            // Nothing should grab focus while editing: the name is already filled in.
            expect(document.activeElement).toBe(document.body);
            expect(document.activeElement).not.toBe(queryElement(NAME_INPUT_SELECTOR));
            expect(document.activeElement).not.toBe(
                queryElement(NEW_EDIT_CONTENT_CHECKBOX_SELECTOR)
            );
        });

        it('should not focus anything when the new content banner is hidden', () => {
            openDialogAndSettleFocus({
                focusOnShow: false,
                newContentEditorEnabled: false,
                id: '1234-5678-edit'
            });

            expect(document.activeElement).toBe(document.body);
        });
    });

    describe('focusOnShow enabled (reproduces the bug being fixed)', () => {
        it('should let PrimeNG steal focus to the first focusable element in DOM order', () => {
            // Not a production configuration -- this reproduces the defect so the reason the
            // binding must stay disabled is pinned in a test. Leaving focusOnShow on lets PrimeNG's
            // post-transition focus() win over the form's own focus, landing on the banner
            // checkbox. This was the visible "focus appears on Name, then jumps away" symptom.
            openDialogAndSettleFocus({ focusOnShow: true, newContentEditorEnabled: true });

            expect(document.activeElement).toBe(queryElement(NEW_EDIT_CONTENT_CHECKBOX_SELECTOR));
            expect(document.activeElement).not.toBe(queryElement(NAME_INPUT_SELECTOR));
        });
    });
});
