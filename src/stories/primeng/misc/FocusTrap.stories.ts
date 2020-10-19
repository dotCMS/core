import { Story, Meta } from '@storybook/angular/types-6-0';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { AccordionModule } from 'primeng/accordion';
import { InputTextModule } from 'primeng/inputtext';
import { CodeHighlighterModule } from 'primeng/codehighlighter';
import { FocusTrapModule } from 'primeng/focustrap';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { CalendarModule } from 'primeng/calendar';
import { MultiSelectModule } from 'primeng/multiselect';
import { DropdownModule } from 'primeng/dropdown';
import { moduleMetadata } from '@storybook/angular';

export default {
  title: 'PrimeNG/Misc/FocusTrap',
  parameters: {
    docs: {
      description: {
        component:
          'Focus Trap keeps focus within a certain DOM element while tabbing: https://primefaces.org/primeng/showcase/#/focustrap',
      },
    },
  },
  decorators: [
    moduleMetadata({
      imports: [
        BrowserModule,
        BrowserAnimationsModule,
        FormsModule,
        DialogModule,
        ButtonModule,
        InputTextModule,
        AccordionModule,
        CodeHighlighterModule,
        FocusTrapModule,
        AutoCompleteModule,
        CalendarModule,
        MultiSelectModule,
        DropdownModule,
        HttpClientModule,
      ],
    }),
  ],
  args: {
    cities: [
      { name: 'New York', code: 'NY' },
      { name: 'Rome', code: 'RM' },
      { name: 'London', code: 'LDN' },
      { name: 'Istanbul', code: 'IST' },
      { name: 'Paris', code: 'PRS' },
    ],
  },
} as Meta;

const FocusTrapTemplate = `
  <div pFocusTrap class="card">
    <h5>Input</h5>
    <input id="input" type="text" size="30" pInputText>

    <h5>Float Label</h5>
    <span class="p-float-label">
        <input id="float-input" type="text" size="30" pInputText>
        <label for="float-input">Username</label>
    </span>

    <h5>Disabled Input</h5>
    <input id="disabled-input" type="text" size="30" pInputText [disabled]="true" />

    <h5>Input with tabindex -1</h5>
    <input type="text" size="30" pInputText tabindex="-1" />

    <h5>Button</h5>
    <button pButton type="button" icon="pi pi-check" label="Check"></button>

    <h5>Disabled Button</h5>
    <button pButton type="button" icon="pi pi-check" [disabled]="true" label="Disabled"></button>

    <h5>Button with tabindex -1</h5>
    <button pButton type="button" icon="pi pi-check" tabindex="-1"  label="Check"></button>

    <h5>Dropdown</h5>
    <p-dropdown [options]="cities" [(ngModel)]="selectedCity" placeholder="Select a City" optionLabel="name" [showClear]="true"></p-dropdown>
</div>
`;

const Template: Story<any> = (props: any) => {
  const template = FocusTrapTemplate;
  return {
    props,
    template,
  };
};

export const Primary: Story = Template.bind({});
Primary.parameters = {
  docs: {
    source: {
      code: FocusTrapTemplate,
    },
  },
};
