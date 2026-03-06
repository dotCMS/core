"use client";

import { DotCMSLayoutBody, useEditableDotCMSPage, useStyleEditorSchemas } from "@dotcms/react";

import { pageComponents } from "@/components/content-types";
import Footer from "@/components/footer/Footer";
import Header from "@/components/header/Header";
import { ACTIVITY_SCHEMA, BANNER_SCHEMA } from "@/utils/styleEditorSchemas";

export function Page({ pageContent }) {
    const { pageAsset, content = {} } = useEditableDotCMSPage(pageContent);
    const navigation = content.navigation;

    useStyleEditorSchemas([ACTIVITY_SCHEMA, BANNER_SCHEMA])

    return (
        <div className="flex flex-col gap-6 min-h-screen bg-slate-50">
            {pageAsset?.layout.header && (
                <Header navItems={navigation?.children} />
            )}

            <main className="container m-auto">
                <DotCMSLayoutBody
                    page={pageAsset}
                    components={pageComponents}
                    mode={process.env.NEXT_PUBLIC_DOTCMS_MODE}
                />
            </main>

            {pageAsset?.layout.footer && <Footer {...content} />}
        </div>
    );
}
