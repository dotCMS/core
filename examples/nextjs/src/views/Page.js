"use client";

import { DotCMSLayoutBody, useEditableDotCMSPage, useStyleEditorSchemas } from "@dotcms/react";

import { pageComponents } from "@/components/content-types";
import Footer from "@/components/footer/Footer";
import Header from "@/components/header/Header";
import { ACTIVITY_SCHEMA, BANNER_SCHEMA } from "@/utils/styleEditorSchemas";

import { useEffect } from 'react';

export function IframeHeightBridge() {
  useEffect(() => {
    const send = () => {
      const height = Math.max(
        document.body?.scrollHeight ?? 0,
        document.documentElement?.scrollHeight ?? 0
      );

      window.parent.postMessage(
        { name: 'dotcms:iframeHeight', payload: { height } },
        '*'
      );
    };

    send();

    const ro = new ResizeObserver(send);
    ro.observe(document.documentElement);
    if (document.body) ro.observe(document.body);

    window.addEventListener('load', send);
    window.addEventListener('resize', send);

    return () => {
      ro.disconnect();
      window.removeEventListener('load', send);
      window.removeEventListener('resize', send);
    };
  }, []);

  return null;
}

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
            <IframeHeightBridge />
        </div>
    );
}
