import { faker } from '@faker-js/faker';
import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ImageModule, Image } from 'primeng/image';

const meta: Meta<Image> = {
    title: 'PrimeNG/Media/Image',
    component: Image,
    decorators: [
        moduleMetadata({
            imports: [ImageModule, BrowserAnimationsModule]
        })
    ],
    args: {
        src: faker.image.urlLoremFlickr({ width: 600, height: 900 }),
        alt: 'Image Description',
        preview: true
    },
    parameters: {
        docs: {
            description: {
                component: 'Displays an image with preview and tranformation options.'
            }
        }
    },
    render: (args) => ({
        props: args,
        template: `<p-image [src]="src" [alt]="alt" [preview]="preview" />`
    })
};
export default meta;

type Story = StoryObj<Image>;

export const Main: Story = {};
