import { createComponentFactory, Spectator } from '@openng/spectator/jest';

import { DotDataViewComponent } from './dot-dataview.component';

describe('DotDataViewComponent - Upload File', () => {
    let spectator: Spectator<DotDataViewComponent>;

    const createComponent = createComponentFactory({
        component: DotDataViewComponent,
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                data: [],
                loading: false
            }
        });
        spectator.detectChanges();
    });

    describe('file input', () => {
        it('should render a hidden file input with accept="*" by default', () => {
            const input = spectator.query<HTMLInputElement>('[data-testid="file-input"]');
            expect(input).toBeTruthy();
            expect(input?.type).toBe('file');
            expect(input?.accept).toBe('*');
        });

        it('should set accept="image/*" when accept input is "image/*"', () => {
            spectator.setInput('accept', 'image/*');
            spectator.detectChanges();
            const input = spectator.query<HTMLInputElement>('[data-testid="file-input"]');
            expect(input?.accept).toBe('image/*');
        });

        it('should allow all files when accept input is "*"', () => {
            spectator.setInput('accept', '*');
            spectator.detectChanges();
            const input = spectator.query<HTMLInputElement>('[data-testid="file-input"]');
            expect(input?.accept).toBe('*');
        });
    });

    describe('Upload button', () => {
        it('should render the upload button', () => {
            const btn = spectator.query('[data-testid="upload-btn"]');
            expect(btn).toBeTruthy();
        });

        it('should trigger file input click when upload button is clicked', () => {
            const fileInput = spectator.query<HTMLInputElement>('[data-testid="file-input"]');
            const clickSpy = jest.spyOn(fileInput!, 'click');

            const btn = spectator
                .query<HTMLElement>('[data-testid="upload-btn"]')
                ?.querySelector('button');
            spectator.click(btn!);

            expect(clickSpy).toHaveBeenCalled();
        });
    });

    describe('onFileSelected', () => {
        it('should emit onUploadFile with the selected file', () => {
            const emitSpy = jest.spyOn(spectator.component.onUploadFile, 'emit');
            const mockFile = new File(['content'], 'photo.png', { type: 'image/png' });

            const fileInput = spectator.query<HTMLInputElement>('[data-testid="file-input"]')!;
            Object.defineProperty(fileInput, 'files', {
                value: { 0: mockFile, length: 1 },
                configurable: true
            });

            spectator.triggerEventHandler('[data-testid="file-input"]', 'change', {
                target: fileInput
            });

            expect(emitSpy).toHaveBeenCalledWith(mockFile);
        });

        it('should not emit onUploadFile when no file is selected', () => {
            const emitSpy = jest.spyOn(spectator.component.onUploadFile, 'emit');

            const fileInput = spectator.query<HTMLInputElement>('[data-testid="file-input"]')!;
            Object.defineProperty(fileInput, 'files', {
                value: null,
                configurable: true
            });

            spectator.triggerEventHandler('[data-testid="file-input"]', 'change', {
                target: fileInput
            });

            expect(emitSpy).not.toHaveBeenCalled();
        });

        it('should reset the input value after emitting', () => {
            const mockFile = new File(['content'], 'photo.png', { type: 'image/png' });

            const fileInput = spectator.query<HTMLInputElement>('[data-testid="file-input"]')!;
            Object.defineProperty(fileInput, 'files', {
                value: { 0: mockFile, length: 1 },
                configurable: true
            });

            spectator.triggerEventHandler('[data-testid="file-input"]', 'change', {
                target: fileInput
            });

            expect(fileInput.value).toBe('');
        });
    });

    describe('onRowSelect', () => {
        it('should have the onUploadFile output defined', () => {
            expect(spectator.component.onUploadFile).toBeDefined();
        });
    });
});
