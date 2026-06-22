import { getStoredPanelState, savePanelState } from './panel-state.storage';

import { IMAGE_EDITOR_PANEL_STATE_KEY } from '../image-editor.constants';

describe('panel-state.storage', () => {
    afterEach(() => {
        localStorage.clear();
        jest.restoreAllMocks();
    });

    describe('getStoredPanelState', () => {
        it('returns an empty array (all collapsed) when nothing is stored', () => {
            expect(getStoredPanelState()).toEqual([]);
        });

        it('returns the persisted open sections', () => {
            localStorage.setItem(
                IMAGE_EDITOR_PANEL_STATE_KEY,
                JSON.stringify(['adjust', 'history'])
            );

            expect(getStoredPanelState()).toEqual(['adjust', 'history']);
        });

        it('falls back to the default when the stored value is corrupt', () => {
            localStorage.setItem(IMAGE_EDITOR_PANEL_STATE_KEY, '{ not json');

            expect(getStoredPanelState()).toEqual([]);
        });

        it('falls back to the default when the stored value is not an array of strings', () => {
            localStorage.setItem(IMAGE_EDITOR_PANEL_STATE_KEY, JSON.stringify({ adjust: true }));

            expect(getStoredPanelState()).toEqual([]);
        });
    });

    describe('savePanelState', () => {
        it('persists the open sections as JSON', () => {
            savePanelState(['transform']);

            expect(localStorage.getItem(IMAGE_EDITOR_PANEL_STATE_KEY)).toBe(
                JSON.stringify(['transform'])
            );
        });

        it('round-trips through getStoredPanelState', () => {
            savePanelState(['adjust', 'fileinfo']);

            expect(getStoredPanelState()).toEqual(['adjust', 'fileinfo']);
        });

        it('does not throw when storage is unavailable', () => {
            jest.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
                throw new Error('QuotaExceededError');
            });

            expect(() => savePanelState(['adjust'])).not.toThrow();
        });
    });
});
