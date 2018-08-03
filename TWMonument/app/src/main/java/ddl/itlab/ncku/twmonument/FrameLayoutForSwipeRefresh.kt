package ddl.itlab.ncku.twmonument

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.widget.FrameLayout

/* Modified from
 * https://stackoverflow.com/questions/25270171/scroll-up-does-not-work-with-swiperefreshlayout-in-listview/27690074 */
class FrameLayoutForSwipeRefresh : FrameLayout {
    @JvmOverloads
    constructor(
            context: Context,
            attrs: AttributeSet? = null,
            defStyleAttr: Int = 0)
            : super(context, attrs, defStyleAttr)

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(
            context: Context,
            attrs: AttributeSet?,
            defStyleAttr: Int,
            defStyleRes: Int)
            : super(context, attrs, defStyleAttr, defStyleRes)

    override fun canScrollVertically(direction: Int): Boolean {
        requestFocus()
        if (focusedChild != null)
            return focusedChild.canScrollVertically(direction)
        return true
    }
}