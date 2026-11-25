# Backend Midtrans Sandbox — Lapar.id

Server Node.js/Express ini membuat Snap Token Midtrans Sandbox dan memverifikasi webhook Midtrans (SHA-512 signature key) sebelum meneruskan pesanan ke penjual, mengurangi stok produk via transaksi Firestore, dan menambahkan Lapar Points.

## Langkah Konfigurasi

1. Daftar di [Midtrans Dashboard Sandbox](https://dashboard.sandbox.midtrans.com/) dan salin **Server Key** serta **Client Key** Sandbox Anda.
2. Masuk ke folder `backend/` dan instal dependensi:
   ```bash
   cd backend
   npm install
   cp .env.example .env
   ```
3. Isi `.env` dengan `MIDTRANS_SERVER_KEY`, `MIDTRANS_CLIENT_KEY`, dan kredensial Firebase Service Account (`GOOGLE_APPLICATION_CREDENTIALS`).
4. Jalankan server:
   ```bash
   npm start
   ```
5. Daftarkan URL publik backend Anda di **Midtrans Dashboard -> Settings -> Payment -> Notification URL**:
   `https://<domain-backend-anda>/api/payments/midtrans-webhook`
6. Masukkan URL backend tersebut ke **Secrets panel di AI Studio** dengan key `MIDTRANS_BACKEND_URL`.
