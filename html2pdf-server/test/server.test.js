import http from 'http'

const body = new URLSearchParams({ html: '<h1>test</h1>' }).toString()

const req = http.request({
	hostname: 'localhost',
	port: process.env.PORT || 3000,
	path: '/convert',
	method: 'POST',
	headers: {
		'Content-Type': 'application/x-www-form-urlencoded',
		'Content-Length': Buffer.byteLength(body)
	}
}, res => {
	if (res.statusCode === 200) {
		console.log('OK')
	} else {
		console.error('FAIL', res.statusCode)
		process.exitCode = 1
	}
})
req.on('error', err => { console.error('ERR', err.message); process.exitCode = 1 })
req.write(body)
req.end()