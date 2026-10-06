import { toElapsedMessage } from './dot-network.utils';

describe('toElapsedMessage', () => {
    it.each([
        [null, 'network.node.contacted.never', []],
        [0, 'network.time.seconds-ago', ['0']],
        [1, 'network.time.second-ago', []],
        [59, 'network.time.seconds-ago', ['59']],
        [60, 'network.time.minute-ago', []],
        [360, 'network.time.minutes-ago', ['6']],
        [3600, 'network.time.hour-ago', []],
        [7200, 'network.time.hours-ago', ['2']],
        [86400, 'network.time.day-ago', []],
        [3 * 86400, 'network.time.days-ago', ['3']],
        [-5, 'network.time.seconds-ago', ['0']]
    ])('%s seconds → %s', (seconds, key, args) => {
        expect(toElapsedMessage(seconds)).toEqual({ key, args });
    });
});
