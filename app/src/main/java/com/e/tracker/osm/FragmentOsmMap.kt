package com.e.tracker.osm


import android.content.Context
import android.graphics.Point
import android.location.Address
import android.location.Geocoder
import android.location.Location
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
import com.e.tracker.database.TrackWayPointModel
import com.e.tracker.databinding.FragmentOsmMapBinding
import com.e.tracker.track.TrackActionType
import com.e.tracker.track.TrackMarkerType
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
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.*
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.*
import kotlin.reflect.KProperty


//var Marker.type: String = ""
//    get() = "o"
//    set(value) {
//
//    }
    //get() = this.type
    //set(value) {  type = value}

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
 * ToDo Update of path start and end marker
 * ToDo Move start marker icon when moveable action
 * ToDo Make end marker moveable
 */
class FragmentOsmMap : Fragment() {

    lateinit var trackObject: TrackObject

    private var path: Polyline = Polyline(map)
    private val pathM: MutableLiveData<Polyline> = MutableLiveData(path)

    private var pathEditEnabled = false
    lateinit var mapEventsOverlay: MapEventsOverlay

    // Marker extension with FieldProperty to act as binding field
    // Should be implemented as hashtable
    // https://github.com/h0tk3y/kotlin-fun/blob/master/src/main/java/com/github/h0tk3y/kotlinFun/util/WeakIdentityHashMap.java
    var Marker.type: TrackMarkerType by FieldProperty { TrackMarkerType.UNDEFINED }

    // keep markers
//    var markerList = mutableMapOf<String, Marker>()
//    var markerSet = mutableSetOf<Marker>()

    //private var pathPointRemoveEnabled = false

//    private var projectedPoints: LongArray = longArrayOf()
//    private val projectedCenter = PointL()
//    private val isHorizontalRepeating = true
//    private val isVerticalRepeating = true
//    private val pointsForMilestones = ListPointL()

    // Path segment
    private var selectedSegment = -1

    // Marker
    private var selectedMarkers = mutableListOf<Marker>()
    var selectedMarkersPathPosition = mutableListOf<Int>()
    private var activeMarkerUid: String? = null
    private var activeMarker: Marker? = null

    // map scroll action
    private var scrollAction = false
    private var selectedMarkerStartPos = GeoPoint(0.0, 0.0)
    private var selectedMarkerStartPosPixel = Point(0, 0)

    //var fusedLocationProviderClient: FusedLocationProviderClient? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val binding = DataBindingUtil.inflate<FragmentOsmMapBinding>(
            inflater, R.layout.fragment_osm_map, container, false
        )

        org.osmdroid.config.Configuration.getInstance()
            .userAgentValue = this.context!!.packageName
            //.setUserAgentValue(this.context!!.packageName)


        binding.map.setUseDataConnection(true)
        binding.map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)

        binding.map.controller.setZoom(15.0)
        //binding.map.controller.setCenter(GeoPoint(52.4908, 13.4186))
        binding.map.controller.setCenter(GeoPoint(trackObject.latitude, trackObject.longitude))

        pathM.observe(this, Observer { path = pathM.value!! })

        attachEventsOverlay()

        binding.map.overlays.add(mapEventsOverlay)

        setHasOptionsMenu(true)

        if (trackObject.trackSourceType == TrackSourceType.FILE) {
            binding.mapToolbar.visibility = View.INVISIBLE
            binding.mapStaticToolbar.visibility = View.VISIBLE

            binding.mapStaticBtnInfo.setOnClickListener {
                onStaticPathInfo()
            }

            binding.mapStaticBtnGps.setOnClickListener {
                onGpsButton()
            }

        } else {
            binding.mapToolbar.visibility = View.VISIBLE
            binding.mapStaticToolbar.visibility = View.INVISIBLE

            // button gps on/off
            binding.mapToolbarBtnGps.alpha = 0.5f
            binding.mapToolbarBtnGps.setOnClickListener {
                onEditButton(binding.mapToolbarBtnGps)
            }

            // button add point to path
            binding.mapToolbarBtnAddPoint.alpha = 0.5f
            binding.mapToolbarBtnAddPoint.isEnabled = false
            binding.mapToolbarBtnAddPoint.setOnClickListener {
                onAddPointButton(binding.mapToolbarBtnAddPoint)
            }

            // button remove point from path
            binding.mapToolbarBtnRemovePoint.alpha = 0.5f
            binding.mapToolbarBtnRemovePoint.isEnabled = false
            binding.mapToolbarBtnRemovePoint.setOnClickListener {
                onRemoveButton(binding.mapToolbarBtnRemovePoint)
            }

            // button move point in path on/off
            binding.mapToolbarBtnMovePoint.alpha = 0.5f
            binding.mapToolbarBtnMovePoint.isEnabled = false
            binding.mapToolbarBtnMovePoint.setOnClickListener {
                onMovePointButton(binding.mapToolbarBtnMovePoint)
            }

            // button show path/track info
            binding.mapToolbarBtnInfo.setOnClickListener {
                onStaticPathInfo()
            }

            // button add a waypoint item to path point
            binding.mapToolbarBtnAddItem.alpha = 0.5f
            binding.mapToolbarBtnAddItem.isEnabled = false
            binding.mapToolbarBtnAddItem.setOnClickListener {
                onAddPathWayPoint()
            }
        }


        //fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        Log.i("LOG", "map tileSource: ${binding.map.tileProvider.tileSource}")
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //addPolyline()
        if (trackObject.trackSourceType == TrackSourceType.FILE) {
            updateMap()
        }
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

    /**
     * Attach a MapEventsOverlay to map
     * Get events from map
     *
     *
     */
    private fun attachEventsOverlay() {
        mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {

            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                Log.i(OSM_LOG, "MapView Tap ${p.latitude}  ${p.longitude}")
                when (trackObject.trackSourceType) {
                    TrackSourceType.DATABASE -> {
                        when (activeMarker?.type) {
                            TrackMarkerType.START -> {
                                Log.i(OSM_LOG, "singleTap on mapEventsOverlay, ${activeMarker?.type}")
                                trackObject.addCoord(
                                    geoPoint = p,
                                    position = 1,
                                    res = { updatePath(TrackActionType.AddAtStart) })
                                activeMarker?.position = p
                                map.invalidate()
                            }

                            TrackMarkerType.END -> {
                                Log.i(OSM_LOG, "singleTap on mapEventsOverlay, ${activeMarker?.type}")
                                trackObject.addCoord(
                                    geoPoint = p,
                                    res = { updatePath(TrackActionType.AddAtEnd) })
                                path.addPoint(p)
                                activeMarker?.position = p
                                map.invalidate()
                            }

                            TrackMarkerType.POINT -> {
                                Log.i(OSM_LOG, "singleTap on mapEventsOverlay, ${activeMarker?.type}")
                            }

                            TrackMarkerType.UNDEFINED -> {
                                Log.i(OSM_LOG, "singleTap on mapEventsOverlay, ${activeMarker?.type}")
                            }

                            TrackMarkerType.POINTSELECTED -> {
                                Log.i(OSM_LOG, "singleTap on mapEventsOverlay, ${activeMarker?.type}")
                                // srollAction? (move point)
                                if (scrollAction) {

                                }
                            }

                            else -> {
                                // no activeMarker, segment selected then deselect
                                if (selectedSegment > -1) {
                                    unSelectSegment()
                                    map.invalidate()
                                }
                            }
                        }
                    }

                    TrackSourceType.FILE -> {}

                    TrackSourceType.NEW -> {
                        this@FragmentOsmMap.getLocationForTap(p)
                        return true
                    }

                }
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                Log.i(OSM_LOG, "MapView LongTap ${p.latitude}, ${p.longitude}")
                return true
            }
        })
    }


    fun showWayPoints(state: Boolean) {
        Log.i(OSM_LOG, "showWayPoints state: $state")
        if (state) {
            showWayPoints()
        } else {
            hideWayPoints()
        }
    }

    /**
     * Put an icon on all waypoints
     *
     */
    fun showWayPoints() {
        Log.i(OSM_LOG, "showWayPoints")
        val mIcon =
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_explore_black_24dp)
                ?.mutate()
        mIcon?.setTint(ContextCompat.getColor(requireContext(), R.color.schema_one_green))

        for( wp in trackObject.wayPoints) {

            val wpMarker = Marker(map)
            wpMarker.position = trackObject.coordsGpx[wp.pointId.toInt() - 1]
            wpMarker.setAnchor(0.5f, 0.5f)
            wpMarker.title = wp.wayPointName
            wpMarker.isDraggable = false
            wpMarker.id = "waypoint"
            wpMarker.icon = mIcon

            wpMarker.setOnMarkerClickListener { marker, mapView ->
                Log.i(OSM_LOG, "onClickListener for waypoint id: ${wp.pointId}")

                osmBottomSheet?.openOsmBottomSheetWithContent("WayPoint", wp)
//                val dialog = WayPointBottomSheetDialog.getInstance(
//                    wp,
//                    {wayPointAction("Edit")},
//                    {wayPointAction("Delete")}
//                )
//                dialog.show(requireFragmentManager(), WayPointBottomSheetDialog::class.java.simpleName)

                //marker.showInfoWindow()
                true
            }

            wpMarker.type = TrackMarkerType.WAYPOINT
            map.overlays.add(wpMarker)
        }

        map.invalidate()
    }


    fun hideWayPoints() {
        Log.i(OSM_LOG, "hideWayPoints")
        removeMarkersOfType( TrackMarkerType.WAYPOINT )
        map.invalidate()
    }


    /**
     * Return from waypoint bottomsheet
     *
     * @param callback Edit or Delete
     */
    fun wayPointAction(callback: String) {
        Log.i(OSM_LOG, "wayPointAction: $callback ")

    }


    /**
     * This comes from activity, is touchUp event on map
     * Use it to update after scroll action
     * Update trackObject.coords, save to db ...
     *
     */
    fun receiveActionUP() {

        Log.i("LOG", "ACTIONUP map center: ${map.mapCenter}, center offset x: ${map.mapCenterOffsetX}")

        // important for OnScroll(), makes sure to get right scroll event distance
        map.controller.setCenter(map.mapCenter)

        if (scrollAction) {
            Log.i("LOG", "receiveActionUp during scrollAction, selected marker path position: ${selectedMarkersPathPosition.first()}")
            scrollAction = false
            val pathIndex = selectedMarkersPathPosition.first()
            trackObject.coords[pathIndex].latitude = trackObject.coordsGpx[pathIndex].latitude
            trackObject.coords[pathIndex].longitude = trackObject.coordsGpx[pathIndex].longitude

            // save changed coord
            trackObject.updateCoordPosition(trackObject.coords[pathIndex])
        } else {
            // deselect selected segment
            if(selectedSegment > -1 && selectedMarkers.isNotEmpty()) {
                //unSelectSegment()
            }

            if (selectedSegment > -1) {
                //unSelectSegment()
            }

        }
        map.invalidate()

    }

    /**
     * Interface MapListeners
     * Subscribe to scroll event to implement moving marker
     * For each new scroll action reset values for position of selected marker
     * Scroll action starts with MotionEvent.ACTION_MOVE and ends with MotionEvent.ACTION_UP
     *
     *
     */
    private var maplistener: MapListener = object : MapListener {
        @Override
        override fun onScroll(event: ScrollEvent?): Boolean {
            //println("Event x: ${event.toString()}")

            if (!scrollAction) {
                Log.i("LOG", "SelectedMarkers.size: ${selectedMarkers.size}")
                selectedMarkerStartPos = selectedMarkers.first().position
                selectedMarkerStartPosPixel = map.projection.toPixels(selectedMarkerStartPos, null)
                Log.i("LOG", "Marker start position scrollAction: $selectedMarkerStartPos, posPixel: $selectedMarkerStartPosPixel")
                scrollAction = true
            }

            if (event != null && scrollAction) {
                Log.i("LOG", "Scrollaction event consum with startPosPixel: $selectedMarkerStartPosPixel")
                val smNewPositionX = selectedMarkerStartPosPixel.x + event.x
                val smNewPositionY = selectedMarkerStartPosPixel.y + event.y
                Log.i("Log", "new position: x: $smNewPositionX, y: $smNewPositionY")
                val smNewPosition = map.projection.fromPixels(smNewPositionX, smNewPositionY)
                selectedMarkers.first().position =
                    GeoPoint(smNewPosition.latitude, smNewPosition.longitude)

                // update path point
//                path.points[selectedMarkersPathPosition.first()].latitude = smNewPosition.latitude
//                path.points[selectedMarkersPathPosition.first()].longitude = smNewPosition.longitude
                trackObject.coordsGpx[selectedMarkersPathPosition.first()].latitude =
                    smNewPosition.latitude
                trackObject.coordsGpx[selectedMarkersPathPosition.first()].longitude =
                    smNewPosition.longitude

                updatePath(TrackActionType.MoveMarker)
            }

            return true
        }

        /**
         * Remove MapListener when zooming, otherwise the selected marker moves with zoom action
         *
         */
        @Override
        override fun onZoom(event: ZoomEvent): Boolean {
            println("Zoom ${event.toString()}")
            removeMapListener()

            map_toolbar_btn_movePoint.isEnabled = false
            map_toolbar_btn_movePoint.alpha = 0.5f

            clickOnMarker(selectedMarkers.first(), 0)
            return true
        }
    }

    /**
     *
     */
    private fun addMapListener() {
        //val moveListener   = MapListener{}
        if (selectedMarkers.isNotEmpty()) {
            selectedMarkers.first().isDraggable = false
            map.addMapListener(maplistener)
        }
    }


    private fun removeMapListener() {
        map.removeMapListener(maplistener)
    }


    fun updateMap() {
        map.controller.setCenter(GeoPoint(trackObject.latitude, trackObject.longitude))

        (activity as? AppCompatActivity)?.supportActionBar?.title = trackObject.trackName

        addPathAsPolyline()
        trackStartMarker()
        trackEndMarker()
        //addIconToPoints(trackObject.coordsGpx)
    }





    private fun trackStartMarker() {

        val mIcon =
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_add_location_black_24dp)
                ?.mutate()
        mIcon?.setTint(ContextCompat.getColor(requireContext(), R.color.schema_one_green))

        val startMarker = Marker(map)
        startMarker.position = trackObject.coordsGpx.first()
        startMarker.title = "Track Start"
        startMarker.isDraggable = false
        startMarker.id = "track_start"
        startMarker.icon = mIcon
        startMarker.setOnMarkerClickListener { marker, mapView ->
            var toastText = marker.title.toString()
            if (activeMarker == marker) {
                setAddButtonState(false, marker.id, marker)
                activeMarker = null
                mIcon?.setTint(ContextCompat.getColor(requireContext(), R.color.schema_one_red))
                map.invalidate()
                toastText += " is not active."
            } else {
                if (activeMarker?.type == TrackMarkerType.END) {
                    activeMarker?.icon?.setTint(ContextCompat.getColor(requireContext(), R.color.schema_one_red))
                }
                activeMarker = marker
                setAddButtonState(true, marker.id, marker)
                mIcon?.setTint(ContextCompat.getColor(requireContext(), R.color.schema_one_blue))
                map.invalidate()
                toastText += " is active. Touch on map to extend track."
            }

            Toast.makeText(
                context,
                toastText,
                Toast.LENGTH_LONG
            ).show()

            //marker.showInfoWindow()
            true
        }

        startMarker.type = TrackMarkerType.START
        map.overlays.add(startMarker)
    }

    /**
     * Marker for end of path.
     * When this marker is selected, each tap on map extends path to tap position
     */
    private fun trackEndMarker() {

        val mIcon =
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_add_location_black_24dp)
                ?.mutate()
        mIcon?.setTint(ContextCompat.getColor(requireContext(), R.color.schema_one_red))

        val endMarker = Marker(map)
        endMarker.position = trackObject.coordsGpx.last()
        endMarker.title = "Track End"
        endMarker.isDraggable = false
        endMarker.id = "track_end"
        endMarker.icon = mIcon
        endMarker.setOnMarkerClickListener { marker, mapView ->
            var toastText = marker.title.toString()
            if (activeMarker == marker) {
                setAddButtonState(false, marker.id, marker)
                activeMarker = null
                mIcon?.setTint(ContextCompat.getColor(requireContext(), R.color.schema_one_red))
                map.invalidate()
                toastText += " is not active."
            } else {
                activeMarker = marker
                setAddButtonState(true, marker.id, marker)
                mIcon?.setTint(ContextCompat.getColor(requireContext(), R.color.schema_one_blue))
                map.invalidate()
                //marker.showInfoWindow()
                toastText += " is active. Touch on map to extend track."
            }

            Toast.makeText(
                context,
                toastText,
                Toast.LENGTH_LONG
            ).show()

            true
        }
        endMarker.type = TrackMarkerType.END
        map.overlays.add(endMarker)

    }


    /**
     * Update path after action
     * Use property path, only works with on path on map
     *
     * @param status
     */
    fun updatePath(status: TrackActionType) {
        println("updatePath: $status")
        when (status) {
            TrackActionType.AddAtStart -> {
            }
            TrackActionType.AddAtEnd -> {
            }
            TrackActionType.RemoveSelected -> {
                // update selected marker
                val sm = selectedMarkersPathPosition.first()
                if (sm < path.points.size) {
                    selectedSegment = +1
                    selectSegment(path, selectedSegment)
                } else {
                    // last point in path?
                }
                // set state of delete button in statusbar
            }

            TrackActionType.AddMarker -> {
                path.setPoints(trackObject.coordsGpx)
                // update marker, set second marker to new marker
                // value selectedSegment is not changed, new segment added
                selectSegment(path, selectedSegment)
            }
        }

        path.setPoints(trackObject.coordsGpx)
        map.invalidate()
    }

    /**
     * First version for adding a path to map
     * Add onClickListener to path (onClickOnPath)
     *
     */
    private fun addPathAsPolyline() {
        // convert coords into GeoPoint's
        val geoPoints = arrayListOf<GeoPoint>()
        for (coord in trackObject.coords) {
            geoPoints.add(GeoPoint(coord.latitude!!, coord.longitude!!))
        }

        // init and style path
        //var path = Polyline(map)
        path.outlinePaint.color = ContextCompat.getColor(requireContext(), R.color.schema_one_dark)

        path.setPoints(geoPoints)

        path.setOnClickListener { polyline, mapView, eventPos ->
            onClickOnPath(polyline, mapView, eventPos)
        }

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
        val geoPoints = arrayListOf<GeoPoint>()
        for (coord in trackObject.coords) {
            geoPoints.add(GeoPoint(coord.latitude!!, coord.longitude!!))
        }

        val roadManager = OSRMRoadManager(requireContext())

        GlobalScope.launch(IO) {
            val road = roadManager.getRoad(geoPoints)
            val roadOverlay = RoadManager.buildRoadOverlay(road)

            val nodeIcon = ContextCompat.getDrawable(requireContext(), R.drawable.marker_cluster)
            val nodeMarkers = mutableListOf<Marker>()

            for (node in road.mNodes) {
                val nodeMarker = Marker(map)
                nodeMarker.position = node.mLocation
                nodeMarker.icon = nodeIcon
                //nodeMarker.setTitle("road node")
                nodeMarker.title = "road node"
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
     * Send polyline to local getCloseTo function (for computeProjected())
     * After path segment is selected activate add point button
     *
     * @param polyline
     * @param mapView
     * @param eventPos
     */
    private fun onClickOnPath(polyline: Polyline, mapView: MapView, eventPos: GeoPoint): Boolean {
        println("onClick at ${eventPos.latitude}, ${eventPos.longitude}")

        val pathPoint = polyline.getCloseTo(eventPos, 6.0, mapView)
        val segmentIndex = OSMPathUtils().getCloseTo(path, eventPos, 6.0, mapView.projection, false)

        //val segmentIndex = getCloseTo(eventPos, 6.0, mapView.projection, false)
        println("Pathpoint close to event: ${pathPoint.latitude}, ${pathPoint.longitude}")
        println("Index of Path segment: $segmentIndex")

        if (segmentIndex != null) {
            selectSegment(polyline, segmentIndex)

            // when path segment selected but no marker - add point active
            map_toolbar_btn_addPoint.isEnabled = true
            map_toolbar_btn_addPoint.alpha = 1.0f

//            selectedSegment = segmentIndex
        }
        return true
    }


    private fun selectSegment(polyline: Polyline, segmentIdx: Int) {
        val pointsToMark = listOf(
            GeoPoint(polyline.points[segmentIdx - 1]),
            GeoPoint(polyline.points[segmentIdx])
        )
        val pointsIdx = listOf(segmentIdx - 1, segmentIdx)

        addIconToPoints(pointsToMark, pointsIdx)

        pathSegmentInfo(pointsToMark, segmentIdx)

        selectedSegment = segmentIdx
    }

    /**
     *
     */
    private fun unSelectSegment() {
       if (selectedSegment > -1) {
           // map.overlayManager.removeAll { it is Marker }
           removeMarkersOfType(TrackMarkerType.POINT)

           selectedSegment = -1
           selectedMarkers.clear()

           map_toolbar_btn_addPoint.isEnabled = false
           map_toolbar_btn_addPoint.alpha = 0.5f

           map_toolbar_btn_removePoint.isEnabled = false
           map_toolbar_btn_removePoint.alpha = 0.5f

           map_toolbar_btn_addItem.isEnabled = false
           map_toolbar_btn_addItem.alpha = 0.5f

           map_toolbar_btn_movePoint.isEnabled = false
           map_toolbar_btn_movePoint.alpha = 0.5f
       }
    }

    /**
     * Remove the markers of type TrackMarkerType
     *
     * @param markerType
     */
    private fun removeMarkersOfType( markerType: TrackMarkerType) {
       for (m in map.overlays) {
           if (m is Marker) {
               if (m.type == markerType) {
                   map.overlays.remove(m)
               }
           }
       }
    }


    /**
     * Set touched marker into selected state
     * Diable add point button
     *
     * First point in path
     * Last point in path: enable add to path through clicking on map
     *
     * @param marker tapped marker, must be on overlay
     * @param idx index in path
     */
    private fun clickOnMarker(marker: Marker, idx: Int): Boolean {
        println("clickOnMarker ${marker.position}, idx: $idx")
        var newMarkerSelected = false
        // first set display of marker to selected
        if (selectedMarkers.contains(marker)) {
            marker.icon =
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_brightness_1_black_12dp)
            selectedMarkers.remove(marker)
            selectedMarkersPathPosition.remove(idx)
            if (map_toolbar_btn_movePoint.isEnabled ) {
                scrollAction = false
                map_toolbar_btn_movePoint.isEnabled = false
                map_toolbar_btn_movePoint.alpha = 0.5f
                removeMapListener()
                map_toolbar_btn_removePoint.isEnabled = false
                map_toolbar_btn_removePoint.alpha = 0.5f
                map.invalidate()
            }
        } else {
            // only one marker can be selected
            if (selectedMarkers.isNotEmpty()) {
                for (selectedMarker in selectedMarkers) {
                    selectedMarker.icon = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_brightness_1_black_12dp
                    )
                    selectedMarkers.remove(selectedMarker)
                    selectedMarkersPathPosition.clear()
                }
                map_toolbar_btn_removePoint.alpha = 0.5f
                map_toolbar_btn_removePoint.isEnabled = false
            }

            // last point in path
            if ( idx + 1 == trackObject.coordsGpx.size) {
                Log.i("LOG", "Selected Marker is last in Path")
                Toast.makeText(
                    context,
                    "Last point of path",
                    Toast.LENGTH_LONG
                ).show()
                unSelectSegment()
                trackEndMarker()
                map.invalidate()
            } else {
                selectedMarkers.add(marker)
                selectedMarkersPathPosition.add(idx)
                marker.icon = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_brightness_1_black_12dp
                )?.mutate()
                marker.icon.setTint(ContextCompat.getColor(requireContext(), R.color.schema_one_red))
                newMarkerSelected = true

                Toast.makeText(
                    context,
                    "Marker id: ${trackObject.coords[selectedMarkersPathPosition.first()].id}, \n path position: ${selectedMarkersPathPosition.first()}",
                    Toast.LENGTH_LONG
                ).show()

                // add item to point
                map_toolbar_btn_addItem.alpha = 1.0f
                map_toolbar_btn_addItem.isEnabled = true

                // wayPoint item?
                val wp =  trackObject.wayPoints.firstOrNull { (it.wayPointName.length < 0) }
                if (wp != null) {

                }
            }

        }


        if (trackObject.trackSourceType == TrackSourceType.FILE) {
            onPathPointInfo(idx)
            map.invalidate()
        }

        if (trackObject.trackSourceType == TrackSourceType.DATABASE && newMarkerSelected) {
            // show toolbar for selected marker
            if (map_toolbar_btn_movePoint.isEnabled && map_toolbar_btn_movePoint.alpha == 1.0f) {
                map_toolbar_btn_addPoint.isEnabled = false
                map_toolbar_btn_addPoint.alpha = 0.5f
            } else {
                map_toolbar_btn_removePoint.alpha = 1.0f
                map_toolbar_btn_removePoint.isEnabled = true
                map_toolbar_btn_movePoint.isEnabled = true

                map_toolbar_btn_addPoint.isEnabled = false
                map_toolbar_btn_addPoint.alpha = 0.5f
            }

            map.invalidate()
        }
        return true
    }


    /**
     * Put icons on top of GeoPoint's
     * This will aways clear all overlays of type Marker?
     * That could be a problem
     *
     * @param points Start and end point of selected path segment
     * @param pointIdx
     */
    private fun addIconToPoints(points: List<GeoPoint>, pointIdx: List<Int>) {

        //map.overlayManager.removeAll { it is Marker }

        for (m in map.overlays) {
            if (m is Marker) {
                if (m.type == TrackMarkerType.POINT) {
                    map.overlayManager.remove(m)
                }
            }
        }

//        println("icon.bounds: ${i?.bounds!!.right}")
//        println("icon.intrinsicWidth: ${i.intrinsicWidth}")
//        i?.setBounds(0, 0, (i.intrinsicWidth * 0.5).toInt(), (i.intrinsicHeight * 0.5).toInt())

        for ((index, p) in points.withIndex()) {
            val marker = Marker(map)
            val i =
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_brightness_1_black_12dp)
            marker.id = "marker_selected"
            marker.position = p
            marker.setAnchor(0.5f, 0.5f)
            marker.icon = i
            marker.icon.setTint(ContextCompat.getColor(requireContext(), R.color.schema_one_dark))
            //marker.icon.updateBounds(0, 0, i?.bounds!!.right, i?.bounds.bottom)
            marker.setInfoWindow(null)
            marker.type = TrackMarkerType.POINT

            marker.setOnMarkerClickListener { marker: Marker, _: MapView ->
                println("Click on Marker with index $pointIdx")
                clickOnMarker(marker, pointIdx[index])
                true
            }

            map.overlays.add(marker)
        }

        map.invalidate()
    }

    /**
     * Display infos about selected path segment
     *
     * @param points start and end point of path segment
     * @param pathIndex
     */
    private fun pathSegmentInfo(points: List<GeoPoint>, pathIndex: Int) {
        if (points.size == 2) {

            val pointsDistance =
                GeoPoint(points.first()).distanceToAsDouble(points.last()).toFloat()
            val pointsDistanceString = "%.2f".format(pointsDistance) + " m"
            activity?.runOnUiThread {
                Toast.makeText(
                    context,
                    "Track segment $pathIndex, distance: $pointsDistanceString",
                    Toast.LENGTH_LONG
                ).show()
            }

        }
    }




    /**
     * Toggles location service on and of?
     */
    private fun onEditButton(btn: ImageButton) {
        //btn.isEnabled = !btn.isEnabled
        if (btn.alpha == 0.5f) {
            btn.alpha = 1.0f
            pathEditEnabled = true

            LocationTrackingService.startService(requireContext(), callback = { onLocationChangeFromService(it) })
        } else {
            btn.alpha = 0.5f
            pathEditEnabled = false

            LocationTrackingService.stopService(requireContext())
        }

    }

    /**
     * Call from LocationTrackingService with new location.
     * Add new location to end of path.
     *
     * @param location
     */
    private fun onLocationChangeFromService(location: Location)  {
        Log.e("LOG", "onLocationChangeFromService")
        Log.e("Log", location.toString())
        //return latitude

        trackObject.addCoord(
            GeoPoint(location.latitude, location.longitude),
            res = { updatePath(TrackActionType.AddAtEnd) })
    }


    /**
     * Remove first point in selected Marker list
     * This button can enable it self?
     * ToDo Update selected path segment and marker
     *
     * @param btn
     */
    private fun onRemoveButton(btn: ImageButton) {

        println("Remove path point")
        if (btn.isEnabled && btn.alpha == 1.0f) {
            if (selectedMarkers.isNotEmpty()) {
                val markerPosition = selectedMarkersPathPosition[0]
                trackObject.deleteCoord(
                    markerPosition,
                    res = { updatePath(TrackActionType.RemoveSelected) })
            }
            unSelectSegment()
        } else {
            if (btn.isEnabled) {
                btn.alpha = 1.0f
            }
        }
    }


    /**
     * Button movePoint in toolbar touched
     * When moving point is active no adding or deleting points
     *
     * @param btn
     */
    private fun onMovePointButton(btn: ImageButton) {

        if (map_toolbar_btn_movePoint.alpha == 1.0f) {
            map_toolbar_btn_movePoint.alpha = 0.5f
            removeMapListener()
        } else {
            map_toolbar_btn_removePoint.alpha = 0.5f
            map_toolbar_btn_removePoint.isEnabled = false

            map_toolbar_btn_movePoint.alpha = 1.0f
            addMapListener()
        }

    }

    /**
     * When a path segemnt is selected and no marker selected
     * add point to path segment
     *
     * @param btn
     */
    private fun onAddPointButton(btn: ImageButton) {
        if (selectedSegment > -1 && selectedMarkers.isEmpty()) {
            val distToNext =
                GeoPoint(trackObject.coordsGpx[selectedSegment - 1]).distanceToAsDouble(trackObject.coordsGpx[selectedSegment])
            val bearingToNext =
                GeoPoint(trackObject.coordsGpx[selectedSegment - 1]).bearingTo(trackObject.coordsGpx[selectedSegment])

            val pointInMiddle =
                GeoPoint(trackObject.coordsGpx[selectedSegment - 1]).destinationPoint(
                    distToNext / 2,
                    bearingToNext
                )

            trackObject.addCoord(
                pointInMiddle,
                selectedSegment + 1,
                res = { updatePath(TrackActionType.AddMarker) })
        }
    }


    private fun onStaticPathInfo() {
        println("Path distance in meter: ${path.distance}")
        var pathDistance = path.distance.toFloat()
        var pathDistanceString = "$pathDistance m"

        if (pathDistance >= 1000) {
            pathDistance /= 1000
            pathDistanceString = "%.2f".format(pathDistance) + " km"
        }

        var pathElevation = 0.0f

        val dialog = MapBottomSheetDialog.getInstance(
            pathDistanceString,
            pathElevation.toString()
           )  { bottomSheetDismiss() }
        dialog.show(requireFragmentManager(), MapBottomSheetDialog::class.java.simpleName)

        showWayPoints()
//        Toast.makeText(
//            context,
//            "Track length is $pathDistance meter",
//            Toast.LENGTH_LONG
//        ).show()
    }

    /**
     * BottomInfoSheet for path point
     * Dismissing of BottomInfoSheet goes to bottomSheetDismiss()
     *
     * @param pathPointIdx selected path point
     */
    private fun onPathPointInfo(pathPointIdx: Int) {
        var computedDistance = 0.0f

        for (p in 0 until pathPointIdx - 1) {
            computedDistance += GeoPoint(path.points[p]).distanceToAsDouble(path.points[p + 1])
                .toFloat()
        }

        var pathDistanceToEnd = path.distance.toFloat() - computedDistance

        var computedDistanceString = "%.2f".format(computedDistance) + " m from start"
        if (computedDistance >= 1000) {
            computedDistance /= 1000
            computedDistanceString = "%.2f".format(computedDistance) + " km from start"
        }


        var pathDistanceToEndString = "%.2f".format(pathDistanceToEnd) + "m to End"
        if (pathDistanceToEnd >= 1000) {
            pathDistanceToEnd /= 1000
            pathDistanceToEndString = "%.2f".format(pathDistanceToEnd) + " km to end"
        }

        val dialog = MapBottomSheetDialog.getInstance(
            computedDistanceString,
            pathDistanceToEndString
            ) { bottomSheetDismiss() }

        dialog.show(requireFragmentManager(), MapBottomSheetDialog::class.java.simpleName)

    }


    /**
     *
     *
     */
    private fun onAddPathWayPoint() {
        Log.i(OSM_LOG, "FragmentOsmMap onAddPathWayPoint()")
//        val dialog = WayPointNewBottomSheetDialog.getInstance()
//        dialog.show(requireFragmentManager(), WayPointNewBottomSheetDialog::class.java.simpleName)
        if (trackObject.wayPoints.indexOfFirst { it.pointId == trackObject.coords[selectedMarkersPathPosition.first()].id} == -1) {
            osmBottomSheet?.openOsmBottomSheet("NewWayPoint")
        }
    }


    fun onItemClick(item: String) {
        Log.i(OSM_LOG, "FragmentOsmMap.onItemClick $item")
    }


    /**
     * BottomInfoSheet dismiss message
     */
    private fun bottomSheetDismiss() {
        println("bottomSheetDismiss")
        if (selectedMarkers.isNotEmpty()) {
            selectedMarkers.first().icon =
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_brightness_1_black_12dp)
            selectedMarkers.clear()
            selectedMarkersPathPosition.clear()
            map.invalidate()
        }
    }

    /**
     * Toggle gps functions
     *
     */
    private fun onGpsButton() {}

    /**
     * Set state of toolbar button add depending situation
     *
     * @param state
     * @param id
     * @param marker
     */
    private fun setAddButtonState(state: Boolean?, id: String, marker: Marker): Boolean {
        // if state parameter then set
        if (state != null) {
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
        val dialogItem =
            map.overlayManager.first { it is ItemizedOverlayWithFocus<*> } as ItemizedOverlayWithFocus<*>
        dialogItem.unSetFocusedItem()

        map_toolbar_btn_addPoint.isEnabled = !map_toolbar_btn_addPoint.isEnabled

        if (map_toolbar_btn_addPoint.isEnabled) {
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
    fun getLocationForTap(geoPoint: GeoPoint) {

        val geoCoder = Geocoder(this.context)
        val addresses = geoCoder.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1)

        if (addresses.isNotEmpty()) {

            logAddress((addresses[0]))

            var addressSequence = arrayOf<CharSequence>()

            for ((index, address) in addresses.withIndex()) {

                addressSequence =
                    addressSequence.plusElement("${address.adminArea}, ${address.subLocality}")
            }

            addressSequence = addressSequence.plus("${addresses[0].getAddressLine(0)}")

            val dialogFragment =
                AdressesDialogFragment("Select Location", addressSequence, geoPoint)
            dialogFragment.show(this.fragmentManager!!, "mydia")
        }
    }


    fun logAddress(address: Address) {
        println("CountryName: ${address.countryName}")
        println("adminArea: ${address.adminArea}")
        println("featureName: ${address.featureName}")
        println("locality: ${address.locality}")
        println("subAdminArea: ${address.subAdminArea}")
        println("subLocality: ${address.subLocality}")
        println("maxAddressLineIndex: ${address.maxAddressLineIndex}")

        for (i in 0..address.maxAddressLineIndex) {
            println("address line: ${address.getAddressLine(i)}")
        }
    }

    //////////// INTERFACES ////////////////////////////

    internal var osmBottomSheet: OsmBottomSheet? = null

    fun setOnOpenDialogListener(osmBottomSheet: OsmBottomSheet) {
        this.osmBottomSheet = osmBottomSheet
    }

    /**
     * implement this interface in Activity, parent Fragment or test implentation
     */
    interface OsmBottomSheet {
        fun openOsmBottomSheet(layoutResource: String)
        fun openOsmBottomSheetWithContent(layoutResource: String, wp: TrackWayPointModel)
    }
}


    //##############################################################################################

//    /**
//     * Get polyline segment with pPoint.
//     * Detection is done in screen coordinates
//     *
//     * This function comes from From LinearRing.java. Also
//     * computeProjected(), setCloserPoint(), getBestOffset(), clipAndStore()
//     *
//     * @param pPoint
//     * @param tolerance in pixels
//     * @param pProjection
//     * @param pClosePath
//     * @return Index of path segemnt with pPoint in it
//     */
//    private fun getCloseTo(pPoint: GeoPoint, tolerance: Double, pProjection: Projection, pClosePath: Boolean) : Int? {
//
//        computeProjected(pProjection)
//        val pixel: Point = pProjection.toPixels(pPoint, null)
//        val offset = PointL()
//        getBestOffset(pProjection, offset)
//        clipAndStore(pProjection, offset, pClosePath, true, null)
//        val mapSize = TileSystem.MapSize(pProjection.zoomLevel)
//        val screenRect = pProjection.intrinsicScreenRect
//        val screenWidth = screenRect.width()
//        val screenHeight = screenRect.height()
//        var startX = pixel.x.toDouble()
//        while(startX - mapSize >= 0) {
//            startX -= mapSize
//        }
//        var startY = pixel.y.toDouble()
//        while (startY - mapSize >= 0) {
//            startY -= mapSize
//        }
//        val squaredTolerance = tolerance * tolerance
//        val point0 = PointL()
//        val point1 = PointL()
//        var first = true
//        var index = 0
//
//        for (point in pointsForMilestones) {
//            point1.set(point)
//            if (first) {
//                first = false
//            } else {
//                var x = startX
//                while (x < screenWidth) {
//                        var y = startY
//                        while (y < screenHeight) {
//                            val projectionFactor =
//                                Distance.getProjectionFactorToSegment(
//                                    x,
//                                    y,
//                                    point0.x.toDouble(),
//                                    point0.y.toDouble(),
//                                    point1.x.toDouble(),
//                                    point1.y.toDouble()
//                                )
//                            val squaredDistance =
//                                Distance.getSquaredDistanceToProjection(
//                                    x,
//                                    y,
//                                    point0.x.toDouble(),
//                                    point0.y.toDouble(),
//                                    point1.x.toDouble(),
//                                    point1.y.toDouble(),
//                                    projectionFactor
//                                )
//                            if (squaredTolerance > squaredDistance) {
////                                val pointAX: Long = projectedPoints.get(2 * (index - 1))
////                                val pointAY: Long = projectedPoints.get(2 * (index - 1) + 1)
////                                val pointBX: Long = projectedPoints.get(2 * index)
////                                val pointBY: Long = projectedPoints.get(2 * index + 1)
////                                val projectionX =
////                                    (pointAX + (pointBX - pointAX) * projectionFactor).toLong()
////                                val projectionY =
////                                    (pointAY + (pointBY - pointAY) * projectionFactor).toLong()
//
//                                return index
////                                return MapView.getTileSystem()
////                                    .getGeoFromMercator(
////                                        projectionX, projectionY, pProjection.mProjectedMapSize,
////                                        null, false, false
////                                    )
//                            }
//                            y += mapSize
//                        }
//                        x += mapSize
//
//                }
//            }
//
//            point0.set(point1)
//            index++
//        }
//        return null
//    }

//    /**
//     * ToDo Get the polyline to fill projectedPoints
//     *
//     * @param pProjection
//     */
//    fun computeProjected( pProjection: Projection) {
//        // points in path * 2
//        //projectedPoints = mutableListOf<Long>((path.points.size * 2).toLong())
//        projectedPoints = LongArray(trackObject.coordsGpx.size * 2)
//
//
////        if (projectedPoints == null || projectedPoints.size != trackObject.coordsGpx.size * 2) {
////            projectedPoints = LongArray(projectedPoints.size * 2)
////        }
//        var minX = 0L
//        var maxX = 0L
//        var minY = 0L
//        var maxY = 0L
//        var index = 0
//        val previous = PointL()
//        val current = PointL()
//        for (currentGeo in path.points) {
//            pProjection.toProjectedPixels(currentGeo.latitude, currentGeo.longitude, false, current)
//
//            if (index == 0) {
//                minX = current.x
//                maxX = current.x
//                minY = current.y
//                maxY = current.y
//            } else {
//                setCloserPoint(previous, current, pProjection.mProjectedMapSize)
//                if (minX > current.x) {
//                    minX = current.x
//                }
//                if (maxX < current.x) {
//                    maxX = current.x
//                }
//                if (minY > current.y) {
//                    minY = current.y
//                }
//                if (maxY < current.y) {
//                    maxY = current.y
//                }
//            }
//            projectedPoints[2 * index] = current.x
//            projectedPoints[2 * index + 1] = current.y
//            previous.set(current.x, current.y)
//            index++
//        }
//        projectedCenter.set((minX + maxX) / 2, (minY + maxY) / 2)
//    }
//
//    private fun setCloserPoint(pPrevious: PointL, pNext: PointL, pWorldSize: Double) {
//        while (isHorizontalRepeating && abs(pNext.x - pWorldSize - pPrevious.x) < abs(
//                pNext.x - pPrevious.x
//            )
//        ) {
//            pNext.x -= pWorldSize.toLong()
//        }
//        while (isHorizontalRepeating && abs(pNext.x + pWorldSize - pPrevious.x) < abs(
//                pNext.x - pPrevious.x
//            )
//        ) {
//            pNext.x += pWorldSize.toLong()
//        }
//        while (isVerticalRepeating && abs(pNext.y - pWorldSize - pPrevious.y) < abs(
//                pNext.y - pPrevious.y
//            )
//        ) {
//            pNext.y -= pWorldSize.toLong()
//        }
//        while (isVerticalRepeating && abs(pNext.y + pWorldSize - pPrevious.y) < abs(
//                pNext.y - pPrevious.y
//            )
//        ) {
//            pNext.y += pWorldSize.toLong()
//        }
//    }
//
//
//    /**
//     * Compute the pixel offset so that a list of pixel segments display in the best possible way:
//     * the center of all pixels is as close to the screen center as possible
//     * This notion of pixel offset only has a meaning on very low zoom level,
//     * when a GeoPoint can be projected on different places on the screen.
//     */
//    private fun getBestOffset(pProjection: Projection, pOffset: PointL) {
//        val powerDifference = pProjection.projectedPowerDifference
//        val center = pProjection.getLongPixelsFromProjected(
//            projectedCenter, powerDifference, false, null
//        )
//        val screenRect = pProjection.intrinsicScreenRect
//        val screenCenterX = (screenRect.left + screenRect.right) / 2.0
//        val screenCenterY = (screenRect.top + screenRect.bottom) / 2.0
//        val worldSize = TileSystem.MapSize(pProjection.zoomLevel)
//        getBestOffset(
//            center.x.toDouble(),
//            center.y.toDouble(),
//            screenCenterX,
//            screenCenterY,
//            worldSize,
//            pOffset
//        )
//    }
//
//
//    private fun getBestOffset(
//        pPolyCenterX: Double,
//        pPolyCenterY: Double,
//        pScreenCenterX: Double,
//        pScreenCenterY: Double,
//        pWorldSize: Double,
//        pOffset: PointL) {
//
//        val worldSize = Math.round(pWorldSize)
//        var deltaPositive = 0
//        var deltaNegative = 0
//        if (!isVerticalRepeating) {
//            deltaPositive = 0
//            deltaNegative = 0
//        } else {
//            deltaPositive = getBestOffset(
//                pPolyCenterX, pPolyCenterY, pScreenCenterX, pScreenCenterY, 0, worldSize
//            )
//            deltaNegative = getBestOffset(
//                pPolyCenterX, pPolyCenterY, pScreenCenterX, pScreenCenterY, 0, -worldSize
//            )
//
//        }
//
//        pOffset.y =
//            worldSize * if (deltaPositive > deltaNegative) deltaPositive else -deltaNegative
//        if (!isHorizontalRepeating) {
//            deltaPositive = 0
//            deltaNegative = 0
//        } else {
//            deltaPositive = getBestOffset(
//                pPolyCenterX, pPolyCenterY, pScreenCenterX, pScreenCenterY, worldSize, 0
//            )
//            deltaNegative = getBestOffset(
//                pPolyCenterX, pPolyCenterY, pScreenCenterX, pScreenCenterY, -worldSize, 0
//            )
//        }
//        pOffset.x =
//            worldSize * if (deltaPositive > deltaNegative) deltaPositive else -deltaNegative
//    }
//
//
//    private fun getBestOffset(
//        pPolyCenterX: Double, pPolyCenterY: Double,
//        pScreenCenterX: Double, pScreenCenterY: Double,
//        pDeltaX: Long, pDeltaY: Long
//    ): Int {
//        var squaredDistance = 0.0
//        var i = 0
//        while (true) {
//            val tmpSquaredDistance = Distance.getSquaredDistanceToPoint(
//                pPolyCenterX + i * pDeltaX, pPolyCenterY + i * pDeltaY,
//                pScreenCenterX, pScreenCenterY
//            )
//            if (i == 0 || squaredDistance > tmpSquaredDistance) {
//                squaredDistance = tmpSquaredDistance
//                i++
//            } else {
//                break
//            }
//        }
//        return i - 1
//    }
//
//
//    private fun clipAndStore(
//        pProjection: Projection,
//        pOffset: PointL,
//        pClosePath: Boolean,
//        pStorePoints: Boolean,
//        pSegmentClipper: SegmentClipper?
//    ) {
//        pointsForMilestones.clear()
//        val powerDifference = pProjection.projectedPowerDifference
//        val projected = PointL()
//        val point = PointL()
//        val first = PointL()
//        var i = 0
//        while (i < projectedPoints.size) {
//            projected[projectedPoints.get(i)] = projectedPoints[i + 1]
//            pProjection.getLongPixelsFromProjected(projected, powerDifference, false, point)
//            val x = point.x + pOffset.x
//            val y = point.y + pOffset.y
//            if (pStorePoints) {
//                pointsForMilestones.add(x, y)
//            }
//            pSegmentClipper?.add(x, y)
//            if (i == 0) {
//                first[x] = y
//            }
//            i += 2
//        }
//        if (pClosePath) {
//            pSegmentClipper?.add(first.x, first.y)
//            if (pStorePoints) {
//                pointsForMilestones.add(first.x, first.y)
//            }
//        }
//    }


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

class FieldProperty<R, T : Any>(
    val initializer: (R) -> T = { throw IllegalStateException("Not initialized.") }
    ) {

    private val map = mutableMapOf<R,T>()

    operator fun getValue(thisRef: R, property: KProperty<*>): T =
        map[thisRef] ?: setValue(thisRef, property, initializer(thisRef))

    operator fun setValue(thisRef: R, property: KProperty<*>, value: T): T {
        map[thisRef] = value
        return value
    }
}

