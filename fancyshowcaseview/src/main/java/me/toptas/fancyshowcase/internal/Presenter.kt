package me.toptas.fancyshowcase.internal

import android.graphics.Rect
import android.view.Gravity
import me.toptas.fancyshowcase.FocusShape
import me.toptas.fancyshowcase.R
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sqrt

internal var DISABLE_ANIMATIONS_FOR_TESTING = false

internal class Presenter(private val pref: SharedPref,
                         private val device: DeviceParams,
                         private val props: Properties) {

    var centerX: Int = 0
    var centerY: Int = 0
    var hasFocus = false
    var circleCenterX: ArrayList<Int> = arrayListOf()
    var circleCenterY: ArrayList<Int> = arrayListOf()
    var focusShape = props.focusShape

    var bitmapWidth = 0
    var bitmapHeight = 0
    var focusWidth: ArrayList<Int> = arrayListOf()
    var focusHeight: ArrayList<Int> = arrayListOf()
    var viewRadius: ArrayList<Int> = arrayListOf()

    fun initialize() {
        props.backgroundColor = if (props.backgroundColor != 0)
            props.backgroundColor
        else
            device.currentBackgroundColor()
        props.titleGravity = if (props.titleGravity >= 0) props.titleGravity else Gravity.CENTER
        props.titleStyle = if (props.titleStyle != 0) props.titleStyle else R.style.FancyShowCaseDefaultTitleStyle


        centerX = (device.deviceWidth() / 2)
        centerY = (device.deviceHeight() / 2)
    }


    fun show(onShow: () -> Unit/*, waitForLayout: () -> Unit*/) {
        if (pref.isShownBefore(props.fancyId)) {
            props.dismissListener?.onSkipped(props.fancyId)
            props.queueListener?.onNext()
            return
        }
        // if view is not laid out get, width/height values in onGlobalLayout
        for (view in props.focusedViewArray) {
            if (view?.cantFocus() == true) {
                view.waitForLayout { onShow() }
            } else {
                onShow()
            }
        }
    }

    fun calculations() {
        val deviceWidth = device.deviceWidth()
        val deviceHeight = device.deviceHeight()
        bitmapWidth = deviceWidth
        bitmapHeight = deviceHeight - if (props.fitSystemWindows) 0 else device.getStatusBarHeight()
        for (view in props.focusedViewArray) {
            if (view != null) {
                focusWidth.add(view.width())
                focusHeight.add(view.height())
                view.apply {
                    val center = getCircleCenter(this)
                    circleCenterX.add(center.x)
                    circleCenterY.add(center.y)
                }

                viewRadius.add(((hypot(view.width().toDouble(), view.height().toDouble()) / 2).toInt() * props.focusCircleRadiusFactor).toInt())
                hasFocus = true
            } else {
                hasFocus = false
            }
        }
    }

    fun writeShown(fancyId: String?) {
        fancyId?.let {
            pref.writeShown(it)
        }
    }

    private fun setRectPosition(positionX: Int, positionY: Int, rectWidth: Int, rectHeight: Int) {
        circleCenterX.add(positionX)
        circleCenterY.add(positionY)
        focusWidth.add(rectWidth)
        focusHeight.add(rectHeight)
        focusShape = FocusShape.ROUNDED_RECTANGLE
        hasFocus = true
    }

    private fun setCirclePosition(positionX: Int, positionY: Int, radius: Int) {
        circleCenterX.add(positionX)
        viewRadius.add(radius)
        circleCenterY.add(positionY)
        focusShape = FocusShape.CIRCLE
        hasFocus = true
    }

    fun circleRadius(animCounter: Int, animMoveFactor: Double): Float {
        for (radius in viewRadius) {
            return (radius + animCounter * animMoveFactor).toFloat()
        }
        return 0f
    }

    fun roundRectLeft(animCounter: Int, animMoveFactor: Double): Float {
        for ((index, center) in circleCenterX.withIndex()) {
            return (circleCenterX[index].toDouble() - (focusWidth[index] / 2).toDouble() - animCounter * animMoveFactor).toFloat()
        }
        return 0f
    }


    fun roundRectTop(animCounter: Int, animMoveFactor: Double): Float {
        for ((index, center) in circleCenterY.withIndex()) {
            return (circleCenterY[index].toDouble() - (focusHeight[index] / 2).toDouble() - animCounter * animMoveFactor).toFloat()
        }
        return 0f
    }


    fun roundRectRight(animCounter: Int, animMoveFactor: Double): Float {
        for ((index, center) in circleCenterX.withIndex()) {
            return (circleCenterX[index].toDouble() + (focusWidth[index] / 2).toDouble() + animCounter * animMoveFactor).toFloat()
        }
        return 0f
    }


    fun roundRectBottom(animCounter: Int, animMoveFactor: Double): Float {
        for ((index, center) in circleCenterX.withIndex()) {
            return (circleCenterY[index].toDouble() + (focusHeight[index] / 2).toDouble() + animCounter * animMoveFactor).toFloat()
        }
        return 0f
    }

    fun getCircleCenter(view: IFocusedView): CircleCenter {
        val shouldAdjustYPosition = (props.fitSystemWindows && device.aboveAPI19()
                || (device.isFullScreen() && !props.fitSystemWindows))

        val adjustHeight = if (shouldAdjustYPosition)
            0
        else
            device.getStatusBarHeight()

        val viewPoint = IntArray(2)

        val point = view.getLocationInWindow(viewPoint)
        val center = CircleCenter(0, 0)
        center.x = point[0] + view.width() / 2
        center.y = point[1] + view.height() / 2 - adjustHeight
        return center
    }

    fun isWithinZone(x: Float, y: Float, clickableView: IFocusedView): Boolean {
        var isWithin = false
        val viewCenter = getCircleCenter(clickableView)
        val focusCenterX = viewCenter.x
        val focusCenterY = viewCenter.y
        val focusWidth = clickableView.width()
        val focusHeight = clickableView.height()
        val focusRadius =
                if (FocusShape.CIRCLE == props.focusShape)
                    circleRadius(0, 1.0)
                else 0f

        when (props.focusShape) {
            FocusShape.CIRCLE -> {
                val distance = sqrt(
                        (focusCenterX - x).toDouble().pow(2.0) + (focusCenterY - y).toDouble().pow(2.0))

                isWithin = abs(distance) < focusRadius
            }
            FocusShape.ROUNDED_RECTANGLE -> {
                val rect = Rect()
                val left = focusCenterX - focusWidth / 2
                val right = focusCenterX + focusWidth / 2
                val top = focusCenterY - focusHeight / 2
                val bottom = focusCenterY + focusHeight / 2
                rect.set(left, top, right, bottom)
                isWithin = rect.contains(x.toInt(), y.toInt())
            }
        }

        return isWithin
    }

    fun setFocusPositions() {
        if (props.focusRectangleWidth > 0 && props.focusRectangleHeight > 0) {
            setRectPosition(props.focusPositionX, props.focusPositionY, props.focusRectangleWidth, props.focusRectangleHeight)
        }
        if (props.focusCircleRadius > 0) {
            setCirclePosition(props.focusPositionX, props.focusPositionY, props.focusCircleRadius)
        }
    }

    fun calcAutoTextPosition(): AutoTextPosition {
        val top = roundRectTop(0, 0.0)
        val bottom = roundRectBottom(0, 0.0)

        val spaceAbove = top.toInt()
        val spaceBelow = bitmapHeight - bottom.toInt()
        //val params = view.layoutParams as RelativeLayout.LayoutParams
        val autoPos = AutoTextPosition()
        for ((index, center) in props.focusedViewArray.withIndex()) {

            val halfViewHeight = if (focusShape == FocusShape.ROUNDED_RECTANGLE) focusHeight[index] / 2 else viewRadius[index]

            if (spaceAbove > spaceBelow) {
                autoPos.bottomMargin = bitmapHeight - (circleCenterY[index] + halfViewHeight)
                autoPos.topMargin = 0
                autoPos.height = top.toInt()
            } else {
                autoPos.topMargin = circleCenterY[index] + halfViewHeight
                autoPos.bottomMargin = 0
                autoPos.height = (bitmapHeight - top).toInt()
            }
        }
        return autoPos
    }
}
