import { createComponentFactory } from '@openng/spectator/vitest';

import {
    DotUsersDialogMaximizeIconComponent,
    DotUsersDialogMinimizeIconComponent
} from './dot-users-dialog-fullscreen-icon.component';

describe('DotUsersDialogFullscreenIcon', () => {
    const createMaximize = createComponentFactory(DotUsersDialogMaximizeIconComponent);
    const createMinimize = createComponentFactory(DotUsersDialogMinimizeIconComponent);

    it('should render open_in_full as a Material Symbol while windowed', () => {
        const icon = createMaximize().query('i');

        expect(icon?.textContent?.trim()).toBe('open_in_full');
        expect(icon?.classList).toContain('material-symbols-outlined');
        expect(icon?.getAttribute('aria-hidden')).toBe('true');
    });

    it('should render close_fullscreen as a Material Symbol while full screen', () => {
        const icon = createMinimize().query('i');

        expect(icon?.textContent?.trim()).toBe('close_fullscreen');
        expect(icon?.classList).toContain('material-symbols-outlined');
    });
});
