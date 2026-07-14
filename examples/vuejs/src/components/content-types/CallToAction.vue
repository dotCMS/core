<script setup lang="ts">
defineProps<{
    title?: string;
    subHeading?: string;
    buttonText1?: string;
    buttonUrl1?: string;
    buttonText2?: string;
    buttonUrl2?: string;
}>();
</script>

<template>
    <div
        class="relative isolate flex w-full flex-col justify-center overflow-hidden rounded-3xl bg-primary-deep p-8 text-bg sm:p-12 md:p-16">
        <div
            aria-hidden="true"
            class="absolute -right-24 -top-24 size-72 rounded-full bg-accent/20 blur-3xl" />
        <div class="relative max-w-2xl">
            <h2
                v-if="title"
                class="font-display text-[clamp(1.85rem,1.2rem+2.4vw,3rem)] font-semibold leading-tight">
                {{ title }}
            </h2>
            <!-- eslint-disable-next-line vue/no-v-html -- renders CMS-authored copy -->
            <div
                v-if="subHeading"
                class="mt-4 text-lg leading-relaxed text-bg/85 [&_a]:underline"
                v-html="subHeading" />

            <div v-if="buttonUrl1 || buttonUrl2" class="mt-8 flex flex-wrap gap-4">
                <a
                    v-if="buttonText1 && buttonUrl1"
                    :href="buttonUrl1"
                    class="inline-flex items-center gap-2 rounded-full bg-accent px-6 py-3 font-semibold text-bg shadow-lg transition-transform duration-300 ease-(--ease-out-quart) hover:-translate-y-0.5">
                    {{ buttonText1 }}
                    <span aria-hidden="true">→</span>
                </a>
                <a
                    v-if="buttonText2 && buttonUrl2"
                    :href="buttonUrl2"
                    class="inline-flex items-center rounded-full border border-bg/30 px-6 py-3 font-semibold text-bg transition-colors hover:border-bg/60 hover:bg-bg/10">
                    {{ buttonText2 }}
                </a>
            </div>
        </div>
    </div>
</template>
