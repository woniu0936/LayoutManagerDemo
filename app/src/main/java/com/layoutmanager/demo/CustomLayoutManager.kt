package com.layoutmanager.demo

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

class CustomLayoutManager : RecyclerView.LayoutManager() {

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun isAutoMeasureEnabled(): Boolean {
        return true
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        // 可用空间
        var totalSpace = width - paddingRight

        var currentPosition = 0

        //轻量级的将view移除屏幕
        detachAndScrapAttachedViews(recycler)

        //开始填充view
        var left = 0
        var top = 0
        var right = 0
        var bottom = 0
        // 在给定的空间内，将item填充进来
        while (totalSpace > 0 && currentPosition < state.itemCount) {
            // 获取当前currentPosition位置对应的childView
            val view = recycler.getViewForPosition(currentPosition)
            // 填充
            addView(view)
            // 计算尺寸
            measureChildWithMargins(view, 0, 0)

            // 计算应该位于布局的位置
            right = left + getDecoratedMeasurementHorizontal(view)
            bottom = top + getDecoratedMeasurementVertical(view)

            // 将填充的childView放到计算出来的位置
            layoutDecoratedWithMargins(view, left, top, right, bottom)

            // 下一个
            currentPosition++
            // 下一个childView的起始位置
            left += getDecoratedMeasurementHorizontal(view)
            //关键点 剩余空间
            totalSpace -= getDecoratedMeasurementHorizontal(view)
        }

    }


    override fun canScrollHorizontally(): Boolean {
        return true
    }

    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        if (childCount == 0 || dx == 0) return 0

        //填充View，consumed就是修复后的移动值
        val consumed = fill(dx, recycler)
        //移动View
        offsetChildrenHorizontal(-consumed)
        //回收View
        recycle(consumed, recycler)
        return consumed
    }

    /**
     * 核心方法，一切炫酷的效果都是这里实现的
     */
    private fun fill(dx: Int, recycler: RecyclerView.Recycler): Int {
        //将要填充的position
        var fillPosition = RecyclerView.NO_POSITION
        //可用的空间，和onLayoutChildren中的totalSpace类似
        var availableSpace = abs(dx)
        //增加一个滑动距离的绝对值，方便计算
        val absDelta = abs(dx)

        //将要填充的View的左上右下
        var left = 0
        var top = 0
        var right = 0
        var bottom = 0

        //dx>0 手指从右滑向左，所以填充尾部
        if (dx > 0) {
            val anchorView = getChildAt(childCount - 1)!!
            val anchorPosition = getPosition(anchorView)
            val anchorRight = getDecoratedRight(anchorView)

            left = anchorRight
            //填充尾部，应该是+1
            fillPosition = anchorPosition + 1

            if (fillPosition >= itemCount && anchorRight - absDelta < width) {
                // 如果下一个超过itemCount，且最后一个已经完全滑到了屏幕里面，则要修正位置，不让继续滑动和填充
                val fixScrolled = anchorRight - width
                return fixScrolled
            }

            // 如果最后一个还没有完全显示在屏幕里面，则也不用填充
            if (anchorRight - absDelta > width) {
                return dx
            }
        }

        //dx<0 手指从左滑向右，所以填充头部
        if (dx < 0) {
            val anchorView = getChildAt(0)!!
            val anchorPosition = getPosition(anchorView)
            val anchorLeft = getDecoratedLeft(anchorView)

            right = anchorLeft
            //填充头部，那么上一个position就应该是-1
            fillPosition = anchorPosition - 1

            if (fillPosition < 0 && anchorLeft + absDelta > 0) {
                // 同理，已经是第一个切完全显示到屏幕里面了，则不再滑动和填充
                return anchorLeft
            }

            // 还没完全显示到屏幕里面，则不填充
            if (anchorLeft + absDelta < 0) {
                return dx
            }
        }

        // 上面没拦住，说明可以填充了
        while (availableSpace > 0 && (fillPosition in 0 until itemCount)) {
            val itemView = recycler.getViewForPosition(fillPosition)

            if (dx > 0) {
                addView(itemView)
            } else {
                addView(itemView, 0)
            }

            measureChildWithMargins(itemView, 0, 0)

            if (dx > 0) {
                right = left + getDecoratedMeasurementHorizontal(itemView)
            } else {
                left = right - getDecoratedMeasurementHorizontal(itemView)
            }

            bottom = top + getDecoratedMeasurementVertical(itemView)
            layoutDecoratedWithMargins(itemView, left, top, right, bottom)

            if (dx > 0) {
                left += getDecoratedMeasurementHorizontal(itemView)
                fillPosition++
            } else {
                right -= getDecoratedMeasurementHorizontal(itemView)
                fillPosition--
            }

            if (fillPosition in 0 until itemCount) {
                availableSpace -= getDecoratedMeasuredWidth(itemView)
            }
        }

        return dx
    }

    /**
     * 回收
     */
    private fun recycle(
        dx: Int,
        recycler: RecyclerView.Recycler
    ) {
        //要回收View的集合，暂存
        val recycleViews = hashSetOf<View>()

        //dx>0 手指从右滑向左，尾部填充，回收头部滑出屏幕的childView
        if (dx > 0) {
            for (i in 0 until childCount) {
                val child = getChildAt(i)!!
                val right = getDecoratedRight(child)
                // right >= 0 表示当前childView还没完全滑出(头部)屏幕
                if (right >= 0) break
                // 完全滑出了，收集起来准备回收
                recycleViews.add(child)
            }
        }

        //dx<0 从左滑向右，填充头部，回收尾部滑出屏幕的childView
        if (dx < 0) {
            for (i in childCount - 1 downTo 0) {
                val child = getChildAt(i)!!
                val left = getDecoratedLeft(child)

                // left <= width 表示当前childView还没完全滑出(尾部)屏幕
                if (left <= width) break
                // 完全滑出了，收集起来准备回收
                recycleViews.add(child)
            }
        }

        //真正把View移除掉
        for (view in recycleViews) {
            removeAndRecycleView(view, recycler)
        }
        recycleViews.clear()
    }

    /**
     * 获取某个childView在水平方向所占的空间
     *
     * @param view
     * @return
     */
    private fun getDecoratedMeasurementHorizontal(view: View): Int {
        val params = view.layoutParams as RecyclerView.LayoutParams
        return (getDecoratedMeasuredWidth(view) + params.leftMargin
                + params.rightMargin)
    }

    /**
     * 获取某个childView在竖直方向所占的空间
     *
     * @param view
     * @return
     */
    private fun getDecoratedMeasurementVertical(view: View): Int {
        val params = view.layoutParams as RecyclerView.LayoutParams
        return (getDecoratedMeasuredHeight(view) + params.topMargin
                + params.bottomMargin)
    }

}