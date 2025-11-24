import { faker } from '@faker-js/faker';
import { Meta, moduleMetadata, StoryObj, argsToTemplate } from '@storybook/angular';

import { DataView, DataViewModule } from 'primeng/dataview';

type Data = {
    id: string;
    name: string;
    description: string;
    image: string;
    price: string;
    category: string;
};

const generateFakeProduct = (): Data => ({
    id: faker.commerce.isbn(),
    name: faker.commerce.productName(),
    description: faker.commerce.productDescription(),
    image: faker.image.url({
        width: 200,
        height: 200
    }),
    price: faker.commerce.price(),
    category: faker.commerce.department()
});

const data = faker.helpers.multiple(generateFakeProduct, { count: 50 });

const meta: Meta<DataView> = {
    title: 'PrimeNG/Data/DataView',
    decorators: [
        moduleMetadata({
            imports: [DataViewModule]
        })
    ],
    parameters: {
        docs: {
            description: {
                component: 'https://primeng.org/dataview'
            }
        }
    },
    args: {
        value: [...data],
        rows: 3,
        paginator: true,
        layout: 'list'
    },
    render: (args) => ({
        props: { ...args },
        template: `
        <p-dataView ${argsToTemplate(args)}>
            <ng-template pTemplate="list" let-products>
                @for(item of products; track item.id){
                    <div class="w-full">
                        <div class="flex flex-col sm:flex-row sm:align-items-center p-4 gap-3">
                            <div class="md:w-10rem relative">
                                <img class="block xl:block mx-auto border-round w-full" [src]="item.image" [alt]="item.name" />
                            </div>
                            <div>
                                <span class="font-medium text-secondary text-sm">{{ item.category }}</span>
                                <div class="text-lg font-medium text-900 mt-2">{{ item.name }}</div>
                            </div>
                        </div>
                    </div>
                }
            </ng-template>
        </p-dataView>`
    })
};
export default meta;

type Story = StoryObj<DataView>;

export const Default: Story = {};

export const Grid: Story = {
    args: {
        layout: 'grid',
        rows: 6
    },
    render: (args) => ({
        props: { ...args },
        template: `
        <p-dataView ${argsToTemplate(args)}>
            <ng-template pTemplate="grid" let-products>
                <div class="grid grid-nogutter">
                    @for(item of products; track item.id){
                        <div class="col-4">
                            <div class="flex flex-col sm:flex-row sm:align-items-center p-4 gap-3">
                                <div class="md:w-10rem relative">
                                    <img class="block xl:block mx-auto border-round w-full" [src]="item.image" [alt]="item.name" />
                                </div>
                                <div>
                                    <span class="font-medium text-secondary text-sm">{{ item.category }}</span>
                                    <div class="text-lg font-medium text-900 mt-2">{{ item.name }}</div>
                                </div>
                            </div>
                        </div>
                    }
                </div>
            </ng-template>
        </p-dataView>`
    })
};
