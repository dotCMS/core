import { DotClipboardUtil } from './ClipboardUtil';
import { DOTTestBed } from '../../../test/dot-test-bed';

describe('DotClipboardUtil', () => {
    const load = () => {};
    const keyDown = () => {};
    let service: DotClipboardUtil;
    let injector;

    beforeEach(() => {
        injector = DOTTestBed.configureTestingModule({
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
            .then((res: boolean) => {})
            .catch((res) => {
                expect(res).toBe(undefined);
            });
    });
});
