/** @type {import('tailwindcss').Config} */
module.exports = {
    content: [
        './src/pages/**/*.{js,ts,jsx,tsx,mdx}',
        './src/components/**/*.{js,ts,jsx,tsx,mdx}',
        './src/lib/**/*.{js,ts,jsx,tsx,mdx}',
        './src/app/**/*.{js,ts,jsx,tsx,mdx}'
    ],
    safelist: ['container', 'space-y-4'],
    theme: {
        container: {
            center: true,
        },
    },
    plugins: [
        require('@tailwindcss/typography'),
    ],
};
