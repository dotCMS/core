"use client";

import { editContentlet } from "@dotcms/uve";
import type { DotCMSBasicContentlet } from "@dotcms/types";
import { useIsEditMode } from "@/hooks/useIsEditMode";

/**
 * `editContentlet` expects a full `DotCMSBasicContentlet`, but our GraphQL
 * fragment types (Blog, Destination) only select a subset of fields. The
 * contentlet is identified by its `identifier`/`inode`, which the fragments do
 * include, so we accept a partial here and widen at the single SDK boundary.
 */
export type EditableContentlet = Partial<DotCMSBasicContentlet> &
    Pick<DotCMSBasicContentlet, "identifier">;

interface EditButtonProps {
    contentlet: EditableContentlet;
}

export function EditButton({ contentlet }: EditButtonProps) {
    const isEditMode = useIsEditMode();
    return (
        isEditMode && (
            <button
                onClick={() =>
                    editContentlet(contentlet as DotCMSBasicContentlet)
                }
                className="absolute bottom-2 right-2 z-10 bg-blue-500 cursor-pointer text-white rounded-md py-1 px-3 text-sm shadow-md hover:bg-blue-600"
            >
                Edit
            </button>
        )
    );
}
