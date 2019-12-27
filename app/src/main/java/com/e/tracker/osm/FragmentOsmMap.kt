package com.e.tracker.osm

import android.graphics.Point
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.e.tracker.R
import com.e.tracker.databinding.FragmentOsmMapBinding
import com.e.tracker.track.TrackObject
import com.e.tracker.track.TrackSourceType
import kotlinx.android.synthetic.main.fragment_osm_map.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.*
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.*
import kotlin.math.abs


/**
 * Fragment with OpenStreetMap
 * On tap on map get address data and coordinates as GeoPoint
 * Put MapEventsOverlay on map to get events
 * Path without routing use a Polyline
 * Path with routing use OSRMRoadManager ()
 *
 * Map Toolbar
 * Edit button: onTap toggle state, active: add point to end of path
 * Add button: active: add point in selected path segment
 * Remove button: active: remove selected point
 *
 */
class FragmentOsmMap() : Fragment() {

    lateinit var trackObject: TrackObject

    private var path: Polyline = Polyline(map)
    val pathM : MutableLiveData<Polyline> = MutableLiveData(path)

    private var pathEditEnabled = false
    private var pathPointRemoveEnabled = false

    var projectedPoints: LongArray = longArrayOf()
    private val projectedCenter = PointL()
    private val isHorizontalRepeating = true
    private val isVerticalRepeating = true
    private val pointsForMilestones = ListPointL()

    // Marker
    private var selectedMarkers  = mutableListOf<Marker>()
    private var activeMarkerUid : String? = null
    private var activeMarker: Marker? = null

    //var pathOverlay = PathPaintingOverlay(requireContext())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val binding = DataBindingUtil.inflate<FragmentOsmMapBinding>(
            inflater, R.layout.fragment_osm_map, container, false)

        org.osmdroid.config.Configuration.getInstance().setUserAgentValue(this.context!!.packageName)

        binding.map.setUseDataConnection(true)
        binding.map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)

        binding.map.controller.setZoom(15.0)
        //binding.map.controller.setCenter(GeoPoint(52.4908, 13.4186))
        binding.map.controller.setCenter(GeoPoint(trackObject.latitude, trackObject.longitude))

        pathM.observe(this, Observer { newPathM -> path = pathM.value!! })

        // get tap location using MapEventsOverlay
        val mapEventsOverlay =  MapEventsOverlay( object : MapEventsReceiver {
            //val p : GeoPoint = GeoPoint(13.78, 52.12)
            override fun singleTapConfirmedHelper(p : GeoPoint) : Boolean {
                println("MapView Tap ${p.latitude}  ${p.longitude}")
                when (trackObject.type) {
                    TrackSourceType.DATABASE -> {
                        // close any marker dialogs, are there any properties in ItemizedOverlayWithFocus which i can use?
//                        var item = map.overlayManager.first { it is ItemizedOverlayWithFocus<*> } as ItemizedOverlayWithFocus<*>
//                        item.unSetFocusedItem()

                        //if( pathEditEnabled ) {
                        when (activeMarkerUid) {
                            "StartOfPath" -> {

                            }

                            "EndOfPath" -> {
                                trackObject.addCoord(geoPoint = p)
                                path.addPoint(p)

                                //map.overlayManager.overlays().first { it. }
                            }

                            "track_end" -> {
                                trackObject.addCoord(geoPoint = p)
                                path.addPoint(p)
                                activeMarker?.position = p
                            }
                        }

                        when (activeMarker?.id) {
                            "track_end" -> {
                                trackObject.addCoord(geoPoint = p)
                                path.addPoint(p)
                                activeMarker?.position = p
                            }
                            "track_start" -> {
                                trackObject.addCoord(geoPoint = p, position = 0)
                                path.setPoints(trackObject.coordsGpx)
                                activeMarker?.position = p
                            }
                        }

                        map.invalidate()
                        return true
                    }
                    TrackSourceType.FILE -> {}
                    TrackSourceType.NEW -> {
                        this@FragmentOsmMap.getLocationForTap(p)
                        return true
                    }
                }
                return false
            }

            override fun longPressHelper(p: GeoPoint) : Boolean {
                return true
            }
        } )

        binding.map.overlays.add(mapEventsOverlay)
        setHasOptionsMenu(true)

        binding.mapToolbarBtnEdit.alpha = 0.5f
        binding.mapToolbarBtnAddPoint.alpha = 0.5f
        binding.mapToolbarBtnAddPoint.isEnabled = false
        binding.mapToolbarBtnRemovePoint.alpha = 0.5f
        binding.mapToolbarBtnRemovePoint.isEnabled = false
        binding.mapToolbarBtnMovePoint.alpha = 0.5f

        binding.mapToolbarBtnEdit.setOnClickListener {  view ->
            onEditButton(binding.mapToolbarBtnEdit)
        }

        binding.mapToolbarBtnAddPoint.setOnClickListener{

        }


        binding.mapToolbarBtnRemovePoint.setOnClickListener {
            onRemoveButton(binding.mapToolbarBtnRemovePoint)
        }


        binding.mapToolbarBtnMovePoint.setOnClickListener {

        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //addPolyline()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        MenuBuilder(context).setOptionalIconsVisible(true)
        super.onCreateOptionsMenu(menu, inflater)
    }




    fun updateMap() {
        map.controller.setCenter(GeoPoint(trackObject.latitude, trackObject.longitude))

        (activity as? AppCompatActivity)?.supportActionBar?.title = trackObject.trackName

        addPathAsPolyline()
        //addTrackStartEndIcon()
        trackStartMarker()
        trackEndMarker()
        //addIconToPoints(trackObject.coordsGpx)
    }


    /**
     * Add start and end icon for path
     *
     */
    fun addTrackStartEndIcon() {
        var items = arrayListOf<OverlayItem>()
        if (trackObject.coordsGpx.isNotEmpty()) {
            items.add(OverlayItem( "StartOfPath", "Start", "Position", trackObject.coordsGpx[0]))
            if (trackObject.coordsGpx.size > 0 ) {
                //items.add(OverlayItem("End", "Position", trackObject.coordsGpx.last()))
                items.add(OverlayItem("EndOfPath", "End", "of Path", trackObject.coordsGpx.last()))
            }
        }
        //items.add(OverlayItem("My Position", "Here", GeoPoint(trackObject.latitude, trackObject.longitude)))

        val mIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_add_location_black_24dp)
        mIcon?.setTint(ContextCompat.getColor(requireContext(), R.color.schema_one_blue_light))

        val itemOverlay = ItemizedOverlayWithFocus<OverlayItem>(
            items,
            mIcon,
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_add_location_black_24dp),
            android.graphics.Color.LTGRAY,
            object : ItemizedIconOverlay.OnItemGestureListener<OverlayItem> {
                @Override
                override fun onItemSingleTapUp(index: Int, item: OverlayItem?): Boolean {
                    Log.d("MAP", "onItemSingleTapUp")
                    setAddButtonState(true, item?.uid, item )

                    return true
                }

                @Override
                override fun onItemLongPress(index: Int, item: OverlayItem?): Boolean {
                    Log.d("MAP", "onItemLongPress")
                    return true
                }
            }, context )

        itemOverlay.setFocusItemsOnTap(true)
        map.overlays.add(itemOverlay)
    }




    private fun trackStartMarker() {

        val mIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_add_location_black_24dp)
        mIcon?.setTint(ContextCompat.getColor(requireContext(), R.color.schema_one_blue_light))

        var startMarker = Marker(map)
        startMarker.position = trackObject.coordsGpx.first()
        startMarker.title = "Start"
        startMarker.isDraggable = false
        startMarker.id = "track_start"
        startMarker.icon = mIcon
        startMarker.setOnMarkerClickListener { marker, mapView ->
            Toast.makeText(
                context,
                marker.title.toString() + " was clicked",
                Toast.LENGTH_LONG
            ).show()
            activeMarker = marker
            setAddButtonState(true, marker.id, marker)
           // marker.showInfoWindow()
            true
        }
        map.overlays.add(startMarker)
    }


    private fun trackEndMarker() {

        val mIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_add_location_black_24dp)
        mIcon?.setTint(ContextCompat.getColor(requireContext(), R.color.schema_one_red))

        var endMarker = Marker(map)
        endMarker.position = trackObject.coordsGpx.last()
        endMarker.title = "End"
        endMarker.isDraggable = false
        endMarker.id = "track_end"
        endMarker.icon = mIcon
        endMarker.setOnMarkerClickListener { marker, mapView ->
            Toast.makeText(
                context,
                marker.title.toString() + " was clicked",
                Toast.LENGTH_LONG
            ).show()

            activeMarker = marker
            true
        }
        map.overlays.add(endMarker)

    }

//    val onMarkerTapListener: ( Marker) -> Marker.OnMarkerClickListener = {
//        when (it.id) {
//            "track_start" -> {
//                activeMarker = it
//            }
//            "track_end" -> {}
//        }
//        it.
//    }

//    fun onMarkerTapListener(marker: Marker) : Marker.OnMarkerClickListener {
//       when (marker.id) {
//           "track_start" -> {
//               activeMarker = marker
//           }
//           "track_end" -> {}
//       }
//       marker.id
//    }


    private fun updateStartIcon() {

    }

    /**
     * First version for adding a path to map
     * Add onClickListener to path (onClickOnPath)
     *
     */
    fun addPathAsPolyline() {
        // convert coords into GeoPoint's
        var geoPoints = arrayListOf<GeoPoint>()
        for ( coord in trackObject.coords) {
            geoPoints.add(GeoPoint(coord.latitude!!, coord.longitude!!))
        }

        // init and style path
        //var path = Polyline(map)
        path.outlinePaint.color = ContextCompat.getColor(requireContext(), R.color.schema_one_dark)

        path.setPoints(geoPoints)

        path.setOnClickListener(Polyline.OnClickListener { polyline, mapView, eventPos ->
            onClickOnPath(polyline, mapView, eventPos)
        })

        // save geoPoints of path in trackObject
        trackObject.coordsGpx = geoPoints

        map.overlays.add(path)
        map.invalidate()

    }

    /**
     * Path as Route, needs a key to work
     * Can't run in Main thread.
     * Can use withContext(Dispatchers.IO) to run
     *
     */
    fun addPathAsRoute() {

        // convert coords into GeoPoint's
        var geoPoints = arrayListOf<GeoPoint>()
        for ( coord in trackObject.coords) {
            geoPoints.add(GeoPoint(coord.latitude!!, coord.longitude!!))
        }

        val roadManager = OSRMRoadManager(requireContext())

        GlobalScope.launch(IO) {
                        val road = roadManager.getRoad(geoPoints)
            val roadOverlay = RoadManager.buildRoadOverlay(road)

            val nodeIcon = ContextCompat.getDrawable(requireContext(), R.drawable.marker_cluster)
            var nodeMarkers = mutableListOf<Marker>()

            for ( node in road.mNodes) {
                val nodeMarker = Marker(map)
                nodeMarker.position = node.mLocation
                nodeMarker.icon = nodeIcon
                nodeMarker.setTitle("road node")
                //map.overlays.add(nodeMarker)
                nodeMarkers.add(nodeMarker)
            }

            launch(Main) {
                map.overlays.add(roadOverlay)
                map.overlays.addAll(nodeMarkers)
                map.invalidate()

            }
        }

    }


    /**
     * Tap event
     * ToDo Send polyline to local getCloseTo function (for computeProjected())
     *
     * @param polyline
     * @param mapView
     * @param eventPos
     */
    fun onClickOnPath(polyline: Polyline, mapView: MapView, eventPos: GeoPoint) : Boolean {
        println("onClick at ${eventPos.latitude}, ${eventPos.longitude}")

        val pathPoint = polyline.getCloseTo(eventPos, 6.0, mapView)

        val segmentIndex = getCloseTo(eventPos, 6.0, mapView.projection, false)
        println("Pathpoint close to event: ${pathPoint.latitude}, ${pathPoint.longitude}")
        //println("PathpointNew: ${pathPointNew?.latitude}, ${pathPointNew?.longitude}")
        println("Index of Path segment: $segmentIndex")

        if (segmentIndex != null) {
            var pointsToMark = mutableListOf<GeoPoint>()
            val firstPointIdx = segmentIndex -1
            val secondPointIdx = segmentIndex
            pointsToMark.add(GeoPoint(polyline.points[firstPointIdx]))
            pointsToMark.add(GeoPoint(polyline.points[secondPointIdx]))
            addIconToPoints(pointsToMark)
        }
//        val pathPointIndex = polyline.points.indexOfFirst { it == pathPoint }

//        for (p in polyline.points) {
//            println("point: ${p.latitude}, ${p.longitude}")
//        }

//        println("Pathpoint index: $pathPointIndex")
//        println("Path distance: ${polyline.distance}")

        // use pathPoint to find point in geoPoints array
//        var minDistance : Double = MAX_VALUE
//        var closestPoint = -1
//        var pointsToMark = mutableListOf<GeoPoint>()
//
//        for (( i , p) in polyline.points.withIndex()) {
//            val dist = p.distanceToAsDouble(pathPoint)
//            if (minDistance > dist) {
//                minDistance = dist
//                closestPoint = i
//            }
//        }
//
//        // first point
//        println("Closest Point is_ $closestPoint with distance: $minDistance")
//        if (closestPoint == 0) {
//            pointsToMark.add(GeoPoint(polyline.points[closestPoint]))
//            pointsToMark.add(GeoPoint(polyline.points[closestPoint + 1]))
//        }
//
//        if (closestPoint > 0 && closestPoint < polyline.points.size - 1) {
//            // if distance closestPoint to closestPoint - 1 more then
//            // distance tap point on path to closestPoint - 1 then points are
//            // closestPoint - 1 / closestPoint
//            val distancePoints = polyline.points[closestPoint].distanceToAsDouble(polyline.points[closestPoint - 1])
//            val distanceTapToPoint = pathPoint.distanceToAsDouble(polyline.points[closestPoint - 1])
//            if (distancePoints < distanceTapToPoint) {
//                pointsToMark.add(GeoPoint(polyline.points[closestPoint]))
//                pointsToMark.add(GeoPoint(polyline.points[closestPoint + 1]))
//            } else {
//                pointsToMark.add(GeoPoint(polyline.points[closestPoint - 1]))
//                pointsToMark.add(GeoPoint(polyline.points[closestPoint]))
//            }
//
//        }
//
//        // last point
//        if (closestPoint == polyline.points.size - 1) {
//            pointsToMark.add(GeoPoint(polyline.points[closestPoint - 1]))
//            pointsToMark.add(GeoPoint(polyline.points[closestPoint]))
//        }
//
//
//        addIconToPoints(pointsToMark)

        return true
    }

    /**
     * Click on map event overlay
     *
     */
    fun onClickOnMap() {

    }


    /**
     *
     * @param marker tapped marker, must be on overlay
     */
    fun clickOnMarker(marker: Marker) : Boolean {

        if (selectedMarkers.contains(marker)) {
            println("clickOnMarke do something")

            marker.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_brightness_1_black_12dp)
            selectedMarkers.remove(marker)
        } else {
            // only one marker can be selected
            if (selectedMarkers.isNotEmpty()) {
                for ( selectedMarker in selectedMarkers) {
                    selectedMarker.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_brightness_1_black_12dp)
                    selectedMarkers.remove(selectedMarker)
                }
                map_toolbar_btn_removePoint.alpha = 0.5f
                map_toolbar_btn_removePoint.isEnabled = false
            }

            selectedMarkers.add(marker)
            marker.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_brightness_1_orange_12dp)

            // show toolbar for selected marker
            map_toolbar_btn_removePoint.alpha = 1.0f
            map_toolbar_btn_removePoint.isEnabled = true
        }

        map.invalidate()
        return true
    }


    /**
     * Put icons on top of GeoPoint's
     * This will aways clear all overlays of type Marker?
     * That could be a problem
     *
     * @param points
     */
    fun addIconToPoints(points: List<GeoPoint>) {

        //map.overlays[0].
        map.overlayManager.removeAll { it is Marker }

//        println("icon.bounds: ${i?.bounds!!.right}")
//        println("icon.intrinsicWidth: ${i.intrinsicWidth}")
//        i?.setBounds(0, 0, (i.intrinsicWidth * 0.5).toInt(), (i.intrinsicHeight * 0.5).toInt())

        for ( p in points ) {
            val marker = Marker(map)
            val i = ContextCompat.getDrawable(requireContext(), R.drawable.ic_brightness_1_black_12dp)
            marker.id = "marker_selected"
            marker.position = p
            marker.setAnchor(0.5f, 0.5f)
            marker.icon = i
            marker.icon.setTint(ContextCompat.getColor(requireContext(), R.color.schema_one_dark))
            //marker.icon.updateBounds(0, 0, i?.bounds!!.right, i?.bounds.bottom)
            marker.setInfoWindow(null)


            marker.setOnMarkerClickListener { marker: Marker, mapView: MapView ->
                println("Click on Marker")
                clickOnMarker(marker)
                //true
            }

            map.overlays.add(marker)
        }

        map.invalidate()
    }


    private fun onEditButton(btn : ImageButton) {
        //btn.isEnabled = !btn.isEnabled
        if (btn.alpha == 0.5f) {
            btn.alpha = 1.0f
            pathEditEnabled = true
        } else {
            btn.alpha = 0.5f
            pathEditEnabled = false
        }

    }

    /**
     * Remove first point in selectedMarker list
     *
     *
     */
    private  fun onRemoveButton(btn: ImageButton) {

        println("Remove path point")

        var markerGeoPoint = selectedMarkers[0].position
        trackObject.deleteCoord((markerGeoPoint))

        map.invalidate()
        // val pathIndex = path.points.indexOfFirst { it == markerGeoPoint }
        // get marker position in path point list

        // coords in TrackObject.coords

        //
//        if (btn.alpha == 0.5f) {
//            btn.alpha = 1.0f
//            pathPointRemoveEnabled = true
//        } else {
//            btn.alpha = 0.5f
//            pathPointRemoveEnabled = false
//        }
    }


    /**
     * Set state of toolbar button add depending situation
     * Path start marker uid: StartOfPath
     * Path end marker uid: EndOfPath
     *
     *
     */
    fun setAddButtonState(state : Boolean?, uid: String?, item: OverlayItem?) : Boolean {

        // if state parameter then set
        if ( state != null ) {
            map_toolbar_btn_addPoint.isEnabled = state

            if (state == true) {
                map_toolbar_btn_addPoint.alpha = 1.0f
            } else {
                map_toolbar_btn_addPoint.alpha = 0.5f
            }
            return true
        }

        // no state requested, toogle state of add button
        // close any marker dialogs, are there any properties in ItemizedOverlayWithFocus which i can use?
        val dialogItem = map.overlayManager.first { it is ItemizedOverlayWithFocus<*> } as ItemizedOverlayWithFocus<*>
        dialogItem.unSetFocusedItem()

        map_toolbar_btn_addPoint.isEnabled = !map_toolbar_btn_addPoint.isEnabled
        if (map_toolbar_btn_addPoint.isEnabled == true) {
            map_toolbar_btn_addPoint.alpha = 1.0f
            activeMarkerUid = uid
            //activeMarker = item
        } else {
            map_toolbar_btn_addPoint.alpha = 0.5f
            activeMarkerUid = null
        }

        return false
    }


    fun setAddButtonState(state: Boolean?, id: String, marker: Marker) : Boolean{
        // if state parameter then set
        if ( state != null ) {
            map_toolbar_btn_addPoint.isEnabled = state

            if (state == true) {
                map_toolbar_btn_addPoint.alpha = 1.0f
            } else {
                map_toolbar_btn_addPoint.alpha = 0.5f
            }
            return true
        }

        // no state requested, toogle state of add button
        // close any marker dialogs, are there any properties in ItemizedOverlayWithFocus which i can use?
        val dialogItem = map.overlayManager.first { it is ItemizedOverlayWithFocus<*> } as ItemizedOverlayWithFocus<*>
        dialogItem.unSetFocusedItem()

        map_toolbar_btn_addPoint.isEnabled = !map_toolbar_btn_addPoint.isEnabled
        if (map_toolbar_btn_addPoint.isEnabled == true) {
            map_toolbar_btn_addPoint.alpha = 1.0f
            activeMarkerUid = id
            //activeMarker = item
        } else {
            map_toolbar_btn_addPoint.alpha = 0.5f
            activeMarkerUid = null
        }

        return false
    }


    /**
     * Find address data for geoPoint (first one)
     *
     * @param geoPoint clicked point on map
     */
    fun getLocationForTap( geoPoint: GeoPoint) {

        val geoCoder = Geocoder(this.context)
        val addresses = geoCoder.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1)

        if (addresses.isNotEmpty()) {

            logAddress((addresses[0]))

            var addressSequence = arrayOf<CharSequence>()

            for( (index, address) in addresses.withIndex()) {

                addressSequence = addressSequence.plusElement( "${address.adminArea}, ${address.subLocality}")
            }

            addressSequence = addressSequence.plus("${addresses[0].getAddressLine(0)}")

            val dialogFragment = AdressesDialogFragment("Select Location", addressSequence, geoPoint)
            dialogFragment.show(this.fragmentManager!!, "mydia")
        }
    }



    fun logAddress( address: Address) {
        println("CountryName: ${address.countryName}")
        println("adminArea: ${address.adminArea}")
        println("featureName: ${address.featureName}")
        println("locality: ${address.locality}")
        println("subAdminArea: ${address.subAdminArea}")
        println("subLocality: ${address.subLocality}")
        println("maxAddressLineIndex: ${address.maxAddressLineIndex}")

        for( i in 0..address.maxAddressLineIndex ) {
            println("address line: ${address.getAddressLine(i)}")
        }
    }


    /**
     * Get polyline segment with pPoint.
     * Detection is done in screen coordinates
     *
     * This function comes from From LinearRing.java. Also
     * computeProjected(), setCloserPoint(), getBestOffset(), clipAndStore()
     *
     * @param pPoint
     * @param tolerance in pixels
     * @param pProjection
     * @param pClosePath
     * @return Index of path segemnt with pPoint in it
     */
    fun getCloseTo(pPoint: GeoPoint, tolerance: Double, pProjection: Projection, pClosePath: Boolean) : Int? {

        computeProjected(pProjection)
        val pixel: Point = pProjection.toPixels(pPoint, null)
        val offset: PointL = PointL()
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
//                                val pointAX: Long = projectedPoints.get(2 * (index - 1))
//                                val pointAY: Long = projectedPoints.get(2 * (index - 1) + 1)
//                                val pointBX: Long = projectedPoints.get(2 * index)
//                                val pointBY: Long = projectedPoints.get(2 * index + 1)
//                                val projectionX =
//                                    (pointAX + (pointBX - pointAX) * projectionFactor).toLong()
//                                val projectionY =
//                                    (pointAY + (pointBY - pointAY) * projectionFactor).toLong()

                                return index
//                                return MapView.getTileSystem()
//                                    .getGeoFromMercator(
//                                        projectionX, projectionY, pProjection.mProjectedMapSize,
//                                        null, false, false
//                                    )
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
     */
    fun computeProjected( pProjection: Projection) {
        // points in path * 2
        //projectedPoints = mutableListOf<Long>((path.points.size * 2).toLong())
        projectedPoints = LongArray(trackObject.coordsGpx.size * 2)


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
        for (currentGeo in path.points) {
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


    private fun getBestOffset(pPolyCenterX: Double, pPolyCenterY: Double,
                              pScreenCenterX: Double, pScreenCenterY: Double,
                              pWorldSize: Double, pOffset: PointL) {

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
        pPolyCenterX: Double, pPolyCenterY: Double,
        pScreenCenterX: Double, pScreenCenterY: Double,
        pDeltaX: Long, pDeltaY: Long
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


    private fun clipAndStore(
        pProjection: Projection, pOffset: PointL,
        pClosePath: Boolean, pStorePoints: Boolean,
        pSegmentClipper: SegmentClipper?
    ) {
        pointsForMilestones.clear()
        val powerDifference = pProjection.projectedPowerDifference
        val projected = PointL()
        val point = PointL()
        val first = PointL()
        var i = 0
        while (i < projectedPoints.size) {
            projected[projectedPoints.get(i)] = projectedPoints.get(i + 1)
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
}

//org.osmdroid.bonuspack.kml.KmlPoint.
//        val geoJson = JSONObject(mapOf(
//            "type" to "Feature",
//            "geometry" to mapOf(
//                "type" to "Point",
//                "coordinates" to listOf(geoPoint.latitude, geoPoint.longitude)
//            )
//        ))
//
//        println("geoJson: $geoJson")