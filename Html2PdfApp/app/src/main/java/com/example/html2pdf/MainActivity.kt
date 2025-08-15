package com.example.html2pdf

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.nio.charset.Charset
import java.util.UUID

class MainActivity : AppCompatActivity() {
	private lateinit var webView: WebView
	private lateinit var htmlInput: EditText
	private lateinit var btnPreview: Button
	private lateinit var btnConvert: Button
	private lateinit var btnLoadFile: Button
	private lateinit var btnSave: Button
	private lateinit var btnShare: Button
	private lateinit var pdfPreview: ImageView

	private var lastGeneratedPdf: File? = null
	private var pendingSaveSource: File? = null

	private val filePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
		uri?.let { loadHtmlFromUri(it) }
	}

	private val createDocLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri: Uri? ->
		val source = pendingSaveSource
		if (uri != null && source != null && source.exists()) {
			try {
				contentResolver.openOutputStream(uri)?.use { out ->
					FileInputStream(source).use { input -> input.copyTo(out) }
				}
				Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
			} catch (e: Exception) {
				Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
			}
		}
		pendingSaveSource = null
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		htmlInput = findViewById(R.id.htmlInput)
		webView = findViewById(R.id.webView)
		btnPreview = findViewById(R.id.btnPreview)
		btnConvert = findViewById(R.id.btnConvert)
		btnLoadFile = findViewById(R.id.btnLoadFile)
		btnSave = findViewById(R.id.btnSave)
		btnShare = findViewById(R.id.btnShare)
		pdfPreview = findViewById(R.id.pdfPreview)

		setupWebView()

		btnPreview.setOnClickListener { loadPreviewFromInput() }
		btnConvert.setOnClickListener { generatePdf { openPdf(it) } }
		btnLoadFile.setOnClickListener { openHtmlFilePicker() }
		btnSave.setOnClickListener { ensurePdfThen { savePdf(it) } }
		btnShare.setOnClickListener { ensurePdfThen { sharePdf(it) } }

		htmlInput.setText(SAMPLE_HTML)
		loadPreviewFromInput()
	}

	private fun setupWebView() {
		webView.settings.javaScriptEnabled = true
		webView.settings.domStorageEnabled = true
		webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
		webView.settings.allowFileAccess = false
		webView.settings.allowContentAccess = true
		webView.webChromeClient = WebChromeClient()
		webView.webViewClient = object : WebViewClient() {
			override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
		}
	}

	private fun loadPreviewFromInput() {
		var html = htmlInput.text?.toString() ?: ""
		if (html.length > MAX_HTML_CHARS) {
			Toast.makeText(this, "HTML truncated to ${MAX_HTML_CHARS} chars for safety", Toast.LENGTH_LONG).show()
			html = html.substring(0, MAX_HTML_CHARS)
			htmlInput.setText(html)
		}
		webView.loadDataWithBaseURL(
			"https://appassets.androidplatform.net/assets/",
			html,
			"text/html",
			"UTF-8",
			null
		)
	}

	private fun openHtmlFilePicker() {
		filePicker.launch(arrayOf("text/html", "application/xhtml+xml"))
	}

	private fun loadHtmlFromUri(uri: Uri) {
		try {
			contentResolver.openInputStream(uri)?.use { input ->
				val bytes = input.readBytes()
				if (bytes.size > MAX_HTML_BYTES) {
					Toast.makeText(this, "File too large (>${MAX_HTML_BYTES/1024}KB)", Toast.LENGTH_LONG).show()
					return
				}
				val html = bytes.toString(Charset.forName("UTF-8"))
				htmlInput.setText(html)
				loadPreviewFromInput()
			}
		} catch (e: Exception) {
			Toast.makeText(this, "Open failed: ${e.message}", Toast.LENGTH_LONG).show()
		}
	}

	private fun generatePdf(onSuccess: (File) -> Unit) {
		val jobName = "html2pdf-${UUID.randomUUID()}"
		val fileName = "html_${System.currentTimeMillis()}.pdf"
		val pdfFile = File(cacheDir, fileName)

		val printAttributes = PrintAttributes.Builder()
			.setMediaSize(PrintAttributes.MediaSize.ISO_A4)
			.setResolution(PrintAttributes.Resolution("pdf", "pdf", 600, 600))
			.setMinMargins(PrintAttributes.Margins.NO_MARGINS)
			.build()

		val adapter = webView.createPrintDocumentAdapter(jobName)
		adapter.onLayout(null, printAttributes, CancellationSignal(), object : PrintDocumentAdapter.LayoutResultCallback() {
			override fun onLayoutFinished(info: PrintDocumentInfo?, changed: Boolean) {
				adapter.onWrite(arrayOf(PageRange.ALL_PAGES), PdfFileDescriptor(pdfFile).pfd, CancellationSignal(), object : PrintDocumentAdapter.WriteResultCallback() {
					override fun onWriteFinished(pages: Array<PageRange>) {
						lastGeneratedPdf = pdfFile
						renderPreview(pdfFile)
						onSuccess(pdfFile)
					}
					override fun onWriteFailed(error: CharSequence?) {
						Toast.makeText(this@MainActivity, "PDF write failed: $error", Toast.LENGTH_LONG).show()
					}
				})
			}
			override fun onLayoutFailed(error: CharSequence?) {
				Toast.makeText(this@MainActivity, "Layout failed: $error", Toast.LENGTH_LONG).show()
			}
		}, null)
	}

	private fun ensurePdfThen(action: (File) -> Unit) {
		val existing = lastGeneratedPdf
		if (existing != null && existing.exists()) {
			action(existing)
		} else {
			generatePdf(action)
		}
	}

	private fun openPdf(pdfFile: File) {
		val uri = FileProvider.getUriForFile(this, "com.example.html2pdf.fileprovider", pdfFile)
		val intent = Intent(Intent.ACTION_VIEW).apply {
			setDataAndType(uri, "application/pdf")
			addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
		}
		try {
			startActivity(intent)
		} catch (_: Exception) {
			Toast.makeText(this, "No PDF viewer found. Use Share or Save.", Toast.LENGTH_LONG).show()
		}
	}

	private fun sharePdf(pdfFile: File) {
		val uri = FileProvider.getUriForFile(this, "com.example.html2pdf.fileprovider", pdfFile)
		val shareIntent = Intent(Intent.ACTION_SEND).apply {
			type = "application/pdf"
			putExtra(Intent.EXTRA_STREAM, uri)
			addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
		}
		startActivity(Intent.createChooser(shareIntent, "Share PDF"))
	}

	private fun savePdf(pdfFile: File) {
		pendingSaveSource = pdfFile
		val suggested = "html_${System.currentTimeMillis()}.pdf"
		createDocLauncher.launch(suggested)
	}

	private fun renderPreview(pdfFile: File) {
		try {
			ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
				PdfRenderer(pfd).use { renderer ->
					if (renderer.pageCount > 0) {
						renderer.openPage(0).use { page ->
							val width = (page.width * 0.5).toInt()
							val height = (page.height * 0.5).toInt()
							val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
							page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
							pdfPreview.setImageBitmap(bitmap)
						}
					}
				}
			}
		} catch (_: Exception) { }
	}

	private class PdfFileDescriptor(file: File) {
		val pfd: ParcelFileDescriptor = ParcelFileDescriptor.open(
			file,
			ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE or ParcelFileDescriptor.MODE_READ_WRITE
		)
	}

	companion object {
		private const val MAX_HTML_BYTES = 1024 * 1024 // 1MB
		private const val MAX_HTML_CHARS = 200_000

		private const val SAMPLE_HTML = """
			<!doctype html>
			<html>
			<head>
				<meta charset='utf-8'/>
				<meta name='viewport' content='width=device-width, initial-scale=1'/>
				<style>
					body{font-family: sans-serif; margin: 24px;}
					h1{color:#6200EE}
					.grid{display:grid;grid-template-columns:repeat(3,1fr);gap:8px}
					.card{border:1px solid #ddd;padding:8px;border-radius:6px}
					img{max-width:100%}
					@media print{a:link:after,a:visited:after{content:" (" attr(href) ")"}}
				</style>
			</head>
			<body>
				<h1>HTML → PDF demo</h1>
				<p>This is a sample showing <strong>selectable text</strong>, images, and CSS grid.</p>
				<div class='grid'>
					<div class='card'>Card 1</div>
					<div class='card'>Card 2</div>
					<div class='card'>Card 3</div>
				</div>
				<p><img src='https://picsum.photos/seed/html2pdf/600/300' alt='sample'/></p>
				<p><a href='https://example.com'>Example link</a></p>
			</body>
			</html>
		"""
	}
}