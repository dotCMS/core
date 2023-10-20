import { Props } from 'tippy.js';

export const TIPPY_OPTIONS: Partial<Props> = {
    duration: [500, 0],
    interactive: true,
    maxWidth: '100%',
    trigger: 'manual',
    hideOnClick: true,
    placement: 'top',
    popperOptions: {
        modifiers: [
            {
                name: 'preventOverflow',
                options: {
                    altAxis: true
                }
            }
        ]
    }
};
