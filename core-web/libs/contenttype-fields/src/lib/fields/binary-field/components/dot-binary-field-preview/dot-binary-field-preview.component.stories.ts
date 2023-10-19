import { action } from '@storybook/addon-actions';
import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { CommonModule } from '@angular/common';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';

import { DotBinaryFieldPreviewComponent } from './dot-binary-field-preview.component';

const previewImage = {
    type: 'image',
    resolution: {
        width: '400',
        height: '400'
    },
    fileSize: 8000,
    content: '',
    mimeType: 'image/png',
    inode: '123456789',
    titleImage: 'true',
    name: 'image.jpg',
    title: 'image.jpg',
    hasTitleImage: 'true',
    contentType: 'image/png',
    __icon__: 'contentIcon',
    contentTypeIcon: 'image'
};

const previewVideo = {
    type: 'image',
    fileSize: 8000,
    content: '',
    mimeType: 'video/png',
    inode: '123456789',
    titlevideo: 'true',
    name: 'video.jpg',
    title: 'video.jpg',
    hasTitleImage: 'true',
    contentType: 'video/png',
    __icon__: 'video'
};

const previewFile = {
    type: 'file',
    fileSize: 8000,
    mimeType: 'text/html',
    inode: '123456789',
    titlevideo: 'true',
    name: 'template.html',
    title: 'template.html',
    hasTitleImage: 'true',
    contentType: 'text/html',
    content: `<!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Document</title>
    </head>
    <body>
        <h1>I have styles</h1>
    </body>
    </html>`,
    __icon__: 'page'
};

export default {
    title: 'Library / Contenttype Fields / Component / DotBinaryFieldPreviewComponent',
    component: DotBinaryFieldPreviewComponent,
    decorators: [
        moduleMetadata({
            imports: [BrowserAnimationsModule, CommonModule, ButtonModule],
            providers: []
        })
    ],
    parameters: {
        actions: {
            handles: ['editFile', 'removeFile']
        }
    },
    args: {
        previewData: previewImage,
        variableName: 'binaryField'
    },
    argTypes: {
        previewData: {
            defaultValue: previewImage,
            control: 'object',
            description: 'Preview object'
        },
        variableName: {
            defaultValue: 'binaryField',
            control: 'text',
            description: 'Field variable name'
        }
    }
} as Meta<DotBinaryFieldPreviewComponent>;

const Template: Story<DotBinaryFieldPreviewComponent> = (args: DotBinaryFieldPreviewComponent) => ({
    props: {
        ...args,
        // https://storybook.js.org/docs/6.5/angular/essentials/actions#action-args
        editFile: action('editFile'),
        removeFile: action('removeFile')
    },
    styles: [
        `
        .container {
            width: 100%;
            max-width: 36rem;
            height: 12.5rem;
            border: 1px solid #f2f2f2;
            border-radius: 4px;
            padding: 0.5rem;
        }
    `
    ],
    template: `
        <div class="container">
            <dot-binary-field-preview
                [previewData]="previewData"
                [variableName]="variableName"
                (editFile)="editFile($event)"
                (removeFile)="removeFile($event)"
            ></dot-binary-field-preview>
        </div> 
    `
});

export const Image = Template.bind({});

export const Video = Template.bind({});

Video.args = {
    previewData: previewVideo,
    variableName: 'binaryField'
};

export const file = Template.bind({});

file.args = {
    previewData: previewFile,
    variableName: 'binaryField'
};
