/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],

  theme: {
    extend: {
      colors: {
        background: "#0B0910",

        surface: "#121018",
        "surface-elevated": "#191520",
        "surface-interactive": "#211B2A",

        border: "#2A2433",
        "border-strong": "#3A3047",
        "border-accent": "#4C3472",

        "text-primary": "#F5F3F7",
        "text-secondary": "#B9B3C2",
        "text-muted": "#81798B",

        accent: "#8B5CF6",
        "accent-hover": "#9D74F8",
        "accent-strong": "#7C3AED",
        "accent-subtle": "#211832",

        success: "#4FBF8B",
        warning: "#D7A84D",
        danger: "#D96B7A",

        "taste-positive": "#9B72F2",
        "taste-negative": "#C46B82",
        "taste-contextual": "#7478D8",
      },

      fontFamily: {
        sans: ["Manrope", "sans-serif"],
      },

      borderRadius: {
        sm: "6px",
        md: "8px",
        lg: "12px",
        xl: "16px",
        "2xl": "20px",

        control: "8px",
        card: "12px",
        "card-prominent": "16px",
        modal: "20px",
      },

      fontSize: {
        display: [
          "36px",
          {
            lineHeight: "1.10",
            fontWeight: "600",
            letterSpacing: "-0.02em",
          },
        ],

        "page-title": [
          "28px",
          {
            lineHeight: "1.20",
            fontWeight: "600",
            letterSpacing: "-0.01em",
          },
        ],

        "section-title": [
          "22px",
          {
            lineHeight: "1.25",
            fontWeight: "600",
            letterSpacing: "-0.01em",
          },
        ],

        "card-title": [
          "18px",
          {
            lineHeight: "1.30",
            fontWeight: "600",
          },
        ],

        subheading: [
          "16px",
          {
            lineHeight: "1.40",
            fontWeight: "600",
          },
        ],

        body: [
          "16px",
          {
            lineHeight: "1.60",
            fontWeight: "400",
          },
        ],

        "body-sm": [
          "14px",
          {
            lineHeight: "1.50",
            fontWeight: "400",
          },
        ],

        label: [
          "14px",
          {
            lineHeight: "1.35",
            fontWeight: "600",
          },
        ],

        metadata: [
          "13px",
          {
            lineHeight: "1.40",
            fontWeight: "500",
          },
        ],

        caption: [
          "12px",
          {
            lineHeight: "1.40",
            fontWeight: "500",
          },
        ],
      },
    },
  },

  plugins: [],
}