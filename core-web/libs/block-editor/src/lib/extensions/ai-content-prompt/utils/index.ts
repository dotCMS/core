import { Props } from 'tippy.js';

export const TIPPY_OPTIONS: Partial<Props> = {
    duration: [500, 0],
    interactive: true,
    maxWidth: '100%',
    trigger: 'manual',
    hideOnClick: false,
    placement: 'bottom-start',
    popperOptions: {
        modifiers: [
            {
                name: 'animate-flip',
                enabled: false
            },
            {
                name: 'preventOverflow',
                options: {
                    altAxis: true
                }
            }
        ]
    }
};
