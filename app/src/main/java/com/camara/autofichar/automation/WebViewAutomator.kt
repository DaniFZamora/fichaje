package com.camara.autofichar.automation

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.ThreadLocalRandom

class WebViewAutomator(
  private val context: Context,
  private val url: String,
  private val email: String,
  private val password: String,
  private val delayMs: Long,
  private val logger: (String) -> Unit,
  private val onDone: (ok: Boolean, detail: String) -> Unit
) {
  private val mainHandler = Handler(Looper.getMainLooper())
  private var webView: WebView? = null
  private var finished = false
  private val scope = CoroutineScope(Dispatchers.Main.immediate + Job())

  fun start() {
    mainHandler.post {
      if (finished) return@post
      logger("WebView: creando")
      val w = WebView(context)
      webView = w

      w.settings.javaScriptEnabled = true
      w.settings.domStorageEnabled = true
      w.settings.javaScriptCanOpenWindowsAutomatically = false

      // Cookies / sesión: importante para que el login persista entre ejecuciones
      CookieManager.getInstance().setAcceptCookie(true)
      try {
        CookieManager.getInstance().setAcceptThirdPartyCookies(w, true)
      } catch (_: Throwable) {
        // API < 21 o fabricante raro
      }

      w.webChromeClient = WebChromeClient()
      w.addJavascriptInterface(Bridge(), "Android")

      w.webViewClient = object : WebViewClient() {
        override fun onReceivedError(
          view: WebView,
          request: WebResourceRequest,
          error: WebResourceError
        ) {
          if (request.isForMainFrame && !finished) {
            logger("WebView: error_mainframe")
            finish(false, "web_error")
          }
        }

        override fun onReceivedHttpError(
          view: WebView,
          request: WebResourceRequest,
          errorResponse: WebResourceResponse
        ) {
          if (request.isForMainFrame && !finished) {
            logger("WebView: http_${errorResponse.statusCode}")
            // No siempre aborta: hay portales que devuelven 302/401 antes de login.
          }
        }

        override fun onPageFinished(view: WebView?, url: String?) {
          super.onPageFinished(view, url)
          if (finished) return
          logger("WebView: cargado")
          injectAndRun()
        }
      }

      // Timeout duro
      mainHandler.postDelayed({
        if (!finished) finish(false, "timeout_global")
      }, 95_000)

      w.loadUrl(url)
    }
  }

  fun destroy() {
    mainHandler.post {
      webView?.removeJavascriptInterface("Android")
      webView?.stopLoading()
      webView?.destroy()
      webView = null
    }
  }

  private fun injectAndRun() {
    val w = webView ?: return
    val safeEmail = jsEscape(email.trim())
    val safePass = jsEscape(password)

    val js = """
      (function(){
        const EMAIL = "$safeEmail";
        const PASS = "$safePass";
        const DELAY = ${delayMs};

        function sleep(ms){ return new Promise(r => setTimeout(r, ms)); }

        function isClickable(el){
          if (!el) return false;
          if (!document.documentElement.contains(el)) return false;
          const style = getComputedStyle(el);
          const rect = el.getBoundingClientRect();
          if (style.display === 'none') return false;
          if (style.visibility === 'hidden') return false;
          if (Number(style.opacity) === 0) return false;
          if (style.pointerEvents === 'none') return false;
          if (rect.width <= 0 || rect.height <= 0) return false;
          if (el.disabled) return false;
          if (el.getAttribute('disabled') !== null) return false;
          if (el.getAttribute('aria-disabled') === 'true') return false;
          return true;
        }

        function findLoginForm(){
          const forms = Array.from(document.querySelectorAll('form[action*="/login"]'));
          for (const f of forms){
            const pass = f.querySelector('input[type="password"]');
            if (pass) return f;
          }
          return null;
        }

        function findEmailInput(form){
          return form.querySelector('input[type="email"]')
            || form.querySelector('input[name*="email" i]')
            || form.querySelector('input[autocomplete="username" i]');
        }

        function findTargetButton(){
          const byId = document.getElementById('ficharButton');
          if (byId) return byId;

          const candidates = document.querySelectorAll(
            "button, [role='button'], input[type='button'], input[type='submit']"
          );

          for (const el of candidates){
            const txt = (el.textContent || el.value || '').trim().toLowerCase();
            const id = (el.id || '').toLowerCase();
            const cls = (el.className || '').toString().toLowerCase();
            if (
              id.includes('fichar') || cls.includes('fichar') ||
              id.includes('entrar') || cls.includes('entrar') ||
              txt === 'entrar' || txt.includes('entrar') || txt.includes('fichar')
            ) return el;
          }
          return null;
        }

        async function doLoginIfNeeded(){
          const form = findLoginForm();
          if (!form) {
            Android.onLog('login: no_login_form (ya logueado)');
            return { ok:true, skipped:true };
          }

          if (!EMAIL || !PASS){
            Android.onLog('login: missing_creds');
            return { ok:false, reason:'missing_creds' };
          }

          const emailEl = findEmailInput(form);
          const passEl = form.querySelector('input[type="password"]');
          if (!passEl) return { ok:false, reason:'password_input_not_found' };

          if (emailEl){
            emailEl.focus();
            emailEl.value = EMAIL;
            emailEl.dispatchEvent(new Event('input', { bubbles:true }));
            emailEl.dispatchEvent(new Event('change', { bubbles:true }));
          }

          passEl.focus();
          passEl.value = PASS;
          passEl.dispatchEvent(new Event('input', { bubbles:true }));
          passEl.dispatchEvent(new Event('change', { bubbles:true }));

          const submitBtn = form.querySelector('button[type="submit"], input[type="submit"]');
          if (submitBtn && isClickable(submitBtn)) submitBtn.click();
          else form.submit();

          Android.onLog('login: submitted');

          const start = Date.now();
          const timeoutMs = 30000;
          while (Date.now() - start < timeoutMs){
            await sleep(300);
            if (!findLoginForm()) {
              Android.onLog('login: success (form gone)');
              return { ok:true, logged:true };
            }
            if (document.getElementById('ficharButton')){
              Android.onLog('login: success (ficharButton found)');
              return { ok:true, logged:true };
            }
          }

          Android.onLog('login: timeout');
          return { ok:false, reason:'login_timeout' };
        }

        async function tryClick(){
          const btn = findTargetButton();
          if (!btn) {
            Android.onLog('click: target_not_found');
            return false;
          }
          if (!isClickable(btn)) {
            Android.onLog('click: target_not_clickable');
            return false;
          }

          try{
            btn.scrollIntoView({ block:'center', inline:'center' });
            await sleep(120);
            btn.dispatchEvent(new MouseEvent('mousedown', { bubbles:true, cancelable:true, view:window }));
            btn.dispatchEvent(new MouseEvent('mouseup', { bubbles:true, cancelable:true, view:window }));
            btn.click();
            Android.onLog('click: clicked');
            return true;
          }catch(e){
            Android.onLog('click: exception');
            return false;
          }
        }

        async function run(){
          const loginRes = await doLoginIfNeeded();
          if (loginRes && loginRes.ok === false && loginRes.reason === 'missing_creds'){
            // sin creds: no abortamos, intentamos click igualmente
          }

          Android.onLog('wait: ' + DELAY + 'ms');
          await sleep(DELAY);

          const ok1 = await tryClick();
          if (ok1) { Android.onDone('clicked'); return; }

          await sleep(900);
          const ok2 = await tryClick();
          if (ok2) { Android.onDone('clicked_retry'); return; }

          Android.onFail('click_failed');
        }

        try{ run(); }catch(e){ Android.onFail('js_crash'); }
      })();
    """.trimIndent()

    logger("JS: inyectando (delay=${delayMs}ms)")
    w.evaluateJavascript(js, null)
  }

  private fun finish(ok: Boolean, detail: String) {
    if (finished) return
    finished = true
    logger("DONE: ok=$ok · $detail")

    // Fuerza persistencia de cookies/sesión
    try { CookieManager.getInstance().flush() } catch (_: Throwable) { }

    onDone(ok, detail)
    destroy()
  }

  private fun jsEscape(s: String): String {
    return s
      .replace("\\", "\\\\")
      .replace("\"", "\\\"")
      .replace("\n", "\\n")
      .replace("\r", "\\r")
  }

  inner class Bridge {
    @JavascriptInterface
    fun onLog(msg: String) {
      logger("JS: $msg")
    }

    @JavascriptInterface
    fun onDone(detail: String) {
      if (!finished) finish(true, detail)
    }

    @JavascriptInterface
    fun onFail(detail: String) {
      if (!finished) finish(false, detail)
    }
  }

  companion object {
    fun randomDelayMs(min: Int, max: Int): Long {
      return ThreadLocalRandom.current().nextInt(min, max + 1).toLong()
    }
  }
}
