"use client";
import { useMemo } from "react";
import { isInsideEditor } from "@dotcms/client";

function Contentlet({ contentlet, children }) {
    const insideEditor = useMemo(isInsideEditor, []);

    return insideEditor ? (
        <div
            data-dot-object="contentlet"
            data-dot-identifier={contentlet.identifier}
            data-dot-basetype={contentlet.baseType}
            data-dot-title={contentlet.widgetTitle || contentlet.title}
            data-dot-inode={contentlet.inode}
            data-dot-type={contentlet.contentType}
            data-dot-on-number-of-pages={contentlet.onNumberOfPages ?? 0}
            key={contentlet.identifier}
        >
            {children}
        </div>
    ) : (
        children
    );
}

export default Contentlet;
