import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { of, throwError } from 'rxjs';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { FileUpload } from 'primeng/fileupload';

import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotConfigurationBackgroundDialogComponent } from './dot-configuration-background-dialog.component';

import { DotConfigurationAssetService } from '../../services/dot-configuration-asset.service';

describe('DotConfigurationBackgroundDialogComponent', () => {
    let spectator: Spectator<DotConfigurationBackgroundDialogComponent>;

    const createComponent = createComponentFactory({
        component: DotConfigurationBackgroundDialogComponent,
        providers: [
            { provide: DotMessageService, useValue: new MockDotMessageService({}) },
            mockProvider(DynamicDialogRef, { close: vi.fn() }),
            mockProvider(DotConfigurationAssetService, {
                uploadImage: vi.fn().mockReturnValue(of('/dA/new-id/asset/custom.jpg'))
            }),
            mockProvider(DotHttpErrorManagerService, { handle: vi.fn() })
        ],
        detectChanges: false
    });

    const open = (current: string) => {
        spectator = createComponent({
            providers: [{ provide: DynamicDialogConfig, useValue: { data: { current } } }]
        });
        spectator.detectChanges();
    };

    const pressed = () =>
        spectator.queryAll('[aria-pressed="true"]').map((tile) => tile.getAttribute('data-testid'));

    const clickButton = (testId: string) =>
        spectator.click(
            spectator.query(byTestId(testId))?.querySelector('button') as HTMLButtonElement
        );

    afterEach(() => vi.clearAllMocks());

    it('shows the 11 bundled backgrounds and None', () => {
        open('');

        expect(spectator.queryAll(byTestId('configuration-background-tile'))).toHaveLength(11);
        expect(spectator.query(byTestId('configuration-background-none-tile'))).toExist();
    });

    it('highlights the current background', () => {
        open('/html/images/backgrounds/bg-3.jpg');
        const [, , third] = spectator.queryAll(byTestId('configuration-background-tile'));

        expect(third).toHaveAttribute('aria-pressed', 'true');
        expect(pressed()).toHaveLength(1);
    });

    it('highlights None when no background is set', () => {
        open('');

        expect(pressed()).toEqual(['configuration-background-none-tile']);
    });

    it('shows a stored custom background as its own tile', () => {
        open('/dA/old-id/asset/office.jpg');

        expect(spectator.query(byTestId('configuration-background-custom-tile'))).toHaveAttribute(
            'aria-pressed',
            'true'
        );
    });

    it('applies the chosen background', () => {
        open('');
        const [first] = spectator.queryAll(byTestId('configuration-background-tile'));
        spectator.click(first);

        clickButton('configuration-background-apply-btn');

        expect(spectator.inject(DynamicDialogRef).close).toHaveBeenCalledWith(
            '/html/images/backgrounds/bg-1.jpg'
        );
    });

    it('applies None as an empty value', () => {
        open('/html/images/backgrounds/bg-3.jpg');
        spectator.click(byTestId('configuration-background-none-tile'));

        clickButton('configuration-background-apply-btn');

        expect(spectator.inject(DynamicDialogRef).close).toHaveBeenCalledWith('');
    });

    it('closes without a value on cancel', () => {
        open('');

        clickButton('configuration-background-cancel-btn');

        expect(spectator.inject(DynamicDialogRef).close).toHaveBeenCalledWith();
    });

    it('uploads an image and selects it', () => {
        open('');
        const file = new File(['x'], 'custom.jpg', { type: 'image/jpeg' });

        spectator.triggerEventHandler(FileUpload, 'onSelect', {
            originalEvent: new Event('change'),
            files: [file],
            currentFiles: [file]
        });
        spectator.detectChanges();

        expect(spectator.inject(DotConfigurationAssetService).uploadImage).toHaveBeenCalledWith(
            file
        );
        expect(pressed()).toEqual(['configuration-background-custom-tile']);
    });

    it('reports a failed upload and keeps the previous selection', () => {
        open('');
        const error = new Error('upload failed');
        vi.mocked(spectator.inject(DotConfigurationAssetService).uploadImage).mockReturnValueOnce(
            throwError(() => error)
        );
        const file = new File(['x'], 'custom.jpg');

        spectator.triggerEventHandler(FileUpload, 'onSelect', {
            originalEvent: new Event('change'),
            files: [file],
            currentFiles: [file]
        });
        spectator.detectChanges();

        expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalledWith(error);
        expect(pressed()).toEqual(['configuration-background-none-tile']);
    });
});
