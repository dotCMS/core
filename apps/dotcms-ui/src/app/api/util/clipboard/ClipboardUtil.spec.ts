import { DotClipboardUtil } from './ClipboardUtil';
import { TestBed } from '@angular/core/testing';

describe('DotClipboardUtil', () => {
    let service: DotClipboardUtil;
    let injector;

    beforeEach(() => {
        injector = TestBed.configureTestingModule({
            providers: [DotClipboardUtil]
        });

        service = injector.get(DotClipboardUtil);
    });

    it('should copy', () => {
        spyOn(document, 'execCommand').and.returnValue(true);

        service.copy('hello-world').then((res: boolean) => {
            expect(res).toBe(true);
        });
        expect(document.execCommand).toHaveBeenCalledWith('copy');
    });

    it('should not copy and habdle error', () => {
        spyOn(document, 'execCommand').and.throwError('failed');

        service
            .copy('hello-world')
            .then(() => {})
            .catch((res) => {
                expect(res).toBe(undefined);
            });
    });
});
