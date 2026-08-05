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
                type="button"
                onClick={() =>
                    editContentlet(contentlet as DotCMSBasicContentlet)
                }
                className="absolute bottom-3 right-3 z-(--z-dropdown) cursor-pointer rounded-full bg-primary px-3.5 py-1.5 text-sm font-semibold text-bg shadow-md transition-colors hover:bg-primary-deep"
            >
                Edit
            </button>
        )
    );
}
