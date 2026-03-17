/* eslint-disable @typescript-eslint/no-explicit-any */
import { TestBed } from '@angular/core/testing';

import { DotClipboardUtil } from './ClipboardUtil';

describe('DotClipboardUtil', () => {
    let service: DotClipboardUtil;
    let originalExecCommand: any;

    beforeEach(() => {
        // Store original execCommand if it exists
        originalExecCommand = document.execCommand;
        // Add execCommand to document if it doesn't exist
        if (!document.execCommand) {
            document.execCommand = jest.fn();
        }

        TestBed.configureTestingModule({
            providers: [DotClipboardUtil]
        });

        service = TestBed.inject(DotClipboardUtil);
    });

    afterEach(() => {
        // Restore original execCommand
        document.execCommand = originalExecCommand;
    });

    it('should copy using modern Clipboard API', async () => {
        const mockClipboard = {
            writeText: jest.fn().mockResolvedValue(undefined)
        };
        Object.defineProperty(navigator, 'clipboard', {
            value: mockClipboard,
            writable: true
        });

        const result = await service.copy('hello-world');
        expect(result).toBe(true);
        expect(navigator.clipboard.writeText).toHaveBeenCalledWith('hello-world');
    });

    it('should use fallback when Clipboard API fails', async () => {
        const mockClipboard = {
            writeText: jest.fn().mockRejectedValue(new Error('Not allowed'))
        };
        Object.defineProperty(navigator, 'clipboard', {
            value: mockClipboard,
            writable: true
        });

        document.execCommand = jest.fn().mockReturnValue(true);

        const result = await service.copy('hello-world');
        expect(result).toBe(true);
        expect(document.execCommand).toHaveBeenCalledWith('copy');
    });

    it('should handle complete failure gracefully', async () => {
        const mockClipboard = {
            writeText: jest.fn().mockRejectedValue(new Error('Not allowed'))
        };
        Object.defineProperty(navigator, 'clipboard', {
            value: mockClipboard,
            writable: true
        });

        document.execCommand = jest.fn().mockImplementation(() => {
            throw new Error('execCommand failed');
        });

        const result = await service.copy('hello-world');
        expect(result).toBe(false);
    });
});
