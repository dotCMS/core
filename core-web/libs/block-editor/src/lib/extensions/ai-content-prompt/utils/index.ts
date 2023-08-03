import { Props } from 'tippy.js';

export const TIPPY_OPTIONS: Partial<Props> = {
    duration: [250, 0],
    interactive: true,
    maxWidth: '100%',
    trigger: 'manual',
    hideOnClick: 'toggle',
    popperOptions: {
        modifiers: [
            {
                name: 'flip',
                options: { fallbackPlacements: ['bottom'] }
            },
            {
                name: 'preventOverflow',
                options: {
                    padding: { left: 10, right: 10 },
                    boundary: 'viewport'
                }
            }
        ]
    }
};
