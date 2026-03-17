import { createPipeFactory, SpectatorPipe } from '@ngneat/spectator/jest';

import { DotTruncatePathPipe } from './dot-truncate-path.pipe';

describe('DotTruncatePathPipe', () => {
    let spectator: SpectatorPipe<DotTruncatePathPipe>;

    const createPipe = createPipeFactory({
        pipe: DotTruncatePathPipe
    });

    it('should return just the path with root level', () => {
        spectator = createPipe(`{{ 'demo.com' | dotTruncatePath }}`);
        expect(spectator.element).toHaveText('demo.com');
    });

    it('should return just the path with one level', () => {
        spectator = createPipe(`{{ 'demo.com/level1' | dotTruncatePath }}`);
        expect(spectator.element).toHaveText('level1');
    });

    it('should return just the path with one level ending in slash', () => {
        spectator = createPipe(`{{ 'demo.com/level1/' | dotTruncatePath }}`);
        expect(spectator.element).toHaveText('level1');
    });

    it('should return just the path with two levels', () => {
        spectator = createPipe(`{{ 'demo.com/level1/level2' | dotTruncatePath }}`);
        expect(spectator.element).toHaveText('level2');
    });

    it('should return just the path with two levels ending in slash', () => {
        spectator = createPipe(`{{ 'demo.com/level1/level2/' | dotTruncatePath }}`);
        expect(spectator.element).toHaveText('level2');
    });

    it('should return just the path with path starting with slash', () => {
        spectator = createPipe(`{{ '/demo.com/level1/level2' | dotTruncatePath }}`);
        expect(spectator.element).toHaveText('level2');
    });

    it('should return just the path with multiple consecutive slashes', () => {
        spectator = createPipe(`{{ 'demo.com//level1//level2' | dotTruncatePath }}`);
        expect(spectator.element).toHaveText('level2');
    });

    it('should return just the path with three levels', () => {
        spectator = createPipe(`{{ 'demo.com/level1/level2/level3' | dotTruncatePath }}`);
        expect(spectator.element).toHaveText('level3');
    });
});
