package com.e.tracker.osm

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.e.tracker.R
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow

class PathPaintingOverlay(context: Context) : View(context) {

    public enum class Mode {
        Polyline,
        Polygone,
        PolygoneHole,
        PolylineAsPath
    }

    private var drawingMode: Mode = Mode.PolylineAsPath

    var mBitmap: Bitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
    var mCanvas: Canvas = Canvas()
    var mPath: Path = Path()
    var map : MapView = MapView(context)
    var mPaint: Paint = Paint()
    var pts = arrayListOf<Point>()
    var mX: Float = 0f
    var mY: Float = 0f
    private val TOUCH_TOLERANCE = 4f


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(mBitmap)
    }


    override fun onDraw(canvas: Canvas?) {

        mCanvas = Canvas(mBitmap)
        mPaint = Paint()
        mPaint.isAntiAlias = true
        mPaint.isDither = true
        mPaint.color = resources.getColor( R.color.green_color)
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeJoin = Paint.Join.ROUND
        mPaint.strokeCap = Paint.Cap.ROUND
        mPaint.strokeWidth = 12f

        canvas?.drawPath(mPath, mPaint)

        //super.onDraw(canvas)
    }

    private fun touch_start(x: Float, y: Float) {
        mPath.reset()
        mPath.moveTo(x, y)
        mX = x
        mY = y
    }

    private fun touch_move(x: Float, y: Float) {
        val dx = Math.abs(x - mX)
        val dy = Math.abs(y - mY)
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2)
            mX = x
            mY = y
        }
    }



    private fun touch_up() {
        mPath.lineTo(mX, mY)

        // commit path to our offscreen
        mCanvas.drawPath(mPath, mPaint)
        // kill this so we don't double draq
        mPath.reset()

        if (map != null) {
            val projection = map.projection
            var geoPoints = arrayListOf<GeoPoint>()
            val unrotatedPoint = Point()
            for ( pt in pts) {
                projection.unrotateAndScalePoint(pt.x, pt.y, unrotatedPoint)
                val iGeoPoint = projection.fromPixels(unrotatedPoint.x, unrotatedPoint.y) as GeoPoint
                geoPoints.add(iGeoPoint)
            }

            if (geoPoints.size > 2) {

                val color = Color.argb(100, 100, 100, 100)
                var line = Polyline(map, true)
                line.infoWindow = BasicInfoWindow(org.osmdroid.library.R.layout.bonuspack_bubble, map)
                line.outlinePaint.color = color
                line.title = "Polyline as Path"
                line.setPoints(geoPoints)
                line.showInfoWindow()
                line.outlinePaint.strokeCap = Paint.Cap.ROUND
            }
        }
        map.invalidate()

        pts.clear()
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val x = event?.x ?: 0f
        val y = event?.y ?: 0f

        pts.add(Point(x.toInt(), y.toInt()))

        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                touch_start(x, y)
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                touch_move(x, y)
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                touch_up()
                invalidate()
            }
        }
        return true
    }

}