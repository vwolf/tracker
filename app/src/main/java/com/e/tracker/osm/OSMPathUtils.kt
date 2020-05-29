package com.e.tracker.osm

import android.graphics.Point
import com.e.tracker.database.TrackCoordModel
import com.e.tracker.xml.gpx.domain.Bounds
import org.osmdroid.util.*
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Polyline
import kotlin.math.abs

class OSMPathUtils {

    private var projectedPoints: LongArray = longArrayOf()
    private val projectedCenter = PointL()
    private val isHorizontalRepeating = true
    private val isVerticalRepeating = true
    private val pointsForMilestones = ListPointL()

    /**
     * Get polyline segment with pPoint.
     * Detection is done in screen coordinates
     *
     * This function comes from From LinearRing.java. Also
     * computeProjected(), setCloserPoint(), getBestOffset(), clipAndStore()
     *
     * @param pPath selected path
     * @param pPoint selected point on pPath
     * @param tolerance in pixels
     * @param pProjection
     * @param pClosePath
     * @return Index of path segemnt with pPoint in it
     */
    fun getCloseTo(pPath: Polyline, pPoint: GeoPoint, tolerance: Double, pProjection: Projection, pClosePath: Boolean) : Int? {

        computeProjected(pProjection, pPath)
        val pixel: Point = pProjection.toPixels(pPoint, null)
        val offset = PointL()
        getBestOffset(pProjection, offset)
        clipAndStore(pProjection, offset, pClosePath, true, null)
        val mapSize = TileSystem.MapSize(pProjection.zoomLevel)
        val screenRect = pProjection.intrinsicScreenRect
        val screenWidth = screenRect.width()
        val screenHeight = screenRect.height()
        var startX = pixel.x.toDouble()
        while(startX - mapSize >= 0) {
            startX -= mapSize
        }
        var startY = pixel.y.toDouble()
        while (startY - mapSize >= 0) {
            startY -= mapSize
        }
        val squaredTolerance = tolerance * tolerance
        val point0 = PointL()
        val point1 = PointL()
        var first = true
        var index = 0

        for (point in pointsForMilestones) {
            point1.set(point)
            if (first) {
                first = false
            } else {
                var x = startX
                while (x < screenWidth) {
                    var y = startY
                    while (y < screenHeight) {
                        val projectionFactor =
                            Distance.getProjectionFactorToSegment(
                                x,
                                y,
                                point0.x.toDouble(),
                                point0.y.toDouble(),
                                point1.x.toDouble(),
                                point1.y.toDouble()
                            )
                        val squaredDistance =
                            Distance.getSquaredDistanceToProjection(
                                x,
                                y,
                                point0.x.toDouble(),
                                point0.y.toDouble(),
                                point1.x.toDouble(),
                                point1.y.toDouble(),
                                projectionFactor
                            )
                        if (squaredTolerance > squaredDistance) {
                            return index
                        }
                        y += mapSize
                    }
                    x += mapSize

                }
            }

            point0.set(point1)
            index++
        }
        return null
    }


    /**
     * ToDo Get the polyline to fill projectedPoints
     *
     * @param pProjection
     * @param pPath
     */
    fun computeProjected( pProjection: Projection, pPath: Polyline) {
        // points in path * 2
        //projectedPoints = mutableListOf<Long>((path.points.size * 2).toLong())

        //projectedPoints = LongArray(trackObject.coordsGpx.size * 2)
        projectedPoints = LongArray(pPath.points.size * 2)

//        if (projectedPoints == null || projectedPoints.size != trackObject.coordsGpx.size * 2) {
//            projectedPoints = LongArray(projectedPoints.size * 2)
//        }
        var minX = 0L
        var maxX = 0L
        var minY = 0L
        var maxY = 0L
        var index = 0
        val previous = PointL()
        val current = PointL()
        for (currentGeo in pPath.points) {
            pProjection.toProjectedPixels(currentGeo.latitude, currentGeo.longitude, false, current)

            if (index == 0) {
                minX = current.x
                maxX = current.x
                minY = current.y
                maxY = current.y
            } else {
                setCloserPoint(previous, current, pProjection.mProjectedMapSize)
                if (minX > current.x) {
                    minX = current.x
                }
                if (maxX < current.x) {
                    maxX = current.x
                }
                if (minY > current.y) {
                    minY = current.y
                }
                if (maxY < current.y) {
                    maxY = current.y
                }
            }
            projectedPoints[2 * index] = current.x
            projectedPoints[2 * index + 1] = current.y
            previous.set(current.x, current.y)
            index++
        }
        projectedCenter.set((minX + maxX) / 2, (minY + maxY) / 2)
    }


    private fun setCloserPoint(pPrevious: PointL, pNext: PointL, pWorldSize: Double) {
        while (isHorizontalRepeating && abs(pNext.x - pWorldSize - pPrevious.x) < abs(
                pNext.x - pPrevious.x
            )
        ) {
            pNext.x -= pWorldSize.toLong()
        }
        while (isHorizontalRepeating && abs(pNext.x + pWorldSize - pPrevious.x) < abs(
                pNext.x - pPrevious.x
            )
        ) {
            pNext.x += pWorldSize.toLong()
        }
        while (isVerticalRepeating && abs(pNext.y - pWorldSize - pPrevious.y) < abs(
                pNext.y - pPrevious.y
            )
        ) {
            pNext.y -= pWorldSize.toLong()
        }
        while (isVerticalRepeating && abs(pNext.y + pWorldSize - pPrevious.y) < abs(
                pNext.y - pPrevious.y
            )
        ) {
            pNext.y += pWorldSize.toLong()
        }
    }

    /**
     * Compute the pixel offset so that a list of pixel segments display in the best possible way:
     * the center of all pixels is as close to the screen center as possible
     * This notion of pixel offset only has a meaning on very low zoom level,
     * when a GeoPoint can be projected on different places on the screen.
     *
     * @param pProjection
     * @param pOffset
     */
    private fun getBestOffset(pProjection: Projection, pOffset: PointL) {
        val powerDifference = pProjection.projectedPowerDifference
        val center = pProjection.getLongPixelsFromProjected(
            projectedCenter, powerDifference, false, null
        )
        val screenRect = pProjection.intrinsicScreenRect
        val screenCenterX = (screenRect.left + screenRect.right) / 2.0
        val screenCenterY = (screenRect.top + screenRect.bottom) / 2.0
        val worldSize = TileSystem.MapSize(pProjection.zoomLevel)
        getBestOffset(
            center.x.toDouble(),
            center.y.toDouble(),
            screenCenterX,
            screenCenterY,
            worldSize,
            pOffset
        )
    }


    private fun getBestOffset(
        pPolyCenterX: Double,
        pPolyCenterY: Double,
        pScreenCenterX: Double,
        pScreenCenterY: Double,
        pWorldSize: Double,
        pOffset: PointL) {

        val worldSize = Math.round(pWorldSize)
        var deltaPositive = 0
        var deltaNegative = 0
        if (!isVerticalRepeating) {
            deltaPositive = 0
            deltaNegative = 0
        } else {
            deltaPositive = getBestOffset(
                pPolyCenterX, pPolyCenterY, pScreenCenterX, pScreenCenterY, 0, worldSize
            )
            deltaNegative = getBestOffset(
                pPolyCenterX, pPolyCenterY, pScreenCenterX, pScreenCenterY, 0, -worldSize
            )

        }

        pOffset.y =
            worldSize * if (deltaPositive > deltaNegative) deltaPositive else -deltaNegative
        if (!isHorizontalRepeating) {
            deltaPositive = 0
            deltaNegative = 0
        } else {
            deltaPositive = getBestOffset(
                pPolyCenterX, pPolyCenterY, pScreenCenterX, pScreenCenterY, worldSize, 0
            )
            deltaNegative = getBestOffset(
                pPolyCenterX, pPolyCenterY, pScreenCenterX, pScreenCenterY, -worldSize, 0
            )
        }
        pOffset.x =
            worldSize * if (deltaPositive > deltaNegative) deltaPositive else -deltaNegative
    }


    private fun getBestOffset(
        pPolyCenterX: Double,
        pPolyCenterY: Double,
        pScreenCenterX: Double,
        pScreenCenterY: Double,
        pDeltaX: Long,
        pDeltaY: Long
    ): Int {
        var squaredDistance = 0.0
        var i = 0
        while (true) {
            val tmpSquaredDistance = Distance.getSquaredDistanceToPoint(
                pPolyCenterX + i * pDeltaX, pPolyCenterY + i * pDeltaY,
                pScreenCenterX, pScreenCenterY
            )
            if (i == 0 || squaredDistance > tmpSquaredDistance) {
                squaredDistance = tmpSquaredDistance
                i++
            } else {
                break
            }
        }
        return i - 1
    }

    /**
     * Fill pointsForMilestones
     *
     * @param pProjection
     * @param pOffset
     * @param pClosePath
     * @param pStorePoints
     * @param pSegmentClipper
     */
    private fun clipAndStore(
        pProjection: Projection,
        pOffset: PointL,
        pClosePath: Boolean,
        pStorePoints: Boolean,
        pSegmentClipper: SegmentClipper?
    ) {
        pointsForMilestones.clear()
        val powerDifference = pProjection.projectedPowerDifference
        val projected = PointL()
        val point = PointL()
        val first = PointL()
        var i = 0
        while (i < projectedPoints.size) {
            projected[projectedPoints.get(i)] = projectedPoints[i + 1]
            pProjection.getLongPixelsFromProjected(projected, powerDifference, false, point)
            val x = point.x + pOffset.x
            val y = point.y + pOffset.y
            if (pStorePoints) {
                pointsForMilestones.add(x, y)
            }
            pSegmentClipper?.add(x, y)
            if (i == 0) {
                first[x] = y
            }
            i += 2
        }
        if (pClosePath) {
            pSegmentClipper?.add(first.x, first.y)
            if (pStorePoints) {
                pointsForMilestones.add(first.x, first.y)
            }
        }
    }


    /**
     * Create a new Bounds object
     *
     * coords = mutableListOf<TrackCoordModel>
     */
    public  fun createBounds(coords: MutableList<TrackCoordModel>): BoundingBox {
        var minLat: Double = coords[0].latitude
        var maxLat: Double = coords[0].latitude
        var minLon: Double = coords[0].longitude
        var maxLon: Double = coords[0].longitude

        for (c in coords) {
            minLat = minOf(c.latitude, minLat)
            maxLat = maxOf(c.latitude, maxLat)
            minLon = minOf(c.longitude, minLon)
            maxLon = maxOf(c.longitude, maxLon)
        }

        var boundingBox = BoundingBox(maxLat, maxLon, minLat, minLon)

        return boundingBox
    }
}