/** @type {import('tailwindcss').Config} */
//
// Tailwind CSS configuration — LeaseDemo
//
// Tailwind owns layout/spacing/sizing/alignment/responsive/simple visual
// utilities. It deliberately does NOT re-declare the LeaseDemo color
// palette as raw hex values; instead every color utility below maps to the
// CSS custom properties emitted from the single canonical source of truth,
// src/styles/_tokens.scss (see that file for the full rationale). Changing
// a brand color only ever requires editing _tokens.scss.
//
// `preflight` is disabled: Angular Material ships its own well-tested base
// styles/resets for its components, and running both reset layers risked
// visible conflicts (e.g. default button/table appearance). LeaseDemo's own
// minimal reset lives in src/styles.scss instead.
module.exports = {
  content: ['./src/**/*.{html,ts}'],
  corePlugins: {
    preflight: false,
  },
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: 'var(--ld-color-primary)',
          hover: 'var(--ld-color-primary-hover)',
        },
        accent: 'var(--ld-color-accent)',
        surface: {
          DEFAULT: 'var(--ld-color-surface)',
          muted: 'var(--ld-color-surface-muted)',
        },
        border: 'var(--ld-color-border)',
        text: {
          primary: 'var(--ld-color-text-primary)',
          secondary: 'var(--ld-color-text-secondary)',
          'on-primary': 'var(--ld-color-text-on-primary)',
        },
        success: 'var(--ld-color-success)',
        warning: 'var(--ld-color-warning)',
        danger: {
          DEFAULT: 'var(--ld-color-danger)',
          hover: 'var(--ld-color-danger-hover)',
        },
      },
      borderRadius: {
        sm: 'var(--ld-radius-sm)',
        md: 'var(--ld-radius-md)',
      },
      boxShadow: {
        sm: 'var(--ld-shadow-sm)',
        md: 'var(--ld-shadow-md)',
      },
      fontFamily: {
        sans: 'var(--ld-font-family)',
      },
    },
  },
  plugins: [],
};

