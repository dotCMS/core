import { defineStyleEditorSchema, styleEditorField } from "@dotcms/uve";

export const ACTIVITY_SCHEMA = defineStyleEditorSchema({
    contentType: 'Activity',
    sections: [
        {
            title: 'Typography',
            fields: [
                styleEditorField.dropdown({
                    id: 'title-size',
                    label: 'Title Size',
                    options: [
                        { label: 'Small', value: 'text-lg' },
                        { label: 'Medium', value: 'text-xl' },
                        { label: 'Large', value: 'text-2xl' },
                        { label: 'Extra Large', value: 'text-3xl' },
                    ]
                }),
                styleEditorField.dropdown({
                    id: 'description-size',
                    label: 'Description Size',
                    options: [
                        { label: 'Small', value: 'text-sm' },
                        { label: 'Medium', value: 'text-base' },
                        { label: 'Large', value: 'text-lg' },
                    ]
                }),
                styleEditorField.checkboxGroup({
                    id: 'title-style',
                    label: 'Title Style',
                    options: [
                        { label: 'Bold', key: 'bold' },
                        { label: 'Italic', key: 'italic' },
                        { label: 'Underline', key: 'underline' },
                    ]
                }),
            ]
        },
        {
            title: 'Layout',
            fields: [
                styleEditorField.radio({
                    id: 'layout',
                    label: 'Layout',
                    columns: 2,
                    options: [
                        {
                            label: 'Left',
                            value: 'left',
                            imageURL: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSphn5CRr3MrQUjWWH7ByHWW-lROnVQl4XxYQ&s',
                        },
                        {
                            label: 'Right',
                            value: 'right',
                            imageURL: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSphn5CRr3MrQUjWWH7ByHWW-lROnVQl4XxYQ&s'
                        },
                        {
                            label: 'Center',
                            value: 'center',
                            imageURL: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSphn5CRr3MrQUjWWH7ByHWW-lROnVQl4XxYQ&s'
                        },
                        {
                            label: 'Overlap',
                            value: 'overlap',
                            imageURL: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSphn5CRr3MrQUjWWH7ByHWW-lROnVQl4XxYQ&s'
                        },
                    ],
                }),
                styleEditorField.dropdown({
                    id: 'image-height',
                    label: 'Image Height',
                    options: [
                        { label: 'Small', value: 'h-40' },
                        { label: 'Medium', value: 'h-56' },
                        { label: 'Large', value: 'h-72' },
                        { label: 'Extra Large', value: 'h-96' },
                    ]
                }),
            ]
        },
        {
            title: 'Card Style',
            fields: [
                styleEditorField.radio({
                    id: 'card-background',
                    label: 'Card Background',
                    columns: 2,
                    options: [
                        {
                            label: 'White',
                            value: 'white',
                        },
                        {
                            label: 'Gray',
                            value: 'gray',
                        },
                        {
                            label: 'Light Blue',
                            value: 'light-blue',
                        },
                        {
                            label: 'Light Green',
                            value: 'light-green',
                        },
                    ],
                }),
                styleEditorField.radio({
                    id: 'border-radius',
                    label: 'Border Radius',
                    columns: 2,
                    options: [
                        {
                            label: 'None',
                            value: 'none',
                        },
                        {
                            label: 'Small',
                            value: 'small',
                        },
                        {
                            label: 'Medium',
                            value: 'medium',
                        },
                        {
                            label: 'Large',
                            value: 'large',
                        },
                    ],
                }),
                styleEditorField.checkboxGroup({
                    id: 'card-effects',
                    label: 'Card Effects',
                    options: [
                        { label: 'Shadow', key: 'shadow' },
                        { label: 'Border', key: 'border' },
                    ]
                }),
            ]
        },
        {
            title: 'Button',
            fields: [
                styleEditorField.radio({
                    id: 'button-color',
                    label: 'Button Color',
                    columns: 2,
                    options: [
                        {
                            label: 'Blue',
                            value: 'blue',
                        },
                        {
                            label: 'Green',
                            value: 'green',
                        },
                        {
                            label: 'Red',
                            value: 'red',
                        },
                        {
                            label: 'Purple',
                            value: 'purple',
                        },
                        {
                            label: 'Orange',
                            value: 'orange',
                        },
                        {
                            label: 'Teal',
                            value: 'teal',
                        },
                    ],
                }),
                styleEditorField.dropdown({
                    id: 'button-size',
                    label: 'Button Size',
                    options: [
                        { label: 'Small', value: 'small' },
                        { label: 'Medium', value: 'medium' },
                        { label: 'Large', value: 'large' },
                    ],
                }),
                styleEditorField.checkboxGroup({
                    id: 'button-style',
                    label: 'Button Style',
                    options: [
                        { label: 'Rounded', key: 'rounded' },
                        { label: 'Full Rounded', key: 'full-rounded' },
                        { label: 'Shadow', key: 'shadow' },
                    ],
                }),
            ]
        },
    ]
})

export const BANNER_SCHEMA = defineStyleEditorSchema({
    contentType: 'Banner',
    sections: [
        {
            title: 'Typography',
            fields: [
                styleEditorField.dropdown({
                    id: 'title-size',
                    label: 'Title Size',
                    options: [
                        { label: 'Small', value: 'text-4xl' },
                        { label: 'Medium', value: 'text-5xl' },
                        { label: 'Large', value: 'text-6xl' },
                        { label: 'Extra Large', value: 'text-7xl' },
                    ],
                }),
                styleEditorField.dropdown({
                    id: 'caption-size',
                    label: 'Caption Size',
                    options: [
                        { label: 'Small', value: 'text-base' },
                        { label: 'Medium', value: 'text-lg' },
                        { label: 'Large', value: 'text-xl' },
                        { label: 'Extra Large', value: 'text-2xl' },
                    ],
                }),
                styleEditorField.checkboxGroup({
                    id: 'title-style',
                    label: 'Title Style',
                    options: [
                        { label: 'Bold', key: 'bold' },
                        { label: 'Italic', key: 'italic' },
                        { label: 'Underline', key: 'underline' },
                    ]
                }),
            ]
        },
        {
            title: 'Layout',
            fields: [
                styleEditorField.radio({
                    id: 'text-alignment',
                    label: 'Text Alignment',
                    columns: 2,
                    options: [
                        {
                            label: 'Left',
                            value: 'left',
                        },
                        {
                            label: 'Center',
                            value: 'center',
                        },
                        {
                            label: 'Right',
                            value: 'right',
                        },
                    ],
                }),
                styleEditorField.radio({
                    id: 'overlay-style',
                    label: 'Overlay Style',
                    columns: 2,
                    options: [
                        {
                            label: 'None',
                            value: 'none',
                        },
                        {
                            label: 'Dark',
                            value: 'dark',
                        },
                        {
                            label: 'Light',
                            value: 'light',
                        },
                        {
                            label: 'Gradient',
                            value: 'gradient',
                        },
                    ],
                }),
            ]
        },
        {
            title: 'Button',
            fields: [
                styleEditorField.radio({
                    id: 'button-color',
                    label: 'Button Color',
                    columns: 2,
                    options: [
                        {
                            label: 'Blue',
                            value: 'blue',
                        },
                        {
                            label: 'Green',
                            value: 'green',
                        },
                        {
                            label: 'Red',
                            value: 'red',
                        },
                        {
                            label: 'Purple',
                            value: 'purple',
                        },
                        {
                            label: 'Orange',
                            value: 'orange',
                        },
                        {
                            label: 'Teal',
                            value: 'teal',
                        },
                    ],
                }),
                styleEditorField.dropdown({
                    id: 'button-size',
                    label: 'Button Size',
                    options: [
                        { label: 'Small', value: 'small' },
                        { label: 'Medium', value: 'medium' },
                        { label: 'Large', value: 'large' },
                    ],
                }),
                styleEditorField.checkboxGroup({
                    id: 'button-style',
                    label: 'Button Style',
                    options: [
                        { label: 'Rounded', key: 'rounded' },
                        { label: 'Full Rounded', key: 'full-rounded' },
                        { label: 'Shadow', key: 'shadow' },
                    ],
                }),
            ]
        },
    ]
})
