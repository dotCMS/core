import { useIsEditMode } from "@/hooks/useIsEditMode";
import ContactUs from "../forms/ContactUs";

import type { ContentTypeProps } from "@/types/content";

type PageFormProps = ContentTypeProps & {
    formType?: string;
    description?: string;
};

export default function PageForm(contentlet: PageFormProps) {
    const { formType } = contentlet;
    const isEditMode = useIsEditMode();

    if (formType === "contact-us") {
        return <ContactUs {...contentlet} />;
    }

    if (isEditMode) {
        return (
            <div>
                <h4>
                    There is no form component for this form type: {formType}
                </h4>
            </div>
        );
    }

    return null;
}
