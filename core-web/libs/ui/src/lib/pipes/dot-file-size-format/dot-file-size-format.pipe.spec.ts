import { createPipeFactory, SpectatorPipe } from '@ngneat/spectator/jest';

import { DotFileSizeFormatPipe } from './dot-file-size-format.pipe';

describe('DotFileSizeFormatPipe', () => {
    let spectator: SpectatorPipe<DotFileSizeFormatPipe>;

    const createPipe = createPipeFactory({
        pipe: DotFileSizeFormatPipe
    });

    it('should return value on Bytes', () => {
        spectator = createPipe(`{{ 512 | dotFileSizeFormat }}`);
        expect(spectator.element).toHaveText('512 Bytes');
    });

    it('should return value on KB', () => {
        spectator = createPipe(`{{ 2048 | dotFileSizeFormat }}`);
        expect(spectator.element).toHaveText('2 KB');
    });

    it('should return value on MB', () => {
        spectator = createPipe(`{{ 2000048 | dotFileSizeFormat }}`);
        expect(spectator.element).toHaveText('1.91 MB');
    });

    it('should return value on GB', () => {
        spectator = createPipe(`{{ 2469606195.2 | dotFileSizeFormat }}`);
        expect(spectator.element).toHaveText('2.30 GB');
    });
});
