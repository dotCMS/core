import { Component, computed, input } from '@angular/core';
import { DotCMSShowWhenDirective } from '@dotcms/angular';
import { UVE_MODE } from '@dotcms/types';
import { ContactUsComponent } from './components/contact-us/contact-us.component';
import { PageForm } from '../../shared/contentlet.model';
@Component({
  selector: 'app-page-form',
  imports: [DotCMSShowWhenDirective, ContactUsComponent],
  template: `
    @if (formType() === 'contact-us') {
      <app-contact-us [description]="description()" />
    } @else {
      <ng-container [dotCMSShowWhen]="UVE_MODE.EDIT">
        <div>
          <h4>
            There is no form component for this form type: {{ formType() }}
          </h4>
        </div>
      </ng-container>
    }
  `,
})
export class PageFormComponent {
  contentlet = input.required<PageForm>();

  formType = computed(() => this.contentlet().formType);
  description = computed(() => this.contentlet().description || '');
  UVE_MODE = UVE_MODE;
}
