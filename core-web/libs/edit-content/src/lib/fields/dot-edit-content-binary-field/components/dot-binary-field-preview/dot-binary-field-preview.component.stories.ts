import { action } from '@storybook/addon-actions';
import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { SkeletonModule } from 'primeng/skeleton';

import { DotResourceLinksService } from '@dotcms/data-access';
import {
    DotTempFileThumbnailComponent,
    DotSpinnerModule,
    DotCopyButtonComponent,
    DotFileSizeFormatPipe,
    DotMessagePipe
} from '@dotcms/ui';

import { DotBinaryFieldPreviewComponent } from './dot-binary-field-preview.component';

import { DotFilePreview } from '../../interfaces';
import { fileMetaData } from '../../utils/mock';

const previewImage: DotFilePreview = {
    ...fileMetaData,
    id: '123',
    inode: '123',
    titleImage: 'Assets',
    contentType: 'image/png',
    name: 'test.png'
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

    contentType: 'video/png'
};

const previewFile = {
    type: 'file',
    fileSize: 8000,
    mimeType: 'text/html',
    inode: '123456789',
    titlevideo: 'true',
    name: 'template.html',
    title: 'template.html',

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
    </html>`
};

export default {
    title: 'Library / Edit Content / Binary Field / Components / Preview',
    component: DotBinaryFieldPreviewComponent,
    decorators: [
        moduleMetadata({
            imports: [
                BrowserAnimationsModule,
                CommonModule,
                ButtonModule,
                SkeletonModule,
                DotTempFileThumbnailComponent,
                DotSpinnerModule,
                DialogModule,
                DotMessagePipe,
                DotFileSizeFormatPipe,
                DotCopyButtonComponent,
                HttpClientModule
            ],
            providers: [DotResourceLinksService]
        })
    ],
    parameters: {
        actions: {
            handles: ['editFile', 'removeFile']
        }
    },
    args: {
        file: previewImage,
        variableName: 'binaryField'
    },
    argTypes: {
        file: {
            defaultValue: previewImage,
            control: 'object',
            description: 'Preview object'
        },
        variableName: {
            defaultValue: 'binaryField',
            control: 'text',
            description: 'Field variable name'
        },
        fieldVariable: {
            defaultValue: 'Blog',
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
                [file]="file"
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
    file: previewVideo,
    variableName: 'binaryField',
    fieldVariable: 'Blog'
};

export const File = Template.bind({});

File.args = {
    file: previewFile,
    variableName: 'binaryField',
    fieldVariable: 'Blog'
};
