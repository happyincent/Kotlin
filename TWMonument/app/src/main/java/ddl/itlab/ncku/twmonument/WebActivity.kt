package ddl.itlab.ncku.twmonument

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class WebActivity : AppCompatActivity() {

    companion object {
        private const val INTENT_URL = "INTENT_URL"

        fun newIntent(context: Context, url: String): Intent {
            val intent = Intent(context, WebActivity::class.java)
            intent.putExtra(INTENT_URL, url)
            return intent
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)

        // button to go back
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // get url
        val url = intent.getStringExtra(INTENT_URL)

        // setup title
        this.title = Uri.parse(url).host.toString()

        // setup webview
        findViewById<WebView>(R.id.webView1).let {
            it.settings.javaScriptEnabled = true
            it.settings.allowContentAccess = true
            it.settings.domStorageEnabled = true
            it.webViewClient = WebViewClient()
            it.loadUrl(url)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}