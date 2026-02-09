import type { DotCMSBasicContentlet } from "@dotcms/types";
import { editContentlet } from "@dotcms/uve";
import { useIsEditMode } from "@/hooks";

export function EditButton({
  contentlet,
}: {
  contentlet: DotCMSBasicContentlet;
}) {
  const isEditMode = useIsEditMode();
  return (
    isEditMode && (
      <button
        onClick={() => editContentlet(contentlet)}
        className="absolute bottom-2 right-2 z-10 bg-violet-800 cursor-pointer text-white rounded-md py-1 px-3 text-sm shadow-md hover:bg-blue-600"
      >
        Edit
      </button>
    )
  );
}
