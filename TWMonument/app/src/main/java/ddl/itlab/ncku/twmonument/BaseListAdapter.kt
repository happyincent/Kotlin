package ddl.itlab.ncku.twmonument

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import kotlin.math.roundToInt

class BaseListAdapter(context: Context, var map: MutableMap<String, Double>) : BaseAdapter() {
    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return map.size
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    override fun getItem(i: Int): Any {
        return map.entries.elementAt(i)
    }

    @SuppressLint("SetTextI18n")
    override fun getView(i: Int, convertView: View?, parent: ViewGroup): View {
        val value = map.values.elementAt(i)

        val view = convertView ?: inflater.inflate(
            if (value != 0.0) android.R.layout.simple_list_item_2 else android.R.layout.simple_list_item_1,
            parent, false
        )

        return view.apply {
            findViewById<TextView>(android.R.id.text1).text = map.keys.elementAt(i)
            if (value != 0.0) {
                findViewById<TextView>(android.R.id.text2).text = value.let {
                    when {
                        it > 1000000 -> "ERROR"
                        it > 1000    -> "${String.format("%.2f", (it/1000))} km"
                        else         -> "${it.roundToInt()} m"
                    }
                }
            }
        }
    }

}