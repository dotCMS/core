import { StringPixels } from './string-pixels-util';
import { TestBed } from '@angular/core/testing';

describe('StringPixelsUtil', () => {
    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [StringPixels]
        });
    });

    it('should return max width --> 140px', () => {
        const textValues = ['demo text', 'demo longer test', 'the longest text of all times'];
        const textWidth = StringPixels.getDropdownWidth(textValues);
        expect(textWidth).toBe('140px');
    });

    it('should return max widht --> 67 (taking 7 as the size of each character)', () => {
        const textValues = ['text', 'demo', 'hello'];
        const textWidth = StringPixels.getDropdownWidth(textValues);
        expect(textWidth).toBe('67px');
    });
});
