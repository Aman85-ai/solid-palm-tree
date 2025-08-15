# html2pdf-server (optional)

Express + Puppeteer service for HTML→PDF. Use only if you need server-side conversion. The Android app works offline via WebView print API.

- POST /convert (body: `html` string, 1MB limit). Returns PDF or JSON with temporary link when `?link=1`.
- GET /download/:id serves a temporary PDF (expires in 5 minutes).

Security
- Rate-limited, size-limited, no storage at rest (in-memory only), ephemeral links.
- Run behind HTTPS (e.g., Nginx/Cloudflare). Or terminate TLS directly in Node with `http2`/certs.

Run locally
```bash
npm i
npm run start
# or: PORT=8080 npm run start
```

Docker (example)
```bash
# simple Dockerfile (multi-arch) not included; use official Puppeteer image
# docker run -e PORT=3000 -p 3000:3000 ghcr.io/puppeteer/puppeteer:22.15.0 node server.js
```

Notes
- Puppeteer requires Chromium; the official image bundles it.
- For large/remote assets, ensure servers allow headless Chrome fetching.