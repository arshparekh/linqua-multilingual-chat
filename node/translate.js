/*
  stdin: JSON
  {"text":"...","sourceLang":"en","targetLang":"es"}

  stdout: JSON
  {"translatedText":"...","detectedSource":"..."}
*/

const googleTranslate = require('google-translate-api-x');

function readStdin() {
    return new Promise((resolve, reject) => {
        let data = '';
        process.stdin.setEncoding('utf8');
        process.stdin.on('data', (chunk) => (data += chunk));
        process.stdin.on('end', () => resolve(data));
        process.stdin.on('error', reject);
    });
}

(async() => {
    try {
        const raw = await readStdin();
        const input = raw ? JSON.parse(raw) : {};

        const text = input.text || '';
        // Ensure language codes are lowercased and clean of trailing spaces
        const sourceLang = input.sourceLang ? String(input.sourceLang).trim().toLowerCase() : 'auto';
        const targetLang = input.targetLang ? String(input.targetLang).trim().toLowerCase() : 'en';

        if (!String(text).trim()) {
            process.stdout.write(JSON.stringify({ translatedText: text, detectedSource: sourceLang }));
            return;
        }

        // Call translation API with corrected options object
        const res = await googleTranslate(text, { from: sourceLang, to: targetLang });

        // Safe extraction for google-translate-api-x standard response format
        let translatedText = '';
        if (res) {
            if (typeof res === 'string') {
                translatedText = res;
            } else if (res.text) {
                translatedText = res.text;
            } else if (res.translated && res.translated.text) {
                translatedText = res.translated.text;
            }
        }

        // Safe extraction for ISO language codes
        let detectedSource = sourceLang;
        if (res && res.from && res.from.language && res.from.language.iso) {
            detectedSource = res.from.language.iso;
        }

        process.stdout.write(JSON.stringify({ translatedText, detectedSource }));
    } catch (err) {
        const message = err && err.message ? err.message : String(err);
        process.stderr.write(message);
        process.exit(1);
    }
})();