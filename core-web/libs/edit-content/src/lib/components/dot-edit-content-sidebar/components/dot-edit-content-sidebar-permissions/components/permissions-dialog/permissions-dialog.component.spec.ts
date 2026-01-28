import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DynamicDialogConfig } from 'primeng/dynamicdialog';

import {
    DotPermissionsDialogComponent,
    PERMISSIONS_IFRAME_PATH,
    PermissionsDialogData
} from './permissions-dialog.component';

describe('DotPermissionsDialogComponent', () => {
    let spectator: Spectator<DotPermissionsDialogComponent>;

    const defaultData: PermissionsDialogData = {
        identifier: 'contentlet-123',
        languageId: 1
    };

    const configRef: { data: PermissionsDialogData | null | undefined } = { data: defaultData };

    const createComponent = createComponentFactory({
        component: DotPermissionsDialogComponent,
        providers: [
            {
                provide: DynamicDialogConfig,
                useValue: configRef
            }
        ]
    });

    beforeEach(() => {
        configRef.data = defaultData;
        spectator = createComponent();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    describe('Elements by data-testId - Success', () => {
        it('should render permissions-iframe when identifier and languageId are valid', () => {
            expect(spectator.query(byTestId('permissions-iframe'))).toBeTruthy();
        });

        it('should NOT render permissions-empty when data is valid', () => {
            expect(spectator.query(byTestId('permissions-empty'))).toBeFalsy();
        });

        it('should set iframe src to permissions JSP with query params when data is valid', () => {
            const iframe = spectator.query(byTestId('permissions-iframe')) as HTMLIFrameElement;
            expect(iframe).toBeTruthy();
            expect(iframe.src).toContain(PERMISSIONS_IFRAME_PATH);
            expect(iframe.src).toContain('contentletId=contentlet-123');
            expect(iframe.src).toContain('languageId=1');
            expect(iframe.src).toContain('popup=true');
            expect(iframe.src).toContain('in_frame=true');
            expect(iframe.src).toContain('frame=detailFrame');
            expect(iframe.src).toContain('container=true');
            expect(iframe.src).toContain('angularCurrentPortlet=edit-content');
        });

        it('should have iframe with title "Content permissions" and min-height 60vh', () => {
            const iframe = spectator.query(byTestId('permissions-iframe')) as HTMLIFrameElement;
            expect(iframe?.getAttribute('title')).toBe('Content permissions');
            expect(iframe?.style.minHeight).toBe('60vh');
        });
    });

    describe('Elements by data-testId - Failure and Edge Cases', () => {
        it('should render permissions-empty when data is undefined', () => {
            configRef.data = undefined;
            spectator = createComponent();
            spectator.detectChanges();

            expect(spectator.query(byTestId('permissions-empty'))).toBeTruthy();
            expect(spectator.query(byTestId('permissions-empty'))?.textContent?.trim()).toContain(
                'No content selected'
            );
            expect(spectator.query(byTestId('permissions-iframe'))).toBeFalsy();
        });

        it('should render permissions-empty when data is null', () => {
            configRef.data = null;
            spectator = createComponent();
            spectator.detectChanges();

            expect(spectator.query(byTestId('permissions-empty'))).toBeTruthy();
            expect(spectator.query(byTestId('permissions-iframe'))).toBeFalsy();
        });

        it('should render permissions-empty when identifier is empty string', () => {
            configRef.data = { identifier: '', languageId: 1 };
            spectator = createComponent();
            spectator.detectChanges();

            expect(spectator.query(byTestId('permissions-empty'))).toBeTruthy();
            expect(spectator.query(byTestId('permissions-iframe'))).toBeFalsy();
        });

        it('should render permissions-empty when languageId is 0', () => {
            configRef.data = { identifier: 'id-ok', languageId: 0 };
            spectator = createComponent();
            spectator.detectChanges();

            expect(spectator.query(byTestId('permissions-empty'))).toBeTruthy();
            expect(spectator.query(byTestId('permissions-iframe'))).toBeFalsy();
        });

        it('should render permissions-empty when data is empty object', () => {
            configRef.data = {} as PermissionsDialogData;
            spectator = createComponent();
            spectator.detectChanges();

            expect(spectator.query(byTestId('permissions-empty'))).toBeTruthy();
            expect(spectator.query(byTestId('permissions-iframe'))).toBeFalsy();
        });
    });

    describe('iframeSrc computed - Edge Cases', () => {
        it('should include correct languageId in URL for different values', () => {
            configRef.data = { identifier: 'x', languageId: 99 };
            spectator = createComponent();
            spectator.detectChanges();

            const iframe = spectator.query(byTestId('permissions-iframe')) as HTMLIFrameElement;
            expect(iframe?.src).toContain('languageId=99');
        });

        it('should include correct identifier in URL for special characters', () => {
            configRef.data = { identifier: 'abc-xyz-789', languageId: 2 };
            spectator = createComponent();
            spectator.detectChanges();

            const iframe = spectator.query(byTestId('permissions-iframe')) as HTMLIFrameElement;
            expect(iframe?.src).toContain('contentletId=abc-xyz-789');
        });
    });
});
