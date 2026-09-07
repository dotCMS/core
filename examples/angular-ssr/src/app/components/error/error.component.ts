import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

export const ERROR_COPY: Record<number | 'default', { heading: string; body: string }> = {
    403: {
        heading: 'Access Denied',
        body: "Sorry, you don't have permission to view this page.",
    },
    404: {
        heading: "Something's missing.",
        body: "Sorry, we can't find that page. You'll find lots to explore on the home page.",
    },
    default: {
        heading: 'Something went wrong.',
        body: 'An unexpected error occurred. Please try again later.',
    },
};

@Component({
    selector: 'app-error',
    imports: [RouterLink],
    template: `
        <div class="bg-slate-100 min-h-dvh w-full flex justify-center items-center">
            <section>
                <div class="py-8 px-4 mx-auto max-w-5xl lg:py-16 lg:px-6">
                    <div class="mx-auto max-w-2xl text-center">
                        <h1 class="mb-4 text-7xl tracking-tight font-extrabold lg:text-9xl text-primary-600">
                            {{ status() }}
                        </h1>
                        <p class="mb-4 text-3xl tracking-tight font-bold text-gray-900 md:text-4xl">
                            {{ heading() }}
                        </p>
                        <p class="mb-4 text-lg font-light text-gray-500">
                            {{ body() }}
                        </p>
                        <a
                            routerLink="/"
                            class="inline-flex text-white bg-purple-600 hover:bg-purple-800 focus:ring-4 focus:outline-hidden focus:ring-purple-300 font-medium rounded-lg text-sm px-5 py-2.5 text-center my-4"
                        >
                            Return Home
                        </a>
                    </div>
                </div>
            </section>
        </div>
    `,
})
export class ErrorComponent {
    status = input<number>(500);
    heading = input<string>(ERROR_COPY['default'].heading);
    body = input<string>(ERROR_COPY['default'].body);
}
