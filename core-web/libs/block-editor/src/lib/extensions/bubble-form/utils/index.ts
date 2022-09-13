import { DynamicControl } from '../model';

export const imageFormControls: DynamicControl<string>[] = [
    {
        key: 'src',
        label: 'path',
        required: true,
        controlType: 'text',
        type: 'text'
    },
    {
        key: 'alt',
        label: 'alt',
        controlType: 'text',
        type: 'text'
    },
    {
        key: 'title',
        label: 'caption',
        controlType: 'text',
        type: 'text'
    }
];
