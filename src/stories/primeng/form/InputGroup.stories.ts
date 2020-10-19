import { Story, Meta } from '@storybook/angular/types-6-0';
import { FormsModule } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { RadioButtonModule } from 'primeng/radiobutton';
import { RippleModule } from 'primeng/ripple';

import { moduleMetadata } from '@storybook/angular';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

export default {
  title: 'PrimeNG/Form/InputGroup',
  parameters: {
    docs: {
      description: {
        component:
          'Text, icon, buttons and other content can be grouped next to an input.: https://primefaces.org/primeng/showcase/#/inputgroup',
      },
    },
  },
  decorators: [
    moduleMetadata({
      imports: [
        FormsModule,
        InputTextModule,
        ButtonModule,
        CheckboxModule,
        RadioButtonModule,
        RippleModule,
        BrowserAnimationsModule,
      ],
    }),
  ],
  args: {
    text: 'Placeholder text',
  },
} as Meta;

const InputGroupTemplate = `
  <div class="p-grid p-fluid">
    <div class="p-col-12">
        <div class="p-inputgroup">
            <span class="p-inputgroup-addon"><i class="pi pi-user"></i></span>
            <input type="text" pInputText placeholder="Username">
        </div>
    </div>

    <div class="p-col-12">
        <div class="p-inputgroup">
            <span class="p-inputgroup-addon">$</span>
            <input type="text" pInputText placeholder="Price">
            <span class="p-inputgroup-addon">.00</span>
      </div>
    </div>  

    <div class="p-col-12">
        <div class="p-inputgroup">
            <span class="p-inputgroup-addon">www</span>
            <input type="text" pInputText placeholder="Website">
        </div>
    </div>
</div>
<div class="p-grid">
    <div class="p-col-12">
        <div class="p-inputgroup">
            <span class="p-inputgroup-addon"><i class="pi pi-tags" style="line-height: 1.25;"></i></span>  
            <span class="p-inputgroup-addon"><i class="pi pi-shopping-cart" style="line-height: 1.25;"></i></span>   
            <input type="text" pInputText placeholder="Price"> 
            <span class="p-inputgroup-addon">$</span>  
            <span class="p-inputgroup-addon">.00</span>      
        </div>
    </div>
</div>
<div class="p-grid p-fluid">
    <div class="p-col-12 p-md-4">
        <div class="p-inputgroup">
            <button type="button" pButton pRipple label="Search"></button>
            <input type="text" pInputText placeholder="Keyword">
        </div>
    </div>

    <div class="p-col-12 p-md-4">
        <div class="p-inputgroup">
            <input type="text" pInputText placeholder="Keyword">   
            <button type="button" pButton pRipple icon="pi pi-refresh" styleClass="p-button-warn"></button>
        </div>
    </div>

    <div class="p-col-12 p-md-4">
        <div class="p-inputgroup">
            <button type="button" pButton pRipple icon="pi pi-check" styleClass="p-button-success"></button>
            <input type="text" pInputText placeholder="Vote">   
            <button type="button" pButton pRipple icon="pi pi-times" styleClass="p-button-danger"></button>
        </div>
    </div>
</div>
<div class="p-grid p-fluid">
    <div class="p-col-12 p-md-12">
        <div class="p-inputgroup">
            <span class="p-inputgroup-addon"><p-checkbox></p-checkbox></span>
            <input type="text" pInputText placeholder="Username">
        </div>
    </div>

    <div class="p-col-12 p-md-12">
        <div class="p-inputgroup">
            <input type="text" pInputText placeholder="Price">
            <span class="p-inputgroup-addon"><p-radioButton></p-radioButton></span>
        </div>
    </div>

    <div class="p-col-12 p-md-12">
        <div class="p-inputgroup">
            <span class="p-inputgroup-addon"><p-checkbox></p-checkbox></span>
            <input type="text" pInputText placeholder="Website">      
            <span class="p-inputgroup-addon"><p-radioButton></p-radioButton></span> 
        </div>
    </div>
</div>
`;

const Template: Story<any> = (props: any) => {
  const template = InputGroupTemplate;
  return {
    props,
    template,
  };
};

export const Primary: Story = Template.bind({});
Primary.parameters = {
  docs: {
    source: {
      code: InputGroupTemplate,
    },
  },
};
