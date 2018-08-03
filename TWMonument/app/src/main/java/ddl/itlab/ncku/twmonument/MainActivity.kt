package ddl.itlab.ncku.twmonument

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.view.View
import android.widget.Toast
import com.ncapdevi.fragnav.FragNavController
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object Data {
    val URLs = mapOf(
        "古蹟" to R.string.url_monument, "歷史建築" to R.string.url_history
    )
}

class MainActivity : AppCompatActivity() {

    /** public: all data **/
    val apiData = HashMap<String, MutableMap<String, JSONObject>>()

    /** public: load one data **/
    fun loadDetail(url: String, key: String): MutableMap<String, String> {
        val detail = mutableMapOf<String, String>()

        apiData[url]?.let {
            var latitude = (it[key]?.opt("latitude") as String? ?: "0.0")
            var longitude = (it[key]?.opt("longitude") as String? ?: "0.0")
            // swap error data
            if (longitude.toDouble() < latitude.toDouble()) {
                longitude = latitude.apply { latitude = longitude }
            }

            val buildDate = it[key]?.opt("buildingCreateWestYear") as String? ?: ""
            val openTime = it[key]?.opt("openTime") as String? ?: ""

            detail["latitude"] = latitude
            detail["longitude"] = longitude
            detail["name"] = it[key]?.opt("name") as String? ?: ""
            detail["開放時間"] = openTime
            detail["建造日期"] = buildDate
            detail["年代"] = it[key]?.opt("buildingYearName") as String? ?: ""
            detail["類型"] = it[key]?.opt("typeName") as String? ?: ""
            detail["等級"] = it[key]?.opt("level") as String? ?: ""
            detail["地址"] = it[key]?.opt("address") as String? ?: ""
            detail["網址"] = it[key]?.opt("srcWebsite") as String? ?: ""
        }

        return detail
    }

    /** savedInstanceState **/
    private var save: Bundle? = null

    /** SharedPreferences **/
    private val preference by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    /** fragNav **/
    private val fragNavController: FragNavController = FragNavController(supportFragmentManager, R.id.fragment1)

    /** bottom navigation listener **/
    private val fragIndex: Map<Int, Int> = mapOf(
            R.id.navigation_monument to 0,
            R.id.navigation_history to 1,
            R.id.navigation_map to 2
    )

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        if (item.itemId in fragIndex.keys) {
            fragNavController.switchTab(fragIndex[item.itemId]!!)
            return@OnNavigationItemSelectedListener true
        }
        false
    }

    /** location **/
    private val timeout: Long = 15 // seconds

    private fun haveLocation() : Boolean {
        return preference.contains(getString(R.string.lat)) && preference.contains(getString(R.string.lng))
    }

    private val locationManager by lazy {
        return@lazy getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private val locationListenerNET: LocationListener = object : LocationListener {
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}

        override fun onLocationChanged(location: Location) {
            if (!haveLocation()) {
                val lat = location.latitude.toFloat()
                val lng = location.longitude.toFloat()
                preference.edit().putFloat(getString(R.string.lat), lat).apply()
                preference.edit().putFloat(getString(R.string.lng), lng).apply()

                println("$lat $lng ... NET set")
                loadData()
            }
        }
    }

    private val locationListenerGPS: LocationListener = object : LocationListener {
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}

        override fun onLocationChanged(location: Location) {
            if (haveLocation()) {
                refreshLayout1.isRefreshing = true
            }

            val lat = location.latitude.toFloat()
            val lng = location.longitude.toFloat()
            preference.edit().putFloat(getString(R.string.lat), lat).apply()
            preference.edit().putFloat(getString(R.string.lng), lng).apply()

            println("$lat $lng ... GPS")
            loadData()
        }
    }

    fun requestLocation() {
        try {
            println("requestSingleUpdate ...")
            refreshLayout1.isRefreshing = true

            locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListenerNET, null)
            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListenerGPS, null)

            Handler().postDelayed({
                locationManager.removeUpdates(locationListenerNET)
                locationManager.removeUpdates(locationListenerGPS)
                refreshLayout1.isRefreshing = false
                if (!haveLocation()) {
                    Toast.makeText(this@MainActivity, getString(R.string.loc_err_msg), Toast.LENGTH_LONG).show()
                    loadData()
                }
            }, timeout * 1000 )
        }
        catch(e: SecurityException) {
            refreshLayout1.isRefreshing = false
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == getString(R.string.myLocationRequestCode).toInt()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocation()
            } else {
                loadData()
            }
        }
    }

    /** refresh listener **/
    private val refreshListener = SwipeRefreshLayout.OnRefreshListener {
        requestLocation()
    }

    /** check json file **/
    private var counter = Data.URLs.size

    private fun loadData() {
        counter = Data.URLs.size

        try {
            for (urlID in Data.URLs.values) {
                val url = getString(urlID)

                if (preference.contains(url)) {
                    parseJSON(url, preference.getString(url, ""))
                    counter--
                } else {
                    getJSON(url)
                }
            }
        }
        catch (e : Exception) {
            counter = 0
        }

        Handler(Looper.getMainLooper()).postDelayed({
            runOnUiThread {
                while (counter > 0) {}
                loadFragments()
                switchUI(hide = false)
            }
        }, 1 * 1000 ) // wait for OkHttp's onFailure
    }

    private fun getJSON(url: String) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                response.body()?.string()?.let {
                    preference.edit().putString(url, it).apply()
                    parseJSON(url, it)
                }
                counter--
            }

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, getString(R.string.file_err_msg), Toast.LENGTH_LONG).show()
                    counter--
                }
            }
        })
    }

    private fun parseJSON(url: String, str: String) {
        val data = HashMap<String, JSONObject>()
        val json = JSONArray(str)

        for (i in 0 until json.length()) {
            val item = json.getJSONObject(i)
            val key = item["name"].toString()
            data[key] = item
        }

        apiData[url] = data
    }

    /** UI reload **/
    private fun loadFragments() {
        fragNavController.rootFragments = listOf(
                BaseFragment.newInstance(getString(R.string.url_monument)),
                BaseFragment.newInstance(getString(R.string.url_history)),
                BaseMapFragment()
        )

        fragNavController.initialize(fragNavController.currentStackIndex, save)
    }

    private fun switchUI(hide: Boolean) {
        if (hide) {
            progressBar1.visibility = View.VISIBLE
            navigation1.visibility = View.GONE
            refreshLayout1.visibility = View.GONE
        } else {
            progressBar1.visibility = View.GONE
            navigation1.visibility = View.VISIBLE
            refreshLayout1.visibility = View.VISIBLE
            refreshLayout1.isRefreshing = false
        }
    }

    /** UI start **/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // share savedInstanceState
        save = savedInstanceState

        // reset location
        preference.edit().remove(getString(R.string.lat)).apply()
        preference.edit().remove(getString(R.string.lng)).apply()

        // init view
        switchUI(hide = true)
        navigation1.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)
        refreshLayout1.setOnRefreshListener(refreshListener)

        // init fragments
        fragNavController.apply {
            rootFragments = listOf(Fragment(), Fragment(), Fragment())
            fragmentHideStrategy = FragNavController.DETACH_ON_NAVIGATE_HIDE_ON_SWITCH
        }
        fragNavController.initialize(0, savedInstanceState)

        // check permission & request location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), getString(R.string.myLocationRequestCode).toInt())
        } else {
            requestLocation()
        }
    }

}
