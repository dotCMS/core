<script setup lang="ts">
import { computed, ref, useId } from 'vue';

import { useIsEditMode } from '@/composables/useIsEditMode';

const TRAVEL_BOT_KEY = '908b8a434ad7e539632b8db57f2967c0';
const DESTINATIONS = [
    'Amalfi Coast, Italy',
    'Kyoto, Japan',
    'Patagonia, Chile',
    'Reykjavík, Iceland',
    'Queenstown, New Zealand'
];

const props = defineProps<{ widgetTitle?: string; identifier?: string; code?: string }>();

const isEditMode = useIsEditMode();
const baseId = useId();
const submitted = ref(false);

const isTravelBot = computed(() => props.identifier === TRAVEL_BOT_KEY);
const isBooking = computed(() => props.widgetTitle === 'Booking Date Selector');
</script>

<template>
    <!-- TravelBot promo -->
    <div
        v-if="isTravelBot"
        class="mx-auto my-6 max-w-lg rounded-2xl border border-line bg-bg p-8 text-center">
        <h2 class="font-display text-2xl font-semibold text-ink">Welcome to TravelBot</h2>
        <p class="mt-4 text-muted">
            TravelBot is built with <span class="font-medium text-ink">dotAI</span>, the dotCMS
            suite of AI features.
        </p>
        <p class="mt-2 text-muted">Configure the dotAI App to enable dotAI and TravelBot.</p>
    </div>

    <!-- Booking date selector -->
    <section
        v-else-if="isBooking && submitted"
        class="mx-auto max-w-2xl rounded-2xl bg-primary-deep p-8 text-center text-bg sm:p-10">
        <h3 class="font-display text-2xl font-semibold">Your trip is on hold</h3>
        <p class="mx-auto mt-3 max-w-md text-bg/80">
            This is a demo, so nothing was booked. In a real site this would check availability and
            take you to checkout.
        </p>
        <button
            type="button"
            class="mt-6 inline-flex items-center gap-1.5 rounded-full bg-accent px-6 py-3 font-semibold text-bg transition-transform duration-300 ease-(--ease-out-quart) hover:-translate-y-0.5"
            @click="submitted = false">
            Start over
        </button>
    </section>

    <section v-else-if="isBooking" class="rounded-2xl bg-surface p-6 sm:p-8">
        <div class="mb-6 flex flex-col gap-2">
            <span class="eyebrow">Plan your trip</span>
            <h3 class="font-display text-h3 font-semibold text-ink">Check availability</h3>
        </div>

        <form class="flex flex-wrap items-end gap-4" @submit.prevent="submitted = true">
            <div class="min-w-52 flex-[2]">
                <label
                    :for="`${baseId}-dest`"
                    class="mb-1.5 block text-sm font-medium text-ink">
                    Destination
                </label>
                <select
                    :id="`${baseId}-dest`"
                    required
                    class="w-full appearance-none rounded-xl border border-line bg-bg px-4 py-3 text-ink focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/30">
                    <option value="" disabled selected>Where are you headed?</option>
                    <option v-for="d in DESTINATIONS" :key="d" :value="d">{{ d }}</option>
                </select>
            </div>

            <div class="min-w-36 flex-1">
                <label :for="`${baseId}-in`" class="mb-1.5 block text-sm font-medium text-ink">
                    Check in
                </label>
                <input
                    :id="`${baseId}-in`"
                    type="date"
                    required
                    class="w-full rounded-xl border border-line bg-bg px-4 py-3 text-ink focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/30" />
            </div>

            <div class="min-w-36 flex-1">
                <label :for="`${baseId}-out`" class="mb-1.5 block text-sm font-medium text-ink">
                    Check out
                </label>
                <input
                    :id="`${baseId}-out`"
                    type="date"
                    required
                    class="w-full rounded-xl border border-line bg-bg px-4 py-3 text-ink focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/30" />
            </div>

            <div class="min-w-24 flex-1">
                <label
                    :for="`${baseId}-guests`"
                    class="mb-1.5 block text-sm font-medium text-ink">
                    Travelers
                </label>
                <input
                    :id="`${baseId}-guests`"
                    type="number"
                    :min="1"
                    :max="12"
                    :value="2"
                    class="w-full rounded-xl border border-line bg-bg px-4 py-3 text-ink focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/30" />
            </div>

            <button
                type="submit"
                class="inline-flex h-[50px] shrink-0 items-center justify-center gap-2 rounded-full bg-primary px-6 font-semibold text-bg transition-colors hover:bg-primary-deep">
                Search
                <span aria-hidden="true">→</span>
            </button>
        </form>
    </section>

    <!-- Edit-mode fallback -->
    <div
        v-else-if="isEditMode"
        role="alert"
        class="rounded-xl border border-line bg-surface p-4 text-sm text-muted">
        <h4 class="font-medium text-ink">Simple Widget: {{ widgetTitle }}</h4>
    </div>
</template>
