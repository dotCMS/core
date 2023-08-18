import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { CommonModule } from '@angular/common';

import { DotDropZoneComponent } from './dot-drop-zone.component';

export default {
    title: 'Library/ui/Components/DropZone',
    component: DotDropZoneComponent,
    decorators: [
        moduleMetadata({
            imports: [CommonModule]
        })
    ]
} as Meta<DotDropZoneComponent>;

const Template: Story<DotDropZoneComponent> = (args: DotDropZoneComponent) => ({
    props: args,

    styles: [
        `
        .drop-zone-active .content {
            background-color: #f2f2f2;
        }

        .drop-zone-error .content {
            background-color: rgba(230, 57, 70, 0.2);
        }

        .content {
            width: 100%;
            height: 200px;
            background: #f8f9fa;
            display:flex;
            justify-content:center;
            align-items:center;
            border: 1px dashed #ced4da;
            border-radius: 5px;
        }
    `
    ],
    template: `
        <dot-drop-zone [allowedExtensions]="allowedExtensions" [allowedMimeTypes]="allowedMimeTypes">
            <div class="content">
                Drop files here
            </div>
        </dot-drop-zone>
    `
});

export const Base = Template.bind({});

Base.args = {
    allowedExtensions: [],
    allowedMimeTypes: []
};

export const ValidExtensions = Template.bind({});

ValidExtensions.args = {
    allowedExtensions: ['.png', '.ts'],
    allowedMimeTypes: []
};

export const ValidMimeTypes = Template.bind({});

ValidMimeTypes.args = {
    allowedExtensions: [],
    allowedMimeTypes: ['video/*', 'image/png', 'image/jpeg']
};

export const ValidMimeTypeAndExtensions = Template.bind({});

ValidMimeTypeAndExtensions.args = {
    allowedExtensions: ['.png'],
    allowedMimeTypes: ['video/*']
};
