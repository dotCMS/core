import { TestBed } from '@angular/core/testing';

import { DotClipboardUtil } from './ClipboardUtil';

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
        jest.spyOn(document, 'execCommand').mockReturnValue(true);

        service.copy('hello-world').then((res: boolean) => {
            expect(res).toBe(true);
        });
        expect(document.execCommand).toHaveBeenCalledWith('copy');
    });

    it('should not copy and habdle error', () => {
        jest.spyOn(document, 'execCommand').mockImplementation(() => {
            throw new Error();
        });

        service
            .copy('hello-world')
            .then()
            .catch((res) => {
                expect(res).toBe(undefined);
            });
    });
});
