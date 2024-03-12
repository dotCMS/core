import { moduleMetadata } from "@storybook/angular";

import { TableModule } from 'primeng/table';

import { DotTableComponent } from "./dot-table.component";


export default {
  title: 'DotTableComponent',
  component: DotTableComponent,
  decorators: [
    moduleMetadata({
        imports: [TableModule],

    })
]
};


export const Default = () => ({
  component: DotTableComponent,
});