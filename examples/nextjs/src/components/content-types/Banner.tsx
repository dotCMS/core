'use client';

import { cva } from 'class-variance-authority';
import { DotCMSEditableText } from '@dotcms/react';
import Image from 'next/image';
import Link from 'next/link';

import { cn, variant } from '@/lib/utils';
import type { ContentTypeProps, DotCMSImage } from '@/types/content';
import { BUTTON_COLORS, buttonColorVariants } from './styles';

const ALIGNMENTS = ['left', 'center', 'right'] as const;
const OVERLAYS = ['dark', 'light', 'gradient', 'none'] as const;
const BUTTON_SIZES = ['small', 'medium', 'large'] as const;

type BannerProps = ContentTypeProps & {
    caption?: string;
    image?: DotCMSImage;
    link?: string;
    buttonText?: string;
};

// Editor-controlled overlay (dotStyleProperties). This sits ON TOP of an
// always-present bottom scrim, so overlaid text keeps its contrast even when an
// editor picks 'none' or 'light'.
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

function Banner(contentlet: BannerProps) {
    const { title, caption, image, link, buttonText, dotStyleProperties } = contentlet;

    const titleStyle = dotStyleProperties?.['title-style'] || {};
    const textAlignment = dotStyleProperties?.['text-alignment'];
    const overlayStyle = dotStyleProperties?.['overlay-style'] || 'none';
    const buttonColor = dotStyleProperties?.['button-color'];
    const buttonSize = dotStyleProperties?.['button-size'];
    const buttonStyle = dotStyleProperties?.['button-style'] || {};

    const titleClasses = cn(
        'max-w-3xl font-display text-[clamp(2.25rem,1.4rem+3.6vw,4.5rem)] font-semibold leading-[1.03] tracking-tight text-shadow',
        titleStyle.italic && 'italic',
        titleStyle.underline && 'underline'
    );

    // CTA defaults to the warm brand accent; an editor-set `button-color` wins.
    const colorKey = variant(buttonColor, BUTTON_COLORS);
    const buttonClasses = cn(
        'inline-flex items-center gap-2 font-semibold text-bg shadow-lg transition-transform duration-300 ease-(--ease-out-quart) hover:-translate-y-0.5',
        colorKey ? buttonColorVariants({ color: colorKey }) : 'bg-accent hover:bg-accent/90',
        buttonSizeVariants({ size: variant(buttonSize, BUTTON_SIZES) }),
        buttonStyle.rounded ? 'rounded-lg' : buttonStyle['full-rounded'] ? 'rounded-full' : 'rounded-xl'
    );

    return (
        <section className="relative isolate overflow-hidden rounded-3xl bg-surface-2 [height:clamp(26rem,60vh,40rem)]">
            {image?.identifier && (
                <Image
                    src={image.identifier}
                    fill
                    priority
                    sizes="(min-width: 1280px) 1216px, 100vw"
                    className="object-cover motion-safe:animate-[heroIn_1.2s_var(--ease-out-expo)_both]"
                    alt={title || 'Featured destination'}
                />
            )}

            {/* Always-on scrim guarantees text contrast over any photo. */}
            <div
                aria-hidden="true"
                className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/25 to-black/10"
            />
            <div aria-hidden="true" className={overlayVariants({ style: variant(overlayStyle, OVERLAYS) })} />

            <div className={contentVariants({ alignment: variant(textAlignment, ALIGNMENTS) })}>
                <h1 className={titleClasses}>
                    <DotCMSEditableText contentlet={contentlet} fieldName="title" />
                </h1>
                {caption && (
                    <p className="max-w-xl text-base text-bg/90 text-shadow sm:text-lg">
                        {caption}
                    </p>
                )}
                {link && (
                    <Link className={buttonClasses} href={link}>
                        {buttonText || 'Explore'}
                        <span aria-hidden="true">→</span>
                    </Link>
                )}
            </div>
        </section>
    );
}

export default Banner;
