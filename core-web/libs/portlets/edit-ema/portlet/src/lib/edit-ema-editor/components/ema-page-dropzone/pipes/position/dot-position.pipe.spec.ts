import { DotPositionPipe } from './dot-position.pipe';

import { PAYLOAD_MOCK } from '../../../../../shared/consts';
import { ContentletArea } from '../../types';

describe('DotPositionPipe', () => {
    let pipe: DotPositionPipe;

    beforeEach(() => {
        pipe = new DotPositionPipe();
    });

    it('should return the position styles', () => {
        const item: ContentletArea = {
            x: 10,
            y: 10,
            width: 10,
            height: 10,
            payload: PAYLOAD_MOCK
        };

        expect(pipe.transform(item)).toEqual({
            position: 'absolute',
            left: '10px',
            top: '10px',
            width: '10px',
            height: '10px'
        });
        expect(pipe).toBeTruthy();
    });
});
