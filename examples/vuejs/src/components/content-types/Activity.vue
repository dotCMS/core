<script setup lang="ts">
import { cva } from 'class-variance-authority';
import { computed } from 'vue';

import { BUTTON_COLORS, buttonColorVariants } from './styles';

import { cn, variant } from '@/lib/utils';
import type { DotCMSImage, DotStyleProperties } from '@/types/content';
import { imageLoader } from '@/utils/imageLoader';

const LAYOUTS = ['left', 'right', 'center', 'overlap'] as const;
const BACKGROUNDS = ['white', 'gray', 'light-blue', 'light-green'] as const;
const RADII = ['none', 'small', 'medium', 'large'] as const;
const BUTTON_SIZES = ['small', 'medium', 'large'] as const;

const props = defineProps<{
    title?: string;
    description?: string;
    image?: DotCMSImage;
    urlTitle?: string;
    dotStyleProperties?: DotStyleProperties;
}>();

const articleVariants = cva('overflow-hidden mb-4', {
    variants: {
        layout: {
            left: 'flex flex-row p-4',
            right: 'flex flex-row-reverse p-4',
            center: 'flex flex-col items-center text-center p-4',
            overlap: 'relative min-h-96 p-0'
        }
    },
    defaultVariants: { layout: 'left' }
});

const imageContainerVariants = cva('', {
    variants: {
        layout: {
            left: 'relative flex-shrink-0 w-1/2',
            right: 'relative flex-shrink-0 w-1/2',
            center: 'relative w-full',
            overlap: 'absolute inset-0 z-0'
        }
    },
    defaultVariants: { layout: 'left' }
});

const contentContainerVariants = cva('', {
    variants: {
        layout: {
            left: 'flex-1 p-6 flex flex-col justify-center',
            right: 'flex-1 p-6 flex flex-col justify-center',
            center: 'w-full px-6 py-4',
            overlap: 'relative z-10 p-8 bg-white/90 min-h-96 flex flex-col justify-center'
        }
    },
    defaultVariants: { layout: 'left' }
});

const cardBackgroundVariants = cva('', {
    variants: {
        background: {
            white: 'bg-bg',
            gray: 'bg-surface',
            'light-blue': 'bg-primary-tint',
            'light-green': 'bg-surface-2'
        }
    },
    defaultVariants: { background: 'white' }
});

const borderRadiusVariants = cva('', {
    variants: {
        radius: {
            none: 'rounded-none',
            small: 'rounded-lg',
            medium: 'rounded-xl',
            large: 'rounded-2xl'
        }
    },
    defaultVariants: { radius: 'small' }
});

const buttonSizeVariants = cva('', {
    variants: {
        size: {
            small: 'px-3 py-1.5 text-sm',
            medium: 'px-4 py-2 text-base',
            large: 'px-6 py-3 text-lg'
        }
    },
    defaultVariants: { size: 'medium' }
});

const sp = computed(() => props.dotStyleProperties ?? {});
const layout = computed(() => variant(sp.value.layout, LAYOUTS) ?? 'left');
const isOverlap = computed(() => layout.value === 'overlap');
const titleStyle = computed(() => sp.value['title-style'] || {});
const cardEffects = computed(() => sp.value['card-effects'] || {});
const buttonStyle = computed(() => sp.value['button-style'] || {});
const imageHeight = computed(() => sp.value['image-height'] || 'h-56');
const descriptionSize = computed(() => sp.value['description-size'] || 'text-base');

const titleClasses = computed(() =>
    cn(
        'mb-2 font-display leading-tight text-ink',
        sp.value['title-size'] || 'text-xl',
        titleStyle.value.bold ? 'font-semibold' : 'font-medium',
        titleStyle.value.italic && 'italic',
        titleStyle.value.underline && 'underline'
    )
);

const articleClasses = computed(() =>
    cn(
        'group',
        articleVariants({ layout: layout.value }),
        cardBackgroundVariants({ background: variant(sp.value['card-background'], BACKGROUNDS) }),
        borderRadiusVariants({ radius: variant(sp.value['border-radius'], RADII) }),
        cardEffects.value.shadow ? 'shadow-xl shadow-primary-deep/5' : 'shadow-sm',
        cardEffects.value.border && 'border border-line'
    )
);

const buttonClasses = computed(() => {
    const colorKey = variant(sp.value['button-color'], BUTTON_COLORS);

    return colorKey
        ? cn(
              'inline-flex items-center gap-1.5 font-semibold text-bg transition-transform duration-300 ease-(--ease-out-quart) hover:-translate-y-0.5',
              buttonColorVariants({ color: colorKey }),
              buttonSizeVariants({ size: variant(sp.value['button-size'], BUTTON_SIZES) }),
              buttonStyle.value.rounded ? 'rounded-lg' : 'rounded-full',
              buttonStyle.value.shadow && 'shadow-md'
          )
        : 'group/btn inline-flex w-fit items-center gap-1.5 text-sm font-semibold text-primary transition-colors hover:text-primary-deep';
});

const imageSrc = computed(() =>
    props.image?.identifier ? imageLoader(props.image.identifier, 800) : ''
);
</script>

<template>
    <article :class="articleClasses">
        <div v-if="imageSrc" :class="imageContainerVariants({ layout })">
            <div
                :class="cn('relative w-full overflow-hidden', isOverlap ? 'h-full' : imageHeight)">
                <img
                    class="absolute inset-0 h-full w-full object-cover transition-transform duration-700 ease-(--ease-out-quart) group-hover:scale-105"
                    :src="imageSrc"
                    :alt="title || 'Activity'" />
            </div>
        </div>
        <div :class="contentContainerVariants({ layout })">
            <h3 :class="titleClasses">{{ title }}</h3>
            <p :class="cn('mb-5 line-clamp-3 leading-relaxed text-muted', descriptionSize)">
                {{ description }}
            </p>
            <div :class="layout === 'center' ? 'flex justify-center' : ''">
                <a :href="`/activities/${urlTitle || '#'}`" :class="buttonClasses">
                    View detail
                    <span
                        aria-hidden="true"
                        class="transition-transform duration-300 ease-(--ease-out-quart) group-hover/btn:translate-x-0.5">
                        →
                    </span>
                </a>
            </div>
        </div>
    </article>
</template>
