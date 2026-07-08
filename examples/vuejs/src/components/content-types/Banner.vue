<script setup lang="ts">
import { DotCMSEditableText } from '@dotcms/vue';
import { cva } from 'class-variance-authority';
import { computed } from 'vue';

import { BUTTON_COLORS, buttonColorVariants } from './styles';

import { cn, variant } from '@/lib/utils';
import type { ContentTypeProps, DotCMSImage } from '@/types/content';
import { imageLoader } from '@/utils/imageLoader';

const ALIGNMENTS = ['left', 'center', 'right'] as const;
const OVERLAYS = ['dark', 'light', 'gradient', 'none'] as const;
const BUTTON_SIZES = ['small', 'medium', 'large'] as const;

const props = defineProps<
    ContentTypeProps & {
        caption?: string;
        image?: DotCMSImage;
        link?: string;
        buttonText?: string;
    }
>();

const overlayVariants = cva('pointer-events-none absolute inset-0', {
    variants: {
        style: {
            dark: 'bg-black/40',
            light: 'bg-white/20',
            gradient: 'bg-gradient-to-b from-black/45 via-transparent to-black/45',
            none: 'hidden'
        }
    },
    defaultVariants: { style: 'none' }
});

const contentVariants = cva(
    'relative z-10 flex h-full flex-col justify-end gap-4 p-6 text-bg sm:p-10 md:p-14',
    {
        variants: {
            alignment: {
                left: 'items-start text-left',
                center: 'items-center text-center',
                right: 'items-end text-right'
            }
        },
        defaultVariants: { alignment: 'left' }
    }
);

const buttonSizeVariants = cva('', {
    variants: {
        size: {
            small: 'px-5 py-2.5 text-sm',
            medium: 'px-6 py-3 text-base',
            large: 'px-8 py-4 text-lg'
        }
    },
    defaultVariants: { size: 'medium' }
});

const sp = computed(() => props.dotStyleProperties ?? {});
const titleStyle = computed(() => sp.value['title-style'] || {});
const buttonStyle = computed(() => sp.value['button-style'] || {});

const titleClasses = computed(() =>
    cn(
        'max-w-3xl font-display text-[clamp(2.25rem,1.4rem+3.6vw,4.5rem)] font-semibold leading-[1.03] tracking-tight text-shadow',
        titleStyle.value.italic && 'italic',
        titleStyle.value.underline && 'underline'
    )
);

const overlayClass = computed(() =>
    overlayVariants({ style: variant(sp.value['overlay-style'] || 'none', OVERLAYS) })
);
const contentClass = computed(() =>
    contentVariants({ alignment: variant(sp.value['text-alignment'], ALIGNMENTS) })
);

const buttonClasses = computed(() => {
    const colorKey = variant(sp.value['button-color'], BUTTON_COLORS);

    return cn(
        'inline-flex items-center gap-2 font-semibold text-bg shadow-lg transition-transform duration-300 ease-(--ease-out-quart) hover:-translate-y-0.5',
        colorKey ? buttonColorVariants({ color: colorKey }) : 'bg-accent hover:bg-accent/90',
        buttonSizeVariants({ size: variant(sp.value['button-size'], BUTTON_SIZES) }),
        buttonStyle.value.rounded
            ? 'rounded-lg'
            : buttonStyle.value['full-rounded']
              ? 'rounded-full'
              : 'rounded-xl'
    );
});

const imageSrc = computed(() =>
    props.image?.identifier ? imageLoader(props.image.identifier, 1600) : ''
);
</script>

<template>
    <section
        class="relative isolate overflow-hidden rounded-3xl bg-surface-2 [height:clamp(26rem,60vh,40rem)]">
        <img
            v-if="imageSrc"
            :src="imageSrc"
            class="absolute inset-0 h-full w-full object-cover motion-safe:animate-[heroIn_1.2s_var(--ease-out-expo)_both]"
            :alt="title || 'Featured destination'" />

        <div
            aria-hidden="true"
            class="absolute inset-0 bg-gradient-to-t from-black/70 via-black/25 to-black/10" />
        <div aria-hidden="true" :class="overlayClass" />

        <div :class="contentClass">
            <h1 :class="titleClasses">
                <DotCMSEditableText :contentlet="(props as never)" field-name="title" />
            </h1>
            <p v-if="caption" class="max-w-xl text-base text-bg/90 text-shadow sm:text-lg">
                {{ caption }}
            </p>
            <a v-if="link" :class="buttonClasses" :href="link">
                {{ buttonText || 'Explore' }}
                <span aria-hidden="true">→</span>
            </a>
        </div>
    </section>
</template>
