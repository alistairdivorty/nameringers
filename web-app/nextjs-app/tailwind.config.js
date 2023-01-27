const { fontFamily } = require('tailwindcss/defaultTheme');

/** @type {import('tailwindcss').Config} */
module.exports = {
    content: [
        './pages/**/*.{js,ts,jsx,tsx}',
        './components/**/*.{js,ts,jsx,tsx}',
    ],
    theme: {
        extend: {
            fontFamily: {
                londrina: ['var(--font-londrina)', ...fontFamily.sans],
                noto: ['var(--font-noto)', ...fontFamily.sans],
            },
            borderRadius: {
                large: '14px',
            },
        },
    },
    plugins: [],
};
