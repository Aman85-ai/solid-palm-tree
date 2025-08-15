import express from 'express'
import helmet from 'helmet'
import rateLimit from 'express-rate-limit'
import multer from 'multer'
import { nanoid } from 'nanoid'
import fs from 'fs'
import path from 'path'
import os from 'os'
import process from 'process'
import puppeteer from 'puppeteer'

const app = express()
app.disable('x-powered-by')
app.use(helmet())
app.use(express.json({ limit: '1mb' }))
app.use(express.urlencoded({ extended: false }))

const limiter = rateLimit({ windowMs: 60_000, max: 30 })
app.use(limiter)

const upload = multer({
	storage: multer.memoryStorage(),
	limits: { fileSize: 1_000_000 },
})

const store = new Map()
const TTL_MS = 5 * 60_000

function putTemp(buf) {
	const id = nanoid()
	const expiry = Date.now() + TTL_MS
	store.set(id, { buf, expiry })
	return id
}

setInterval(() => {
	const now = Date.now()
	for (const [id, v] of store) {
		if (v.expiry < now) store.delete(id)
	}
}, 30_000)

async function htmlToPdf(html) {
	const browser = await puppeteer.launch({
		headless: 'new',
		args: ['--no-sandbox', '--disable-setuid-sandbox']
	})
	try {
		const page = await browser.newPage()
		await page.setContent(html, { waitUntil: 'networkidle0' })
		const pdfBuffer = await page.pdf({
			format: 'A4',
			printBackground: true,
			preferCSSPageSize: true
		})
		return pdfBuffer
	} finally {
		await browser.close()
	}
}

app.post('/convert', upload.none(), async (req, res) => {
	try {
		const html = req.body.html
		if (!html || typeof html !== 'string') return res.status(400).json({ error: 'Missing html' })
		if (html.length > 200_000) return res.status(413).json({ error: 'HTML too large' })
		const pdf = await htmlToPdf(html)
		if (req.query.link === '1') {
			const id = putTemp(pdf)
			return res.json({ id, url: `/download/${id}`, expiresInSeconds: TTL_MS / 1000 })
		}
		res.setHeader('Content-Type', 'application/pdf')
		res.setHeader('Content-Disposition', 'attachment; filename="html.pdf"')
		return res.send(pdf)
	} catch (e) {
		return res.status(500).json({ error: e.message })
	}
})

app.get('/download/:id', (req, res) => {
	const entry = store.get(req.params.id)
	if (!entry) return res.status(404).json({ error: 'Not found or expired' })
	res.setHeader('Content-Type', 'application/pdf')
	res.setHeader('Content-Disposition', 'attachment; filename="html.pdf"')
	return res.send(entry.buf)
})

const PORT = process.env.PORT || 3000
app.listen(PORT, () => console.log(`html2pdf-server listening on ${PORT}`))