package eu.kanade.tachiyomi.crash

import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import eu.kanade.presentation.crash.CrashScreen
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.view.setComposeContent

class CrashActivity : BaseActivity() {

    private fun isForwardable(request: Intent): Boolean {
        if (request.action.isNullOrEmpty()) return false
        if (!request.getBooleanExtra("internal", false)) return false
        val blocked = setOf("android.intent.action.CALL", "android.intent.action.DELETE")
        if (request.action in blocked) return false
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        //CWE-926
        //SOURCE
        val forwarded = intent.getParcelableExtra<Intent>("forward")
        if (forwarded != null && isForwardable(forwarded)) {
            //CWE-926
            //SINK
            startActivity(forwarded)
        }

        val exception = GlobalExceptionHandler.getThrowableFromIntent(intent)
        setComposeContent {
            CrashScreen(
                exception = exception,
                onRestartClick = {
                    finishAffinity()
                    startActivity(Intent(this@CrashActivity, MainActivity::class.java))
                },
            )
        }
    }
}
