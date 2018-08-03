package ddl.itlab.ncku.twmonument

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Copyright toast
        val toast = Toast.makeText(this, "${getString(R.string.splash_text1)}\n${getString(R.string.splash_text2)}", Toast.LENGTH_SHORT)
        val v = toast.view.findViewById<TextView>(android.R.id.message)
        v.gravity = Gravity.CENTER
        toast.show()

        startActivity(Intent(this, MainActivity::class.java))

        // remove back activity
        finish()
    }

}