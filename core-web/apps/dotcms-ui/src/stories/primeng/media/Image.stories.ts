import { Meta, StoryFn } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ImageModule } from 'primeng/image';

export default {
    title: 'PrimeNG/Media/Image',
    component: Image,
    args: { src: 'https://picsum.photos/600/900', alt: 'Image Description', preview: true },
    parameters: {
        docs: {
            description: {
                component: 'Displays an image with preview and tranformation options.'
            }
        }
    }
} as Meta;

const Template = `<p-image [src]="src" [alt]="alt" [preview]="preview"></p-image>`;

export const Main: StoryFn = (args) => {
    return {
        props: {
            src: args.src,
            alt: args.alt,
            preview: args.preview
        },
        moduleMetadata: { imports: [ImageModule, BrowserAnimationsModule] },
        template: Template
    };
};
