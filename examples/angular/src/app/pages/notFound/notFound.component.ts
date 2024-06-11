import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [RouterLink],
  template: `<div
    class="bg-slate-100 min-h-dvh w-full flex justify-center items-center"
  >
    <section>
      <div class="py-8 px-4 mx-auto max-w-screen-xl lg:py-16 lg:px-6">
        <div class="mx-auto max-w-screen-sm text-center">
          <h1
            class="mb-4 text-7xl tracking-tight font-extrabold lg:text-9xl text-primary-600"
          >
            404
          </h1>
          <p
            class="mb-4 text-3xl tracking-tight font-bold text-gray-900 md:text-4xl"
          >
            Something&apos;s missing.
          </p>
          <p class="mb-4 text-lg font-light text-gray-500">
            Sorry, we can&apos;t find that page. You&apos;ll find lots to
            explore on the home page.
          </p>
          <a
            routerLink="/"
            class="inline-flex text-white bg-red-400 hover:bg-red-500 focus:ring-4 focus:outline-none focus:ring-red-100 font-medium rounded-lg text-sm px-5 py-2.5 text-center my-4"
          >
            Return Home
          </a>
        </div>
      </div>
    </section>
  </div> `,
  styleUrl: './notFound.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotFoundComponent {}
