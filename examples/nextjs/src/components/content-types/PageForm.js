import { useIsEditMode } from "@/hooks/isEditMode";
import ContactUs from "../forms/ContactUs";

export default function PageForm(contentlet) {
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
