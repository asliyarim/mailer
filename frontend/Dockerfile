# --- build stage ---
FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm install

COPY . .
# Odyssey ile AYNI origin uzerinden calisiyoruz - API adresi GORECELI olmali.
# Mutlak URL verilirse tarayici istegi cross-site sayar ve oturum cerezi
# gonderilmez (bkz. docs/BRIEF.md, "Kritik tasarim karari: ayni origin").
ARG VITE_API_BASE_URL=""
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
# Kabuk bizi /mailer/ onekinden servis ediyor - Vite varlik adreslerini
# bu onekle uretsin (bkz. vite.config.js).
ARG VITE_BASE_PATH=/mailer/
ENV VITE_BASE_PATH=${VITE_BASE_PATH}
# Oturum yoksa kullanicinin geri gonderilecegi giris adresi (Odyssey).
ARG VITE_ODYSSEY_URL=/
ENV VITE_ODYSSEY_URL=${VITE_ODYSSEY_URL}
RUN npm run build

# --- run stage ---
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
# nginx'in resmi entrypoint script'i /etc/nginx/templates/*.template
# dosyalarini baslangicta envsubst ile isleyip conf.d/'ye yazar.
COPY nginx.conf /etc/nginx/templates/default.conf.template

EXPOSE 80
