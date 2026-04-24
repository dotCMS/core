import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { Subject, of } from 'rxjs';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotEditContentSidebarReferencesDialogComponent } from './dot-edit-content-sidebar-references-dialog.component';

import { DotContentReference } from '../../../../../models/dot-edit-content.model';
import { DotEditContentService } from '../../../../../services/dot-edit-content.service';

const MOCK_REFERENCES: DotContentReference[] = [
    {
        page: {
            title: 'Home',
            uri: '/home',
            hostName: 'demo.dotcms.com',
            ownerUserName: 'admin',
            identifier: 'page-1'
        },
        container: { name: 'Main Container', identifier: 'cont-1' },
        personaName: 'Default Visitor'
    }
];

describe('DotEditContentSidebarReferencesDialogComponent', () => {
    let spectator: Spectator<DotEditContentSidebarReferencesDialogComponent>;

    const dialogRefMock = { close: jest.fn() };

    const createComponent = createComponentFactory({
        component: DotEditContentSidebarReferencesDialogComponent,
        providers: [
            {
                provide: DynamicDialogConfig,
                useValue: { data: { identifier: 'test-id' } }
            },
            {
                provide: DynamicDialogRef,
                useValue: dialogRefMock
            },
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({ of: 'of' })
            },
            mockProvider(DotEditContentService, {
                getContentletReferences: jest.fn().mockReturnValue(of(MOCK_REFERENCES))
            })
        ]
    });

    beforeEach(() => jest.clearAllMocks());

    describe('loading state', () => {
        it('should show skeletons while the HTTP call is in-flight', () => {
            const responses$ = new Subject<DotContentReference[]>();

            spectator = createComponent({
                providers: [
                    mockProvider(DotEditContentService, {
                        getContentletReferences: jest.fn().mockReturnValue(responses$)
                    })
                ],
                detectChanges: false
            });
            spectator.detectChanges();

            expect(spectator.queryAll('p-skeleton').length).toBeGreaterThan(0);
            expect(spectator.query(byTestId('references-table'))).toBeFalsy();
        });
    });

    describe('with references', () => {
        beforeEach(() => {
            spectator = createComponent();
        });

        it('should render the references table after data loads', () => {
            expect(spectator.query(byTestId('references-table'))).toBeTruthy();
        });

        it('should close the dialog when the close button is clicked', () => {
            const closeBtn = spectator.query(byTestId('close-btn'))?.querySelector('button');
            spectator.click(closeBtn!);

            expect(dialogRefMock.close).toHaveBeenCalled();
        });
    });

    describe('empty state', () => {
        beforeEach(() => {
            spectator = createComponent({
                providers: [
                    mockProvider(DotEditContentService, {
                        getContentletReferences: jest.fn().mockReturnValue(of([]))
                    })
                ]
            });
        });

        it('should render the table with no rows', () => {
            expect(spectator.query(byTestId('references-table'))).toBeTruthy();
            expect(spectator.component.$references()).toEqual([]);
        });
    });
});
