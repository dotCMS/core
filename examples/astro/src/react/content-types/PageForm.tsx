import type { DotCMSBasicContentlet } from "@dotcms/types";

interface PageFormProps extends DotCMSBasicContentlet {
    formType: string;
}

export default function PageForm(contentlet: PageFormProps) {
    const { formType } = contentlet;
    const isEditMode = false; // useIsEditMode();

    if (formType === "contact-us") {
        // return <ContactUs {...contentlet} />;
        return <div>Contact Us</div>;
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
