import '@mdi/font/css/materialdesignicons.css'
import 'vuetify/styles'
import { createVuetify } from 'vuetify'

export default createVuetify({
  theme: {
    defaultTheme: 'moviTbd',
    themes: {
      moviTbd: {
        dark: false,
        colors: {
          primary: '#00B2A9',
          secondary: '#F68B1F',
          tertiary: '#333F48',
          background: '#F5F7F8',
          surface: '#FFFFFF',
          error: '#B3261E',
        },
      },
    },
  },
  defaults: {
    VBtn: { rounded: 'lg', style: 'text-transform: none' },
    VCard: { rounded: 'xl' },
    VTextField: { variant: 'outlined', density: 'comfortable', hideDetails: 'auto' },
    VSelect: { variant: 'outlined', density: 'comfortable', hideDetails: 'auto' },
  },
})
