import { Component, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

interface FormData {
  firstName: string;
  lastName: string;
  email: string;
  acceptTerms: boolean;
}

const initialFormData: FormData = {
  firstName: '',
  lastName: '',
  email: '',
  acceptTerms: false,
};

@Component({
  selector: 'app-contact-us',
  imports: [FormsModule],
  templateUrl: './contact-us.component.html',
})
export class ContactUsComponent {
  description = input<string>('');

  formData = signal<FormData>({ ...initialFormData });
  isSubmitting = signal<boolean>(false);
  isSuccess = signal<boolean>(false);

  handleSubmit(event: Event): void {
    event.preventDefault();
    this.isSubmitting.set(true);

    // Emulate a form submission with timeout
    setTimeout(() => {
      this.isSuccess.set(true);
      this.resetForm();
      this.isSubmitting.set(false);
    }, 3000);
  }

  resetForm(): void {
    this.formData.set({ ...initialFormData });
  }
}
