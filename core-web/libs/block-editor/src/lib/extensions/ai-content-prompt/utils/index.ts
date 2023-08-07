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
                    boundary: 'viewport'
                }
            }
        ]
    }
};

export const AI_PROMPT_DYNAMIC_CONTROLS = [
    {
        key: 'textPrompt',
        label: '',
        type: 'text',
        required: true
    }
];
