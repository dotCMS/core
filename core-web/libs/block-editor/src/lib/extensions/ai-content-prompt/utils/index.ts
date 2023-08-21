import { Props } from 'tippy.js';

export const TIPPY_OPTIONS: Partial<Props> = {
    duration: [500, 0],
    interactive: true,
    maxWidth: '100%',
    trigger: 'manual',
    hideOnClick: 'toggle',
    placement: 'top',
    popperOptions: {
        modifiers: [
            {
                name: 'flip',
                options: { fallbackPlacements: ['bottom', 'right', 'left'] }
            }
        ]
    },
    onShow: (instance) => {
        (instance.popper as HTMLElement).style.width = '100%';
    }
};
