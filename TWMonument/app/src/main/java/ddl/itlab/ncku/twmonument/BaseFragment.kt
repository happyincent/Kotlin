package ddl.itlab.ncku.twmonument

import android.location.Location
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.view.*
import android.support.v7.widget.SearchView
import android.widget.Toast
import kotlinx.android.synthetic.main.fragment_base.*
import org.json.JSONObject

class BaseFragment : Fragment() {

    /** pass argument **/
    companion object {
        private const val INSTANCE_URL = "MY_STR_ARG_KEY"

        fun newInstance(str: String) = BaseFragment().apply {
            arguments = Bundle(1).apply {
                putString(INSTANCE_URL, str)
            }
        }
    }

    /** variables **/
    private lateinit var url: String
    private val data = mutableMapOf<String, JSONObject>()
    private var distance = mutableMapOf<String, Double>()

    /** SharedPreferences **/
    private val preference by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    /** load data **/
    private fun loadData() {
        try {
            (activity as MainActivity).apiData[url]?.let { data.putAll(it) }

            for (key in data.keys) {
                distance[key] = 0.0
            }

            loadDistance()
        }
        catch (e: Exception) {
            Toast.makeText(activity, e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun loadDistance() {
        val lat = preference.getFloat(getString(R.string.lat), 0.0F).toDouble()
        val lng = preference.getFloat(getString(R.string.lng), 0.0F).toDouble()

        if (lat != 0.0 && lng != 0.0) {
            for (key in data.keys) {
                var latitude = (data[key]?.opt("latitude") as String? ?: "0.0").toDouble()
                var longitude = (data[key]?.opt("longitude") as String? ?: "0.0").toDouble()
                val results = FloatArray(1)

                // swap error data
                if (longitude < latitude) {
                    longitude = latitude.also { latitude = longitude }
                }

                Location.distanceBetween(
                        latitude,
                        longitude,
                        lat, lng,
                        results
                )

                distance[key] = results[0].toDouble()
            }

            distance = distance.toSortedMap(compareBy{ distance[it] }).toMutableMap()
        }
    }

    /** UI start **/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            it.getString(INSTANCE_URL)?.let { arg_url ->
                url = arg_url
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_base, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (this::url.isInitialized) {
            // load data & distance
            loadData()

            // init listView (check if having distance)
            listView1.adapter = BaseListAdapter(context!!, distance)

            listView1.setOnItemClickListener { _, _, i, _ ->
                listView1?.let {
                    val adapter = it.adapter as BaseListAdapter
                    startActivity(DetailActivity.newIntent(context!!, (activity as MainActivity).loadDetail(url, adapter.map.keys.elementAt(i))))
                }
            }
        }
    }

    /** Action bar: Search **/
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        if (menu != null && inflater != null) {
            inflater.inflate(R.menu.basebar, menu)

            val searchView = menu.findItem(R.id.searchBtn).actionView as SearchView

            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
                override fun onQueryTextChange(query: String?): Boolean {
                    listView1?.let { view ->
                        val adapter = view.adapter as BaseListAdapter
                        adapter.map = distance.filterKeys { it.contains(query.toString()) }.toMutableMap()
                        adapter.notifyDataSetChanged()
                    }
                    return false
                }

                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }
            })
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

}