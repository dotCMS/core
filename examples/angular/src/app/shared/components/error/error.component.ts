import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PageError } from '../../models';
@Component({
  selector: 'app-error',
  imports: [RouterLink],
  template: `<div
    class="flex justify-center items-center w-full bg-slate-100 min-h-dvh"
  >
    <section>
      <div class="px-4 py-8 mx-auto max-w-screen-xl lg:py-16 lg:px-6">
        <div class="mx-auto max-w-screen-sm text-center text-gray-500">
          <h1
            class="mb-4 text-7xl font-extrabold tracking-tight lg:text-9xl text-primary-600"
          >
            {{ $error().status }}
          </h1>
          <p
            class="mb-4 text-3xl font-bold tracking-tight text-gray-900 md:text-4xl"
          >
            {{ $error().message }}
          </p>
          @if ($error().status !== 401) {
            <p class="mb-4 text-lg font-light text-gray-500">
              You&apos;ll find lots to explore on the home page.
            </p>
            <a
              routerLink="/"
              class="inline-flex text-white bg-red-400 hover:bg-red-500 focus:ring-4 focus:outline-none focus:ring-red-100 font-medium rounded-lg text-sm px-5 py-2.5 text-center my-4"
            >
              Return Home
            </a>
          }
        </div>
      </div>
    </section>
  </div> `,
  styleUrl: './error.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ErrorComponent {
  $error = input.required<PageError>();
}
