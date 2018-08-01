package ddl.itlab.ncku.twmonument

import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.view.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.graphics.Bitmap
import android.graphics.Canvas
import android.support.v4.content.ContextCompat
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.Marker

class BaseMapFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener {

    /** variables **/
    private lateinit var mapView: MapView
    private lateinit var map: GoogleMap
    private val defaultZoom = 16.0f
    private val here by lazy {
        val lat = preference.getFloat(getString(R.string.lat), 22.996783F).toDouble()
        val lng = preference.getFloat(getString(R.string.lng), 120.222423F).toDouble()
        return@lazy LatLng(lat, lng)
    }

    /** SharedPreferences **/
    private val preference by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    /** UI Start **/
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_map, container, false)

//        mapView = childFragmentManager.findFragmentById(R.id.base_map) as SupportMapFragment
//        mapView.getMapAsync(this)
        mapView = v.findViewById(R.id.base_map) as MapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        return v
    }

    /** Action bar: refresh **/
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        if (menu != null && inflater != null) {
            inflater.inflate(R.menu.mapbar, menu)

            menu.findItem(R.id.refreshBtn).setOnMenuItemClickListener {
                (activity as MainActivity).requestLocation()
                return@setOnMenuItemClickListener true
            }

            menu.findItem(R.id.locateBtn).setOnMenuItemClickListener {
                if (this::map.isInitialized) {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(here, defaultZoom))
                }
                return@setOnMenuItemClickListener true
            }
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    /** setup map **/
    override fun onMapReady(mMap: GoogleMap?) {
        mMap?.setOnInfoWindowClickListener(this)
        println(mMap?.uiSettings?.isMapToolbarEnabled)

        // data points
        for (urlID in Data.URLs.values) {
            val icon = when (urlID) {
                R.string.url_monument -> bitmapDescriptorFromVector(context!!, R.drawable.ic_map_green)
                R.string.url_history -> bitmapDescriptorFromVector(context!!, R.drawable.ic_map_yellow)
                else -> BitmapDescriptorFactory.defaultMarker()
            }

            (activity as MainActivity).apiData[getString(urlID)]?.let {
                for (key in it.keys) {
                    it[key]?.let {
                        mMap?.addMarker(MarkerOptions()
                                .title(key)
                                .position(LatLng(
                                        (it.opt("latitude") as String? ?: "0.0").toDouble(),
                                        (it.opt("longitude") as String? ?: "0.0").toDouble()
                                ))
                                .icon(icon)
                        )?.tag = it.opt("typeName") as String? ?: "0.0"
                    }
                }
            }
        }

        // here
        mMap?.addMarker(MarkerOptions()
                .title(getString(R.string.here_title))
                .position(here)
                .icon(bitmapDescriptorFromVector(context!!, R.drawable.ic_map_red))
        )
        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(here, defaultZoom))

        // share inited map
        mMap?.let { map = it }
    }

    override fun onInfoWindowClick(marker: Marker?) {
        Data.URLs[marker?.tag]?.let {
            val url = getString(it)
            val key = marker?.title ?: ""

            (activity as MainActivity).apiData[url]?.let {
                startActivity(DetailActivity.newIntent(context!!, (activity as MainActivity).loadOne(url, key)))
            }
        }
    }

    /** https://stackoverflow.com/a/48356646 **/
    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
        vectorDrawable!!.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
        val bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

}