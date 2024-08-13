import { faker } from '@faker-js/faker';
import { Meta, moduleMetadata, StoryObj, argsToTemplate } from '@storybook/angular';

import { DataView, DataViewModule } from 'primeng/dataview';

const data = Array.from({ length: 4 }).map(() => ({
    id: faker.commerce.isbn(),
    name: faker.commerce.productName(),
    description: faker.commerce.productDescription(),
    image: faker.image.urlLoremFlickr({
        category: 'fashion',
        width: 300,
        height: 300
    }),
    price: faker.commerce.price(),
    category: faker.commerce.productMaterial()
}));

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
    render: (args) => {
        return {
            props: { ...args },
            template: `
            <p-dataView ${argsToTemplate(args)}>
                <ng-template pTemplate="list" let-products>
                    <div class="grid grid-nogutter">
                         <div class="col-12" *ngFor="let item of products">
                            <div class="flex flex-column sm:flex-row sm:align-items-center p-4 gap-3">
                                <div class="md:w-10rem relative">
                                    <img class="block xl:block mx-auto border-round w-full" [src]="item.image" [alt]="item.name" />
                                </div>
                                <div>
                                    <span class="font-medium text-secondary text-sm">{{ item.category }}</span>
                                    <div class="text-lg font-medium text-900 mt-2">{{ item.name }}</div>
                                </div>
                            </div>
                         </div>
                    </div>
                </ng-template>
            </p-dataView>`
        };
    }
};
export default meta;

type Story = StoryObj<DataView>;

export const Primary: Story = {
    args: {
        value: [...data],
        rows: 3,
        paginator: true
    }
};
