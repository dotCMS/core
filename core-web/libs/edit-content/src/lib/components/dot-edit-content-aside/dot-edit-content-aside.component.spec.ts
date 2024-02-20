import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';
import { mockProvider } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';

import { DotMessageService, DotWorkflowService, DotFormatDateService } from '@dotcms/data-access';

import { DotEditContentAsideComponent } from './dot-edit-content-aside.component';

import { CONTENT_FORM_DATA_MOCK } from '../../utils/mocks';

describe('DotEditContentAsideComponent', () => {
    let spectator: Spectator<DotEditContentAsideComponent>;
    const createComponent = createComponentFactory({
        component: DotEditContentAsideComponent,
        detectChanges: false,
        imports: [HttpClientTestingModule],
        providers: [
            mockProvider(ActivatedRoute), // Needed, use RouterLink
            {
                provide: DotMessageService,
                useValue: {
                    get() {
                        return 'Sample';
                    }
                }
            },
            {
                provide: DotFormatDateService,
                useValue: {
                    differenceInCalendarDays: () => 10,
                    format: () => '11/07/2023',
                    getUTC: () => new Date()
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            providers: [
                {
                    provide: DotWorkflowService,
                    useValue: {
                        getWorkflowStatus: () =>
                            of({
                                scheme: { name: 'Test' },
                                step: { name: 'Test' },
                                task: { assignedTo: 'Admin User' }
                            })
                    }
                }
            ]
        });
    });

    it('should render aside information data', () => {
        spectator.setInput('contentLet', CONTENT_FORM_DATA_MOCK.contentlet);
        spectator.setInput('contentType', CONTENT_FORM_DATA_MOCK.contentType.contentType);
        spectator.detectChanges();
        expect(spectator.query(byTestId('modified-by')).textContent.trim()).toBe('Admin User');
        expect(spectator.query(byTestId('last-modified')).textContent.trim()).toBe('11/07/2023');
        expect(spectator.query(byTestId('inode')).textContent.trim()).toBe(
            CONTENT_FORM_DATA_MOCK.contentlet.inode.slice(0, 8)
        );
    });

    it('should not render aside information data', () => {
        const CONTENT_WITHOUT_CONTENTLET = { ...CONTENT_FORM_DATA_MOCK };
        delete CONTENT_WITHOUT_CONTENTLET.contentlet;
        spectator.setInput('contentLet', CONTENT_WITHOUT_CONTENTLET.contentlet);
        spectator.setInput('contentType', CONTENT_WITHOUT_CONTENTLET.contentType.contentType);
        spectator.detectChanges();

        expect(spectator.query(byTestId('modified-by')).textContent).toBe('');
        expect(spectator.query(byTestId('last-modified')).textContent).toBe('');
        expect(spectator.query(byTestId('inode'))).toBeFalsy();
    });

    it('should render aside workflow data', () => {
        spectator.setInput('contentLet', CONTENT_FORM_DATA_MOCK.contentlet);
        spectator.setInput('contentType', CONTENT_FORM_DATA_MOCK.contentType.contentType);
        spectator.detectChanges();

        expect(spectator.component.workflow$).toBeDefined();
        expect(spectator.query(byTestId('workflow-name')).textContent.trim()).toBe('Test');
        expect(spectator.query(byTestId('workflow-step')).textContent.trim()).toBe('Test');
        expect(spectator.query(byTestId('workflow-assigned')).textContent.trim()).toBe(
            'Admin User'
        );
    });

    it('should render New as status when dont have contentlet', () => {
        spectator.setInput('contentLet', null);
        spectator.setInput('contentType', CONTENT_FORM_DATA_MOCK.contentType.contentType);
        spectator.detectChanges();

        expect(spectator.query(byTestId('workflow-step')).textContent).toBe('New');
    });
});
