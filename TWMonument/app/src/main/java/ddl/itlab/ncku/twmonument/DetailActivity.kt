package ddl.itlab.ncku.twmonument

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_detail.*

class DetailActivity : AppCompatActivity(), OnMapReadyCallback, AdapterView.OnItemClickListener {

    private val data = mutableMapOf<String, String>()
    private val defaultZoom = 16.0f

    companion object {
        private const val INTENT_KEY = "INTENT_KEY"

        fun newIntent(context: Context, data: MutableMap<String, String>): Intent {
            val intent = Intent(context, DetailActivity::class.java)

            intent.putExtra(INTENT_KEY, data.keys.toTypedArray())
            for (k in data.keys) {
                intent.putExtra(k, data[k])
            }

            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        // load data
        for (k in intent.getStringArrayExtra(INTENT_KEY)) {
            data[k] = intent.getStringExtra(k)
        }

        this.title = data["name"]

        // button to go back
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // setup listView
        listView2.adapter = loadAdapter()
        listView2.onItemClickListener = this

        // setup map
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.detail_map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onMapReady(mMap: GoogleMap?) {
        val pos = LatLng(data["latitude"]?.toDouble() ?: 0.0, data["longitude"]?.toDouble() ?: 0.0)
        mMap?.addMarker(MarkerOptions().position(pos).title(data["name"]))
        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, defaultZoom))
    }

    private fun loadAdapter(): ArrayAdapter<String> {
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1)

        for (k in data.keys) {
            if (data[k] != "" && k != "latitude" && k != "longitude" && k != "name") {
                adapter.add("$k：${data[k]}")
            }
        }

        return adapter
    }

    override fun onItemClick(adapter: AdapterView<*>?, v: View?, i: Int, l: Long) {
        adapter?.getItemAtPosition(i).let {
            val str = it.toString()
            if (str.contains("網址")) {
                openURL(str.split("：").last())
            }
        }
    }

    private fun openURL(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
        catch (e: Exception) {
            Toast.makeText(this, "Failed to open \"$url\"", Toast.LENGTH_SHORT).show()
        }
    }

}