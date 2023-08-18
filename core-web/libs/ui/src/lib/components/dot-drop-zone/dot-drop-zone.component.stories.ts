import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { DotDropZoneComponent } from './dot-drop-zone.component';

export default {
    title: 'Library/ui/Components/DropZone',
    component: DotDropZoneComponent,
    decorators: [
        moduleMetadata({
            imports: []
        })
    ]
} as Meta<DotDropZoneComponent>;

const Template: Story<DotDropZoneComponent> = (args: DotDropZoneComponent) => ({
    props: args,
    template: `
        <dot-drop-zone>
            <div style="width: 100%; height: 200px; background: #f2f2f2; display:flex; justify-content:center; align-items:center;">
                Drop files here
            </div>
        </dot-drop-zone>
    `
});

export const DotDropzone = Template.bind({});
