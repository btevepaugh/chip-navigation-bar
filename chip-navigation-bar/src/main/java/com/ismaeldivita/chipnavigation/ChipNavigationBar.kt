package com.ismaeldivita.chipnavigation

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.IntRange
import androidx.annotation.MenuRes
import com.ismaeldivita.chipnavigation.model.MenuParser
import com.ismaeldivita.chipnavigation.util.applyWindowInsets
import com.ismaeldivita.chipnavigation.util.forEachChild
import com.ismaeldivita.chipnavigation.util.getChildren
import com.ismaeldivita.chipnavigation.view.HorizontalMenuItemView
import com.ismaeldivita.chipnavigation.view.MenuItemView
import com.ismaeldivita.chipnavigation.view.VerticalMenuItemView

class ChipNavigationBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private lateinit var orientationMode: MenuOrientation
    private var listener: OnItemSelectedListener? = null
    private var minimumExpandedWidth: Int = 0
    private var isExpanded: Boolean = false

    @MenuRes
    private var menuRes = -1

    private val badgesState = mutableMapOf<Int, Int>()

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.ChipNavigationBar)

        val menuResource = a.getResourceId(R.styleable.ChipNavigationBar_cnb_menuResource, -1)
        val minExpanded = a.getDimension(R.styleable.ChipNavigationBar_cnb_minExpandedWidth, 0F)
        val leftInset = a.getBoolean(R.styleable.ChipNavigationBar_cnb_addLeftInset, false)
        val topInset = a.getBoolean(R.styleable.ChipNavigationBar_cnb_addTopInset, false)
        val rightInset = a.getBoolean(R.styleable.ChipNavigationBar_cnb_addRightInset, false)
        val bottomInset = a.getBoolean(R.styleable.ChipNavigationBar_cnb_addBottomInset, false)
        val orientation = when (a.getInt(R.styleable.ChipNavigationBar_cnb_orientationMode, 0)) {
            0 -> MenuOrientation.HORIZONTAL
            1 -> MenuOrientation.VERTICAL
            else -> MenuOrientation.HORIZONTAL
        }

        a.recycle()

        setMenuOrientation(orientation)

        if (menuResource >= 0) {
            setMenuResource(menuResource)
        }

        setMinimumExpandedWidth(minExpanded.toInt())
        applyWindowInsets(leftInset, topInset, rightInset, bottomInset)
        collapse()

        isClickable = true
    }

    /**
     * Inflate a menu from the specified XML resource
     *
     * @param menuRes Resource ID for an XML layout resource to load
     */
    fun setMenuResource(@MenuRes menuRes: Int) {
        this.menuRes = menuRes

        val menu = (MenuParser(context).parse(menuRes))
        val childListener: (View) -> Unit = { view ->
            val id = view.id
            setItemSelected(id)
            listener?.onItemSelected(id)
        }

        removeAllViews()

        menu.items.forEach {
            val itemView = createMenuItem().apply {
                bind(it)
                setOnClickListener(childListener)
            }
            addView(itemView)
        }
    }

    /**
     * Set the menu orientation
     *
     * @param mode orientation
     */
    fun setMenuOrientation(menuOrientation: MenuOrientation) {
        orientationMode = menuOrientation
        orientation = when (menuOrientation) {
            MenuOrientation.HORIZONTAL -> HORIZONTAL
            MenuOrientation.VERTICAL -> VERTICAL
        }
    }

    /**
     * Set the enabled state for the menu item with the provided [id]
     *
     * @param id menu item id
     * @param isEnabled true if this view is enabled, false otherwise
     */
    fun setItemEnabled(id: Int, isEnabled: Boolean) {
        getItemById(id)?.isEnabled = isEnabled
    }

    /**
     * Remove the selected state from the current item and set the selected state to true
     * for the menu item with the [id]
     *
     * This event will not be propagated to the current [OnItemSelectedListener]
     *
     * @param id menu item id
     */
    fun setItemSelected(id: Int) {
        val selectedItem = getSelectedItem()

        if (selectedItem?.id != id) {
            selectedItem?.isSelected = false
            getItemById(id)?.isSelected = true
        }
    }

    /**
     * Set the minimum width for the vertical expanded state.
     *
     * @param minExpandedWidth width in pixels
     */
    fun setMinimumExpandedWidth(minExpandedWidth: Int) {
        minimumExpandedWidth = minExpandedWidth
    }

    /**
     * Register a callback to be invoked when a menu item is selected
     *
     * @param listener The callback that will run
     */
    fun setOnItemSelectedListener(listener: OnItemSelectedListener) {
        this.listener = listener
    }

    /**
     * Register a callback to be invoked when a menu item is selected
     *
     * @param block The callback that will run
     */
    fun setOnItemSelectedListener(block: (Int) -> Unit) {
        setOnItemSelectedListener(object : OnItemSelectedListener {
            override fun onItemSelected(id: Int) {
                block(id)
            }
        })
    }

    /**
     * Display a notification numberless badge for a menu item
     *
     * @param id menu item id
     */
    fun showBadge(id: Int) {
        badgesState[id] = 0
        getItemById(id)?.showBadge()
    }

    /**
     * Display a notification badge with a counter for a menu item
     * The maximum digits length to be displayed is 2 otherwise "+99" will be displayed
     *
     * @param id menu item id
     */
    fun showBadge(id: Int, @IntRange(from = 1) count: Int) {
        badgesState[id] = count
        getItemById(id)?.showBadge(count)
    }

    /**
     * Dismiss the displayed badge for a menu item
     *
     * @param id menu item id
     */
    fun dismissBadge(id: Int) {
        badgesState.remove(id)
        getItemById(id)?.dismissBadge()
    }

    /**
     * Collapse the menu items if orientationMode is VERTICAL otherwise, do nothing
     */
    fun collapse() {
        isExpanded = false

        if (orientationMode == MenuOrientation.VERTICAL) {
            forEachChild {
                it.minimumWidth = 0
                (it as? VerticalMenuItemView)?.collapse()
            }
        }
    }

    /**
     * Expand the menu items if orientationMode is VERTICAL otherwise, do nothing
     */
    fun expand() {
        isExpanded = true

        if (orientationMode == MenuOrientation.VERTICAL) {
            forEachChild {
                it.minimumWidth = minimumExpandedWidth
                (it as? VerticalMenuItemView)?.expand()
            }
        }
    }

    /**
     * Return a boolean if the menu is expanded on the VERTICAL orientationMode.
     *
     * @return true if expanded
     */
    fun isExpanded(): Boolean = when (orientationMode) {
        MenuOrientation.VERTICAL -> isExpanded
        MenuOrientation.HORIZONTAL -> false
    }

    /**
     * Return the selected menu item id
     *
     * @return menu item id or -1 if none item is selected
     */
    fun getSelectedItemId(): Int = getSelectedItem()?.id ?: -1

    /**
     * Return the current selected menu item
     *
     * @return the selected menu item view or null if none is selected
     */
    private fun getSelectedItem() = getChildren().firstOrNull { it.isSelected }

    /**
     * Return a menu item view with provided [id]
     *
     * @param id menu item id
     * @return the menu item view or null if the id was not found
     */
    private fun getItemById(id: Int) = getChildren()
        .filterIsInstance<MenuItemView>()
        .firstOrNull { it.id == id }

    /**
     * Create a menu item view based on the menu orientationMode
     *
     * @return a new [MenuItemView] instance
     */
    private fun createMenuItem(): MenuItemView = when (orientationMode) {
        MenuOrientation.HORIZONTAL -> HorizontalMenuItemView(context)
        MenuOrientation.VERTICAL -> VerticalMenuItemView(context)
    }


    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        return State(superState, Bundle()).apply {
            menuId = menuRes
            selectedItem = getSelectedItemId()
            badges = badgesState
            expanded = isExpanded
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        when (state) {
            is State -> {
                super.onRestoreInstanceState(state.superState)

                if (state.menuId != -1) setMenuResource(state.menuId)
                if (state.selectedItem != -1) setItemSelected(state.selectedItem)
                if (state.expanded) expand() else collapse()

                state.badges.forEach { (itemId, count) ->
                    if (count > 0) {
                        showBadge(itemId, count)
                    } else {
                        showBadge(itemId)
                    }
                }
            }
            else -> super.onRestoreInstanceState(state)
        }
    }

    /**
     * Interface definition for a callback to be invoked when a menu item is selected
     */
    interface OnItemSelectedListener {
        /**
         * Called when a item has been selected
         *
         * @param id menu item id
         */
        fun onItemSelected(id: Int)
    }

    enum class MenuOrientation {
        HORIZONTAL,
        VERTICAL
    }
}