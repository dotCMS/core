import { action } from '@storybook/addon-actions';
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
    ],
    parameters: {
        // https://storybook.js.org/docs/6.5/angular/essentials/actions#action-event-handlers
        actions: {
            // detect if the component is emitting the correct HTML events
            handles: ['fileDrop', 'dragStart', 'dragStop', 'dropZoneError']
        }
    }
} as Meta<DotDropZoneComponent>;

const Template: Story<DotDropZoneComponent> = (args: DotDropZoneComponent) => ({
    props: {
        ...args,
        // https://storybook.js.org/docs/6.5/angular/essentials/actions#action-args
        fileDrop: action('fileDrop'),
        dragStart: action('dragStart'),
        dragStop: action('dragStop'),
        dropZoneError: action('dropZoneError')
    },
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
            flex-direction: column;
            gap: 1rem;
            justify-content:center;
            align-items:center;
            border: 1px dashed #ced4da;
            border-radius: 5px;
        }
    `
    ],
    template: `
        <dot-drop-zone
            [allowedExtensions]="allowedExtensions"
            [allowedMimeTypes]="allowedMimeTypes"
            (fileDrop)="fileDrop($event)"
            (dragStart)="dragStart($event)"
            (dragStop)="dragStop($event)"
            (dropZoneError)="dropZoneError($event)"
        >
            <div class="content">
                Drop files here.

                <div *ngIf="allowedExtensions.length">
                    <strong>Allowed extensions:</strong> {{ allowedExtensions }}
                </div>

                <div *ngIf="allowedMimeTypes.length">
                    <strong>Allowed mime types:</strong> {{ allowedMimeTypes }}
                </div>
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
