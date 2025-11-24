import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { FormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { InputTextModule } from 'primeng/inputtext';
import { RadioButtonModule } from 'primeng/radiobutton';
import { RippleModule } from 'primeng/ripple';

const InputGroupTemplate = `
<div class="grid p-fluid">
  <div class="p-col-12 mb-3">
      <div class="p-inputgroup">
          <span class="p-inputgroup-addon"><i class="pi pi-user"></i></span>
          <input type="text" pInputText placeholder="Username">
      </div>
  </div>

  <div class="p-col-12 mb-3">
      <div class="p-inputgroup">
          <span class="p-inputgroup-addon">$</span>
          <input type="text" pInputText placeholder="Price">
          <span class="p-inputgroup-addon">.00</span>
    </div>
  </div>

  <div class="p-col-12 mb-3">
      <div class="p-inputgroup">
          <span class="p-inputgroup-addon">www</span>
          <input type="text" pInputText placeholder="Website">
      </div>
  </div>
</div>
<div class="grid">
  <div class="p-col-12 mb-3">
      <div class="p-inputgroup">
          <span class="p-inputgroup-addon"><i class="pi pi-tags" style="line-height: 1.25;"></i></span>
          <span class="p-inputgroup-addon"><i class="pi pi-shopping-cart" style="line-height: 1.25;"></i></span>
          <input type="text" pInputText placeholder="Price">
          <span class="p-inputgroup-addon">$</span>
          <span class="p-inputgroup-addon">.00</span>
      </div>
  </div>
</div>
<div class="grid p-fluid">
  <div class="p-col-12 p-md-4 mb-3">
      <div class="p-inputgroup">
          <button type="button" pButton pRipple>
              <span pButtonLabel>Search</span>
          </button>
          <input type="text" pInputText placeholder="Keyword">
      </div>
  </div>

  <div class="p-col-12 p-md-4 mb-3">
      <div class="p-inputgroup">
          <input type="text" pInputText placeholder="Keyword">
          <button type="button" pButton pRipple class="p-button-warn">
              <i class="pi pi-refresh" pButtonIcon></i>
          </button>
      </div>
  </div>

  <div class="p-col-12 p-md-4 mb-3">
      <div class="p-inputgroup">
          <button type="button" pButton pRipple class="p-button-success">
              <i class="pi pi-check" pButtonIcon></i>
          </button>
          <input type="text" pInputText placeholder="Vote">
          <button type="button" pButton pRipple class="p-button-danger">
              <i class="pi pi-times" pButtonIcon></i>
          </button>
      </div>
  </div>
</div>
<div class="grid p-fluid">
  <div class="p-col-12 p-md-12 mb-3">
      <div class="p-inputgroup">
          <span class="p-inputgroup-addon"><p-checkbox></p-checkbox></span>
          <input type="text" pInputText placeholder="Username">
      </div>
  </div>

  <div class="p-col-12 p-md-12 mb-3">
      <div class="p-inputgroup">
          <input type="text" pInputText placeholder="Price">
          <span class="p-inputgroup-addon"><p-radioButton></p-radioButton></span>
      </div>
  </div>

  <div class="p-col-12 p-md-12 mb-3">
      <div class="p-inputgroup">
          <span class="p-inputgroup-addon"><p-checkbox></p-checkbox></span>
          <input type="text" pInputText placeholder="Website">
          <span class="p-inputgroup-addon"><p-radioButton></p-radioButton></span>
      </div>
  </div>
</div>
`;

const meta: Meta = {
    title: 'PrimeNG/Form/InputText/InputGroup',
    parameters: {
        docs: {
            description: {
                component:
                    'Text, icon, buttons and other content can be grouped next to an input.: https://primeng.org/inputgroup'
            }
        }
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
                BrowserAnimationsModule
            ]
        })
    ],
    args: {
        text: 'Placeholder text'
    },
    render: (args) => ({
        props: args,
        template: InputGroupTemplate
    })
};
export default meta;

type Story = StoryObj;

export const Primary: Story = {
    parameters: {
        docs: {
            source: {
                code: InputGroupTemplate
            }
        }
    }
};
