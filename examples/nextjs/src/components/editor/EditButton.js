
import { editContentlet } from "@dotcms/uve";
import { useIsEditMode } from "@/hooks/isEditMode";

export function EditButton({ contentlet }) {
    const isEditMode = useIsEditMode();
    return (
        isEditMode && (
            <button
                onClick={() => editContentlet(contentlet)}
                className="absolute top-2 right-2 z-10 bg-blue-500 text-white rounded-md py-2 px-4 shadow-md hover:bg-blue-600"
            >
                Edit
            </button>
        )
    );
};
