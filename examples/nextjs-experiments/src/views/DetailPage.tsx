'use client';

import Image from 'next/image';

import { enableBlockEditorInline } from '@dotcms/uve';
import {
    DotCMSBlockEditorRenderer,
    DotCMSEditableText,
    useEditableDotCMSPage
} from '@dotcms/react';
import type { CustomRenderer, CustomRendererProps } from '@dotcms/react';
import type { BlockEditorNode, DotCMSBasicContentlet } from '@dotcms/types';

import { useIsEditMode } from '@/hooks/useIsEditMode';
import Footer from '@/components/footer/Footer';
import Header from '@/components/header/Header';
import type { PageExtraContent } from '@/types/content';

interface DetailPageProps {
    pageContent: Parameters<typeof useEditableDotCMSPage>[0];
}

/** Fields read out of `node.attrs.data` by the block-editor custom renderers. */
interface ActivityRendererData {
    title?: string;
    description?: string;
    shortDescription?: string;
    contentType?: string;
}

export function DetailPage({ pageContent }: DetailPageProps) {
    const { pageAsset, content = {} } = useEditableDotCMSPage(pageContent);
    const urlContentMap = pageAsset?.urlContentMap as
        | (DotCMSBasicContentlet & { blogContent?: BlockEditorNode })
        | undefined;
    const blogContent = urlContentMap?.blogContent as BlockEditorNode;
    const pageContentData = content as PageExtraContent;
    const navigation = pageContentData.navigation;
    const isEditMode = useIsEditMode();

    // Derived from edit mode — no effect/state needed: in the UVE we add an
    // outline + pointer cursor to signal the block editor is clickable.
    const blockEditorClasses = [
        'prose lg:prose-xl prose-a:text-blue-600',
        isEditMode ? 'border-2 border-solid border-cyan-400 cursor-pointer' : ''
    ]
        .filter(Boolean)
        .join(' ');

    const handleClick = () => {
        if (isEditMode && urlContentMap) {
            enableBlockEditorInline(urlContentMap, 'blogContent');
        }
    };

    return (
        <div className="flex flex-col gap-6 bg-slate-50">
            {pageAsset?.layout.header && <Header navItems={navigation?.children} />}
            <main className="flex flex-col gap-8 m-auto">
                <h1 className="text-4xl font-bold">
                    {urlContentMap && (
                        <DotCMSEditableText contentlet={urlContentMap} fieldName="title" />
                    )}
                </h1>

                {urlContentMap?.image?.identifier && (
                    <div className="relative w-full h-80 overflow-hidden">
                        <Image
                            className="object-cover"
                            src={urlContentMap?.image?.identifier}
                            fill={true}
                            alt="Activity Image"
                        />
                    </div>
                )}

                <div onClick={handleClick}>
                    <DotCMSBlockEditorRenderer
                        blocks={blogContent}
                        className={blockEditorClasses}
                        customRenderers={customRenderers}
                    />
                </div>
            </main>

            {pageAsset?.layout.footer && (
                <Footer
                    blogs={pageContentData.blogs}
                    destinations={pageContentData.destinations}
                />
            )}
        </div>
    );
}

const customRenderers: CustomRenderer = {
    Activity: (props: CustomRendererProps<ActivityRendererData>) => {
        const { title, description, contentType } = props.node.attrs?.data || {};

        return (
            <div className="p-6 mb-4 overflow-hidden rounded-2xl bg-white shadow-lg">
                <h2 className="text-2xl font-bold">{title}</h2>
                <p className="line-clamp-2">{description}</p>
                <p className="text-sm text-cyan-700">{contentType}</p>
            </div>
        );
    },
    Product: (props: CustomRendererProps<ActivityRendererData>) => {
        const { title, description, contentType } = props.node.attrs?.data || {};

        return (
            <div className="p-6 mb-4 overflow-hidden rounded-2xl bg-white shadow-lg">
                <h2 className="text-2xl font-bold">{title}</h2>
                <div className="line-clamp-2" dangerouslySetInnerHTML={{ __html: description ?? '' }} />
                <p className="text-sm text-blue-500">{contentType}</p>
            </div>
        );
    },
    Destination: (props: CustomRendererProps<ActivityRendererData>) => {
        const { title, shortDescription, contentType } = props.node.attrs?.data || {};

        return (
            <div className="p-6 mb-4 rounded-2xl bg-white shadow-lg">
                <h2 className="text-2xl font-bold">{title}</h2>
                <p className="line-clamp-2">{shortDescription}</p>
                <p className="text-sm text-indigo-700">{contentType}</p>
            </div>
        );
    }
};
