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
    var circleCenterXArray: ArrayList<Int> = arrayListOf()
    var circleCenterYArray: ArrayList<Int> = arrayListOf()
    var circleCenterX: Int = 0
    var circleCenterY: Int = 0
    var focusShape = props.focusShape
    var clickableArea = props.clickableArea

    var bitmapWidth = 0
    var bitmapHeight = 0
    var focusWidthArray: ArrayList<Int> = arrayListOf()
    var focusHeightArray: ArrayList<Int> = arrayListOf()
    var viewRadiusArray: ArrayList<Int> = arrayListOf()
    var focusWidth: Int = 0
    var focusHeight: Int = 0
    var viewRadius: Int = 0

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
        if (props.focusedViewArray.size > 0) {
            for (view in props.focusedViewArray) {
                if (view?.cantFocus() == true) {
                    view.waitForLayout { onShow() }
                } else {
                    onShow()
                }
            }
        } else {
            if (props.focusedView?.cantFocus() == true) {
                props.focusedView?.waitForLayout { onShow() }
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
       if ( props.focusedViewArray.size > 0) {
           for (view in props.focusedViewArray) {
               hasFocus = if (view != null) {
                   focusWidthArray.add(view.width())
                   focusHeightArray.add(view.height())
                   view.apply {
                       val center = getCircleCenter(this)
                       circleCenterXArray.add(center.x)
                       circleCenterYArray.add(center.y)
                   }

                   viewRadiusArray.add(((hypot(view.width().toDouble(), view.height().toDouble()) / 2).toInt() * props.focusCircleRadiusFactor).toInt())
                   true
               } else {
                   false
               }
           }
       } else {
           if (props.focusedView != null) {
               focusWidth = props.focusedView!!.width()
               focusHeight = props.focusedView!!.height()
               props.focusedView?.apply {
                   val center = getCircleCenter(this)
                   circleCenterX = center.x
                   circleCenterY = center.y
               }

               viewRadius = ((hypot(props.focusedView!!.width().toDouble(), props.focusedView!!.height().toDouble()) / 2).toInt() * props.focusCircleRadiusFactor).toInt()
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
        if (props.focusedViewArray.size > 0) {
            circleCenterXArray.add(positionX)
            circleCenterYArray.add(positionY)
            focusWidthArray.add(rectWidth)
            focusHeightArray.add(rectHeight)
        } else {
            circleCenterX = positionX
            circleCenterY = positionY
            focusWidth = rectWidth
            focusHeight = rectHeight
        }
        focusShape = FocusShape.ROUNDED_RECTANGLE
        hasFocus = true
    }

    private fun setCirclePosition(positionX: Int, positionY: Int, radius: Int) {
        if (props.focusedViewArray.size > 0) {
            circleCenterXArray.add(positionX)
            viewRadiusArray.add(radius)
            circleCenterYArray.add(positionY)
        } else {
            circleCenterX = positionX
            viewRadius = radius
            circleCenterY = positionY
        }
        focusShape = FocusShape.CIRCLE
        hasFocus = true
    }

    fun circleRadius(animCounter: Int, animMoveFactor: Double): Float {
        if (viewRadiusArray.size > 0) {
            for (radius in viewRadiusArray) {
                return (radius + animCounter * animMoveFactor).toFloat()
            }
        } else {
            return (viewRadius + animCounter * animMoveFactor).toFloat()
        }
        return 0f
    }

    fun roundRectLeft(animCounter: Int, animMoveFactor: Double): Float {
        if (circleCenterXArray.size > 0) {
            for ((index, center) in circleCenterXArray.withIndex()) {
                return (circleCenterXArray[index].toDouble() - (focusWidthArray[index] / 2).toDouble() - animCounter * animMoveFactor).toFloat()
            }
        } else {
            return (circleCenterX.toDouble() - (focusWidth / 2).toDouble() - animCounter * animMoveFactor).toFloat()
        }
        return 0f
    }


    fun roundRectTop(animCounter: Int, animMoveFactor: Double): Float {
        if (circleCenterYArray.size > 0) {
            for ((index, center) in circleCenterYArray.withIndex()) {
                return (circleCenterYArray[index].toDouble() - (focusHeightArray[index] / 2).toDouble() - animCounter * animMoveFactor).toFloat()
            }
        } else {
            return (circleCenterY.toDouble() - (focusHeight / 2).toDouble() - animCounter * animMoveFactor).toFloat()
        }
        return 0f
    }


    fun roundRectRight(animCounter: Int, animMoveFactor: Double): Float {
        if (circleCenterXArray.size > 0) {
            for ((index, center) in circleCenterXArray.withIndex()) {
                return (circleCenterXArray[index].toDouble() + (focusWidthArray[index] / 2).toDouble() + animCounter * animMoveFactor).toFloat()
            }
        } else {
            return (circleCenterX.toDouble() + (focusWidth / 2).toDouble() + animCounter * animMoveFactor).toFloat()
        }
        return 0f
    }


    fun roundRectBottom(animCounter: Int, animMoveFactor: Double): Float {
        if (circleCenterYArray.size > 0) {
            for ((index, center) in circleCenterYArray.withIndex()) {
                return (circleCenterYArray[index].toDouble() + (focusHeightArray[index] / 2).toDouble() + animCounter * animMoveFactor).toFloat()
            }
        } else {
            return (circleCenterY.toDouble() + (focusHeight / 2).toDouble() + animCounter * animMoveFactor).toFloat()
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
        if (props.focusedViewArray.size > 0) {
            for ((index, center) in props.focusedViewArray.withIndex()) {

                val halfViewHeight = if (focusShape == FocusShape.ROUNDED_RECTANGLE) focusHeightArray[index] / 2 else viewRadiusArray[index]

                if (spaceAbove > spaceBelow) {
                    autoPos.bottomMargin = bitmapHeight - (circleCenterYArray[index] + halfViewHeight)
                    autoPos.topMargin = 0
                    autoPos.height = top.toInt()
                } else {
                    autoPos.topMargin = circleCenterYArray[index] + halfViewHeight
                    autoPos.bottomMargin = 0
                    autoPos.height = (bitmapHeight - top).toInt()
                }
            }
        } else {
            val halfViewHeight = if (focusShape == FocusShape.ROUNDED_RECTANGLE) focusHeight / 2 else viewRadius

            if (spaceAbove > spaceBelow) {
                autoPos.bottomMargin = bitmapHeight - (circleCenterY + halfViewHeight)
                autoPos.topMargin = 0
                autoPos.height = top.toInt()
            } else {
                autoPos.topMargin = circleCenterY + halfViewHeight
                autoPos.bottomMargin = 0
                autoPos.height = (bitmapHeight - top).toInt()
            }
        }
        return autoPos
    }
}
