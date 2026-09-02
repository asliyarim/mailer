import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// base: Uygulama Odyssey kabugunun ICINDE /mailer/ alt yolundan servis
// ediliyor (bkz. Odyssey nginx.conf) - varlik adresleri de bu onekle
// uretilmeli. Derleme sirasinda VITE_BASE_PATH verilir; verilmezse "/" kalir
// ki yerel "npm run dev" ve dogrudan kok erisimi bozulmasin.
// React Router da ayni degeri import.meta.env.BASE_URL uzerinden okur.
export default defineConfig({
  base: process.env.VITE_BASE_PATH || '/',
  plugins: [react()],
  // "npm run dev" icin: production'da Odyssey'in nginx'i /api/auth/* ile
  // /api/mailer/* uzerini FARKLI backend'lere (odyssey-auth 8081 /
  // aksa-mailer 8082) AYNI origin uzerinden proxy'ler. Yerelde bu yoksa
  // /api/auth/me cagrisi yanlislikla mailer backend'ine gider ve 401 doner.
  server: {
    proxy: {
      // Ikisi de .env'deki portlarla degistirilebilir. Varsayilanlar
      // .env.example'daki degerler; portlari degistirdiyseniz (ornegin
      // Capacity Planner yigini ayaktayken) burayi da soylemeniz gerekir,
      // yoksa dev sunucusunda oturum surekli duser (bkz. docs/kurulum.md).
      // URETIMI ETKILEMEZ: orada istekleri Odyssey'in nginx'i proxy'ler.
      '/api/auth': { target: process.env.VITE_AUTH_BASE_URL || 'http://localhost:8081', changeOrigin: true },
      '/api/mailer': { target: process.env.VITE_MAILER_BASE_URL || 'http://localhost:8082', changeOrigin: true },
    },
  },
})
