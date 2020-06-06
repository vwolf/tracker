package com.e.tracker.osm


import android.annotation.SuppressLint
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
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.reflect.KProperty



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

    private var path: Polyline = Polyline(this.map)
    private val pathM: MutableLiveData<Polyline> = MutableLiveData(this.path)

    private var pathEditEnabled = false
    private lateinit var mapEventsOverlay: MapEventsOverlay

    // Marker extension with FieldProperty to act as binding field
    // Should be implemented as hashtable
    // https://github.com/h0tk3y/kotlin-fun/blob/master/src/main/java/com/github/h0tk3y/kotlinFun/util/WeakIdentityHashMap.java
    var Marker.type: TrackMarkerType by FieldProperty { TrackMarkerType.UNDEFINED }

    // keep markers
//    var markerList = mutableMapOf<String, Marker>()
//    var markerSet = mutableSetOf<Marker>()

    //private var pathPointRemoveEnabled = false


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

    // items for marker
//    private var itemsOn = false

    // location overlay
    private var currentLocationOverlay: MyLocationNewOverlay? = null

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

        binding.map.controller.setZoom(16.0)
        //binding.map.controller.setCenter(GeoPoint(52.4908, 13.4186))
        binding.map.controller.setCenter(GeoPoint(this.trackObject.latitude, this.trackObject.longitude))

        this.pathM.observe(viewLifecycleOwner, Observer { this.path = this.pathM.value!! })

        this.attachEventsOverlay()

        binding.map.overlays.add(this.mapEventsOverlay)

        this.setHasOptionsMenu(true)

        if (this.trackObject.trackSourceType == TrackSourceType.FILE) {
            binding.mapToolbar.visibility = View.INVISIBLE
            binding.mapStaticToolbar.visibility = View.VISIBLE

            binding.mapStaticBtnInfo.setOnClickListener {
                this.onStaticPathInfo()
            }

            binding.mapStaticBtnGps.alpha = 0.5f
            binding.mapStaticBtnGps.setOnClickListener {
                this.onGpsButtonStatic(binding.mapStaticBtnGps)
            }

        } else {
            binding.mapToolbar.visibility = View.VISIBLE
            binding.mapStaticToolbar.visibility = View.INVISIBLE

            // button gps on/off
            binding.mapToolbarBtnGps.alpha = 0.5f
            binding.mapToolbarBtnGps.setOnClickListener {
                this.onGpsButton(binding.mapToolbarBtnGps)
            }

            // button add point to path
            binding.mapToolbarBtnAddPoint.alpha = 0.5f
            binding.mapToolbarBtnAddPoint.isEnabled = false
            binding.mapToolbarBtnAddPoint.setOnClickListener {
                this.onAddPointButton()
            }

            // button remove point from path
            binding.mapToolbarBtnRemovePoint.alpha = 0.5f
            binding.mapToolbarBtnRemovePoint.isEnabled = false
            binding.mapToolbarBtnRemovePoint.setOnClickListener {
                this.onRemoveButton(binding.mapToolbarBtnRemovePoint)
            }

            // button move point in path on/off
            binding.mapToolbarBtnMovePoint.alpha = 0.5f
            binding.mapToolbarBtnMovePoint.isEnabled = false
            binding.mapToolbarBtnMovePoint.setOnClickListener {
                this.onMovePointButton(binding.mapToolbarBtnMovePoint)
            }
            binding.mapToolbarBtnMovePoint.visibility = View.GONE

            // button show path/track info
            binding.mapToolbarBtnInfo.setOnClickListener {
                this.onStaticPathInfo()
            }

            // button show icon for all marker items
            // binding.mapToolbarBtnInfo.alpha = 0.5f

            // button add a waypoint item to path point
//            binding.mapToolbarBtnAddItem.alpha = 0.5f
//            binding.mapToolbarBtnAddItem.isEnabled = false
            binding.mapToolbarBtnAddItem.setOnClickListener {
                this.onAddPathWayPoint()
            }
        }

//        this.map.addOnFirstLayoutListener() {
//            this.map.addOnLayoutChangeListener { ol }
//        }

        //fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        Log.i("LOG", "map tileSource: ${binding.map.tileProvider.tileSource}")
        return binding.root
    }


    
    override fun onAttach(context: Context) {
        super.onAttach(context)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //addPolyline()
        if (this.trackObject.trackSourceType == TrackSourceType.FILE) {
            this.updateMap()
            //this.zoomToTrackBounds()
        }
    }

    override fun onStart() {
        super.onStart()
        Log.i(OSM_LOG, "FragmentOsmMap.onStart()")
//        if (this.trackObject.trackSourceType == TrackSourceType.FILE) {
//            //this.zoomToTrackBounds()
//        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log.i(OSM_LOG, "FragmentOsmMap.onActivityCreated()")
    }

    override fun onPause() {
        super.onPause()
        this.map.onPause()
    }

    override fun onResume() {
        super.onResume()
        this.map.onResume()
    }


    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        MenuBuilder(this.context).setOptionalIconsVisible(true)
        super.onCreateOptionsMenu(menu, inflater)
    }

    /**
     * Attach a MapEventsOverlay to map
     * Get events from map
     *
     *
     */
    private fun attachEventsOverlay() {
        this.mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {

            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                Log.i(OSM_LOG, "MapView Tap ${p.latitude}  ${p.longitude}")
                when (this@FragmentOsmMap.trackObject.trackSourceType) {
                    TrackSourceType.DATABASE -> {
                        when (this@FragmentOsmMap.activeMarker?.type) {
                            TrackMarkerType.START -> {
                                Log.i(OSM_LOG, "singleTap on mapEventsOverlay, ${this@FragmentOsmMap.activeMarker?.type}")
                                this@FragmentOsmMap.trackObject.addCoord(
                                    geoPoint = p,
                                    position = 1,
                                    res = { this@FragmentOsmMap.updatePath(TrackActionType.AddAtStart) })
                                this@FragmentOsmMap.activeMarker?.position = p
                                this@FragmentOsmMap.map.invalidate()
                            }

                            TrackMarkerType.END -> {
                                Log.i(OSM_LOG, "singleTap on mapEventsOverlay, ${this@FragmentOsmMap.activeMarker?.type}")
                                this@FragmentOsmMap.trackObject.addCoord(
                                    geoPoint = p,
                                    res = { this@FragmentOsmMap.updatePath(TrackActionType.AddAtEnd) })
                                this@FragmentOsmMap.path.addPoint(p)
                                this@FragmentOsmMap.activeMarker?.position = p
                                this@FragmentOsmMap.map.invalidate()
                            }

                            TrackMarkerType.POINT -> {
                                Log.i(OSM_LOG, "singleTap on mapEventsOverlay, ${this@FragmentOsmMap.activeMarker?.type}")
                            }

                            TrackMarkerType.UNDEFINED -> {
                                Log.i(OSM_LOG, "singleTap on mapEventsOverlay, ${this@FragmentOsmMap.activeMarker?.type}")
                            }

                            TrackMarkerType.POINTSELECTED -> {
                                Log.i(OSM_LOG, "singleTap on mapEventsOverlay, ${this@FragmentOsmMap.activeMarker?.type}")
                                // srollAction? (move point)
//                                if (scrollAction) {
//
//                                }
                            }

                            else -> {
                                // no activeMarker, segment selected then deselect
                                if (this@FragmentOsmMap.selectedSegment > -1) {
                                    this@FragmentOsmMap.unSelectSegment()
                                    this@FragmentOsmMap.map.invalidate()
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
            if ( this.trackObject.wayPoints.isNotEmpty()) {
                this.showWayPoints()
            }
        } else {
            this.hideWayPoints()
        }
    }

    /**
     * Put an icon on all waypoints
     *
     */
    private fun showWayPoints() {
        Log.i(OSM_LOG, "showWayPoints")
        //Log.i(OSM_LOG, "")

        if (trackObject.wayPoints.isEmpty()) {
            return
        }

        if ( this.map_toolbar_btn_info.isActivated ) {
            hideWayPoints()
        } else {
            val mIcon =
                ContextCompat.getDrawable(
                    this.requireContext(),
                    R.drawable.ic_panorama_fish_eye_black_16dp
                )
                    ?.mutate()
            mIcon?.setTint(
                ContextCompat.getColor(
                    this.requireContext(),
                    R.color.schema_one_blue_dark
                )
            )

            for (wp in this.trackObject.wayPoints) {

                val wpMarker = Marker(this.map)
                wpMarker.position = this.trackObject.coordsGpx[wp.pointId.toInt() - 1]
                wpMarker.setAnchor(0.5f, 0.5f)
                wpMarker.title = wp.wayPointName
                wpMarker.isDraggable = false
                wpMarker.id = "waypoint"
                wpMarker.icon = mIcon

                wpMarker.setOnMarkerClickListener { marker, mapView ->
                    Log.i(OSM_LOG, "onClickListener for waypoint id: ${wp.pointId}")

                    this.osmBottomSheet?.openOsmBottomSheetWithContent("WayPoint", wp)

                    //marker.showInfoWindow()
                    true
                }

                wpMarker.type = TrackMarkerType.WAYPOINT
                this.map.overlays.add(wpMarker)
            }
            this.map_toolbar_btn_info.isActivated = true
            this.map.invalidate()
        }
    }


    fun hideWayPoints() {
        Log.i(OSM_LOG, "hideWayPoints")
        this.removeMarkersOfType( TrackMarkerType.WAYPOINT )
        this.map_toolbar_btn_info.isActivated = false
        this.map.invalidate()
    }



    /**
     * This comes from activity, is touchUp event on map
     * Use it to update after scroll action
     * Update trackObject.coords, save to db ...
     *
     */
    fun receiveActionUP() {

        Log.i("LOG", "ACTIONUP map center: ${this.map.mapCenter}, center offset x: ${this.map.mapCenterOffsetX}, zoom: ${this.map.zoomLevelDouble}")

        // important for OnScroll(), makes sure to get right scroll event distance
        this.map.controller.setCenter(this.map.mapCenter)

        if (this.scrollAction) {
            Log.i(OSM_LOG, "receiveActionUp during scrollAction, selected marker path position: ${this.selectedMarkersPathPosition.first()}")
            this.scrollAction = false
            val pathIndex = selectedMarkersPathPosition.first()
            this.trackObject.coords[pathIndex].latitude = this.trackObject.coordsGpx[pathIndex].latitude
            this.trackObject.coords[pathIndex].longitude = this.trackObject.coordsGpx[pathIndex].longitude

            // save changed coord
            trackObject.updateCoordPosition(this.trackObject.coords[pathIndex])
            // last marker moved? then update icon position
            Log.i(OSM_LOG, "pathIndex: $pathIndex, path.points.size: ${this.path.points.size}")
            if (pathIndex == this.path.points.size - 1) {
                val endMarker = this.getMarkerOfType(TrackMarkerType.END)
                if (endMarker != null) {
                    endMarker.position = this.trackObject.coordsGpx[pathIndex]
                }
            }
            // first marker moved? then update icon start position
            if (pathIndex == 0) {
                val startMarker = this.getMarkerOfType(TrackMarkerType.START)
                if (startMarker != null) {
                    startMarker.position = this.trackObject.coordsGpx[pathIndex]
                }
            }


        } else {
            // deselect selected segment
            if(this.selectedSegment > -1 && this.selectedMarkers.isNotEmpty()) {
                //unSelectSegment()
            }

            if (this.selectedSegment > -1) {
                //unSelectSegment()
            }

        }
        this.map.invalidate()

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

            if (!this@FragmentOsmMap.scrollAction) {
                Log.i("LOG", "SelectedMarkers.size: ${this@FragmentOsmMap.selectedMarkers.size}")
                this@FragmentOsmMap.selectedMarkerStartPos = this@FragmentOsmMap.selectedMarkers.first().position
                this@FragmentOsmMap.selectedMarkerStartPosPixel = this@FragmentOsmMap.map.projection.toPixels(
                    this@FragmentOsmMap.selectedMarkerStartPos, null)
                Log.i("LOG", "Marker start position scrollAction: ${this@FragmentOsmMap.selectedMarkerStartPos}, posPixel: ${this@FragmentOsmMap.selectedMarkerStartPosPixel}")
                this@FragmentOsmMap.scrollAction = true
            }

            if (event != null && this@FragmentOsmMap.scrollAction) {
                //Log.i("LOG", "Scrollaction event consum with startPosPixel: $selectedMarkerStartPosPixel")
                val smNewPositionX = this@FragmentOsmMap.selectedMarkerStartPosPixel.x + event.x
                val smNewPositionY = this@FragmentOsmMap.selectedMarkerStartPosPixel.y + event.y
                // Log.i("Log", "new position: x: $smNewPositionX, y: $smNewPositionY")
                val smNewPosition = this@FragmentOsmMap.map.projection.fromPixels(smNewPositionX, smNewPositionY)
                this@FragmentOsmMap.selectedMarkers.first().position =
                    GeoPoint(smNewPosition.latitude, smNewPosition.longitude)

                // update path point
//                path.points[selectedMarkersPathPosition.first()].latitude = smNewPosition.latitude
//                path.points[selectedMarkersPathPosition.first()].longitude = smNewPosition.longitude
                this@FragmentOsmMap.trackObject.coordsGpx[this@FragmentOsmMap.selectedMarkersPathPosition.first()].latitude =
                    smNewPosition.latitude
                this@FragmentOsmMap.trackObject.coordsGpx[this@FragmentOsmMap.selectedMarkersPathPosition.first()].longitude =
                    smNewPosition.longitude

                this@FragmentOsmMap.updatePath(TrackActionType.MoveMarker)
            }

            return true
        }

        /**
         * Remove MapListener when zooming, otherwise the selected marker moves with zoom action
         *
         */
        @Override
        override fun onZoom(event: ZoomEvent): Boolean {
            println("Zoom ${event}")
            this@FragmentOsmMap.removeMapListener()

            this@FragmentOsmMap.map_toolbar_btn_movePoint.isEnabled = false
            this@FragmentOsmMap.map_toolbar_btn_movePoint.alpha = 0.5f

            this@FragmentOsmMap.clickOnMarker(this@FragmentOsmMap.selectedMarkers.first(), 0)
            return true
        }


    }

    /**
     *
     */
    private fun addMapListener() {
        //val moveListener   = MapListener{}
        if (this.selectedMarkers.isNotEmpty()) {
            this.selectedMarkers.first().isDraggable = false
            this.map.addMapListener(this.maplistener)
        }
    }


    private fun removeMapListener() {
        this.map.removeMapListener(this.maplistener)
    }


    fun updateMap() {
        Log.i(OSM_LOG, "zoom: ${this.map.zoomLevelDouble}")

        this.map.controller.setCenter(GeoPoint(this.trackObject.latitude, this.trackObject.longitude))

        (this.activity as? AppCompatActivity)?.supportActionBar?.title = this.trackObject.trackName

        this.addPathAsPolyline()
        this.trackStartMarker()
        this.trackEndMarker()
        //addIconToPoints(trackObject.coordsGpx)
    }


    fun invokeUpdateMap(type: String) {
        when (type) {
            "waypoints" -> {
                this.hideWayPoints()
                this.showWayPoints()
            }
        }
        this.map.invalidate()
    }

    fun zoomToTrackBounds() {
        Log.i(OSM_LOG, "zoomToTrack.map: ${map}")
        if (map == null) {
            return
        }
        if (this.trackObject.coords.size > 1 ) {
            val trackBounds = OSMPathUtils().createBounds(this.trackObject.coords)
//            map.zoomToBoundingBox(trackBounds, true)
            map.zoomToBoundingBox(trackBounds, true, 36)
        }
    }


    private fun trackStartMarker() {

        val mIcon =
            ContextCompat.getDrawable(this.requireContext(), R.drawable.ic_add_location_black_24dp)
                ?.mutate()
        mIcon?.setTint(ContextCompat.getColor(this.requireContext(), R.color.schema_one_green))

        val startMarker = Marker(this.map)
        startMarker.position = this.trackObject.coordsGpx.first()
        startMarker.title = "Track Start"
        startMarker.isDraggable = false
        startMarker.id = "track_start"
        startMarker.icon = mIcon
        startMarker.setOnMarkerClickListener { marker, mapView ->
            var toastText = marker.title.toString()
            if (this.activeMarker == marker) {
                // marker is selected, reset to not selected
                this.setAddButtonState(false, marker.id, marker)
                this.activeMarker = null
                mIcon?.setTint(ContextCompat.getColor(this.requireContext(), R.color.schema_one_green))
                this.map.invalidate()
                toastText += " is not active."
            } else {
                // before selecting this marker reset any selected marker
                if (this.activeMarker?.type == TrackMarkerType.END) {
                    this.activeMarker?.icon?.setTint(ContextCompat.getColor(this.requireContext(), R.color.schema_one_red))
                }
                this.activeMarker = marker
                this.setAddButtonState(true, marker.id, marker)
                mIcon?.setTint(ContextCompat.getColor(this.requireContext(), R.color.schema_one_blue))

                unSelectSegment()
                this.map.invalidate()
                toastText += " is active. Touch on map to extend track."
            }

            Toast.makeText(
                this.context,
                toastText,
                Toast.LENGTH_LONG
            ).show()

            //marker.showInfoWindow()
            true
        }

        startMarker.type = TrackMarkerType.START
        this.map.overlays.add(startMarker)
    }

    /**
     * Marker for end of path.
     * When this marker is selected, each tap on map extends path to tap position
     */
    private fun trackEndMarker() {

        val mIcon =
            ContextCompat.getDrawable(this.requireContext(), R.drawable.ic_add_location_black_24dp)
                ?.mutate()
        mIcon?.setTint(ContextCompat.getColor(this.requireContext(), R.color.schema_one_red))

        val endMarker = Marker(this.map)
        endMarker.position = this.trackObject.coordsGpx.last()
        endMarker.title = "Track End"
        endMarker.isDraggable = false
        endMarker.id = "track_end"
        endMarker.icon = mIcon
        endMarker.setOnMarkerClickListener { marker, mapView ->
            var toastText = marker.title.toString()
            if (this.activeMarker == marker) {
                this.setAddButtonState(false, marker.id, marker)
                this.activeMarker = null
                mIcon?.setTint(ContextCompat.getColor(this.requireContext(), R.color.schema_one_red))
                this.map.invalidate()
                toastText += " is not active."
            } else {
                if (this.activeMarker?.type == TrackMarkerType.START) {
                    this.activeMarker?.icon?.setTint(ContextCompat.getColor(this.requireContext(), R.color.schema_one_green))
                }
                this.activeMarker = marker
                this.setAddButtonState(true, marker.id, marker)
                mIcon?.setTint(ContextCompat.getColor(this.requireContext(), R.color.schema_one_blue))
                unSelectSegment()
                this.map.invalidate()
                //marker.showInfoWindow()
                toastText += " is active. Touch on map to extend track."
            }

            Toast.makeText(
                this.context,
                toastText,
                Toast.LENGTH_LONG
            ).show()

            true
        }
        endMarker.type = TrackMarkerType.END
        this.map.overlays.add(endMarker)

    }

    /**
     * Move track end marker to new track end position
     *
     */
    private fun updateTrackEndMarker() {
        this.map.overlays.forEach {
            if (it is Marker) {
                if (it.id == "track_end") {
                    it.position = this.trackObject.coordsGpx.last()
                }
            }
        }
    }


    /**
     * Reset active marker to default state
     */
    private fun resetActiveMarker() {

        if (activeMarker != null) {
            when (activeMarker!!.type) {
                TrackMarkerType.START -> {
                    this.activeMarker?.icon?.setTint(ContextCompat.getColor(this.requireContext(), R.color.schema_one_green))
                }
                TrackMarkerType.END -> {
                    this.activeMarker?.icon?.setTint(ContextCompat.getColor(this.requireContext(), R.color.schema_one_red))
                }
            }
        }

    }


    /**
     * Update path after action
     * Use property path, only works with one path on map
     *
     * @param status
     */
    fun updatePath(status: TrackActionType) {
        println("updatePath: $status")

//        activity?.runOnUiThread {
//            Toast.makeText(this.context, "updatePath: $status", Toast.LENGTH_SHORT).show()
//        }


        when (status) {
            TrackActionType.AddAtStart -> { }
            TrackActionType.AddAtEnd -> {
                this.path.setPoints(this.trackObject.coordsGpx)
                activity?.runOnUiThread() {
                    //this.updateMap()
                    this.updateTrackEndMarker()
                }
            }
            TrackActionType.RemoveSelected -> {
                // update selected marker

                val sm = this.selectedMarkersPathPosition.first()
                if (sm < this.path.points.size) {
                    this.selectedSegment = +1
                    this.selectSegment(this.path, this.selectedSegment)
                } else {
                    // last point in path?
                }
                // set state of delete button in statusbar
            }

            TrackActionType.AddMarker -> {
                this.path.setPoints(this.trackObject.coordsGpx)
                // update marker, set second marker to new marker
                // value selectedSegment is not changed, new segment added
                this.selectSegment(this.path, this.selectedSegment)
            }
            TrackActionType.MoveMarker -> {}
        }

        this.path.setPoints(this.trackObject.coordsGpx)
        activity?.runOnUiThread {
            this.map.invalidate()
        }
    }

    /**
     * First version for adding a path to map
     * Add onClickListener to path (onClickOnPath)
     *
     */
    private fun addPathAsPolyline() {
        // convert coords into GeoPoint's
        val geoPoints = arrayListOf<GeoPoint>()
        for (coord in this.trackObject.coords) {
            geoPoints.add(GeoPoint(coord.latitude, coord.longitude))
        }

        // init and style path
        //var path = Polyline(map)
        this.path.outlinePaint.color = ContextCompat.getColor(this.requireContext(), R.color.schema_one_dark)

        this.path.setPoints(geoPoints)

        this.path.setOnClickListener { polyline, mapView, eventPos ->
            this.onClickOnPath(polyline, mapView, eventPos)
        }

        // save geoPoints of path in trackObject
        this.trackObject.coordsGpx = geoPoints

        this.map.overlays.add(this.path)
        this.map.invalidate()


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
        for (coord in this.trackObject.coords) {
            geoPoints.add(GeoPoint(coord.latitude, coord.longitude))
        }

        val roadManager = OSRMRoadManager(this.requireContext())

        GlobalScope.launch(IO) {
            val road = roadManager.getRoad(geoPoints)
            val roadOverlay = RoadManager.buildRoadOverlay(road)

            val nodeIcon = ContextCompat.getDrawable(this@FragmentOsmMap.requireContext(), R.drawable.marker_cluster)
            val nodeMarkers = mutableListOf<Marker>()

            for (node in road.mNodes) {
                val nodeMarker = Marker(this@FragmentOsmMap.map)
                nodeMarker.position = node.mLocation
                nodeMarker.icon = nodeIcon
                //nodeMarker.setTitle("road node")
                nodeMarker.title = "road node"
                //map.overlays.add(nodeMarker)
                nodeMarkers.add(nodeMarker)
            }

            this.launch(Main) {
                this@FragmentOsmMap.map.overlays.add(roadOverlay)
                this@FragmentOsmMap.map.overlays.addAll(nodeMarkers)
                this@FragmentOsmMap.map.invalidate()

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
        val segmentIndex = OSMPathUtils().getCloseTo(this.path, eventPos, 6.0, mapView.projection, false)

        //val segmentIndex = getCloseTo(eventPos, 6.0, mapView.projection, false)
        println("Pathpoint close to event: ${pathPoint.latitude}, ${pathPoint.longitude}")
        println("Index of Path segment: $segmentIndex")

        if (segmentIndex != null) {
            this.selectSegment(polyline, segmentIndex)

            // when path segment selected but no marker - add point active
            this.map_toolbar_btn_addPoint.isEnabled = true
            this.map_toolbar_btn_addPoint.alpha = 1.0f

            // edit marker
            this.map_toolbar_btn_addItem.isEnabled = false
            this.map_toolbar_btn_addItem.alpha = 0.5f

            if (this.map_toolbar_btn_movePoint.isEnabled) {
                this.disableTrackPointMove()
            }

            // disable start and end marker if enabled
            if ( this.activeMarker != null ) {
                //this.activeMarker.id == "TrackEnd"
                this.resetActiveMarker()
                this.activeMarker = null
            }
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
        this.addIconToPoints(pointsToMark, pointsIdx)
        this.pathSegmentInfo(pointsToMark, segmentIdx)

        // remove possible mapListener ( move map active)
        if (this.map_toolbar_btn_movePoint.isEnabled) {
            this.removeMapListener()
        }

        this.selectedSegment = segmentIdx
        this.map.invalidate()
    }

    /**
     * Get called when segment selected and touch on map
     *
     */
    private fun unSelectSegment() {
       if (this.selectedSegment > -1) {
           // map.overlayManager.removeAll { it is Marker }
           this.removeMarkersOfType(TrackMarkerType.POINT)

           this.selectedSegment = -1
           this.selectedMarkers.clear()
           this.selectedMarkersPathPosition.clear()

           this.map_toolbar_btn_addPoint.isEnabled = false
           this.map_toolbar_btn_addPoint.alpha = 0.5f

           this.map_toolbar_btn_removePoint.isEnabled = false
           this.map_toolbar_btn_removePoint.alpha = 0.5f

           this.map_toolbar_btn_addItem.isEnabled = false
           this.map_toolbar_btn_addItem.alpha = 0.5f

           this.map_toolbar_btn_movePoint.isEnabled = false
           this.map_toolbar_btn_movePoint.alpha = 0.5f

           this.removeMapListener()
       }
    }

    /**
     * Remove the markers of type TrackMarkerType
     *
     * @param markerType
     */
    private fun removeMarkersOfType( markerType: TrackMarkerType) {
       for (m in this.map.overlays) {
           if (m is Marker) {
               if (m.type == markerType) {
                   this.map.overlays.remove(m)
               }
           }
       }
    }

    /**
     * Return first marker of type [TrackMarkerType]
     *
     * @param markerType
     * @return [Marker] or null
     */
    private fun getMarkerOfType( markerType: TrackMarkerType) : Marker? {
        for (m in this.map.overlays) {
            if (m is Marker) {
                if (m.type == markerType) {
                    return m
                }
            }
        }
        return null
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
        Log.i(OSM_LOG,"clickOnMarker ${marker.position}, idx: $idx")
        var newMarkerSelected = false
        // first set display of marker to selected
        if (this.selectedMarkers.contains(marker)) {
            Log.i(OSM_LOG, "clickOnMarker deselect marker")
            marker.icon =
                ContextCompat.getDrawable(this.requireContext(), R.drawable.ic_brightness_1_black_12dp)
            this.selectedMarkers.remove(marker)
            this.selectedMarkersPathPosition.remove(idx)
            if (this.map_toolbar_btn_movePoint.isEnabled ) {
                this.disableTrackPointMove()
//                scrollAction = false
//                map_toolbar_btn_movePoint.isEnabled = false
//                map_toolbar_btn_movePoint.alpha = 0.5f
//                removeMapListener()
//                map_toolbar_btn_removePoint.isEnabled = false
//                map_toolbar_btn_removePoint.alpha = 0.5f
//                map.invalidate()
            }

            // no adding of item
            map_toolbar_btn_addItem.isEnabled = false
            map_toolbar_btn_addItem.alpha = 0.5f
        } else {
            // only one marker can be selected
            if (this.selectedMarkers.isNotEmpty()) {
                for (selectedMarker in this.selectedMarkers) {
                    selectedMarker.icon = ContextCompat.getDrawable(
                        this.requireContext(),
                        R.drawable.ic_brightness_1_black_12dp
                    )
                    this.selectedMarkers.remove(selectedMarker)
                    this.selectedMarkersPathPosition.clear()
                }
                this.map_toolbar_btn_removePoint.alpha = 0.5f
                this.map_toolbar_btn_removePoint.isEnabled = false

                if (this.map_toolbar_btn_movePoint.isEnabled) {
                    this.disableTrackPointMove()
                }
            }

            // last point in path
            if ( idx + 1 == this.trackObject.coordsGpx.size) {
                Log.i("LOG", "Selected Marker is last in Path")
                Toast.makeText(
                    this.context,
                    "Last point of path",
                    Toast.LENGTH_LONG
                ).show()
                // make last marker moveable
                this.selectedMarkers.add(marker)
                this.selectedMarkersPathPosition.add(idx)
                marker.icon = ContextCompat.getDrawable(
                    this.requireContext(),
                    R.drawable.ic_brightness_1_black_12dp
                )?.mutate()
                marker.icon.setTint(ContextCompat.getColor(this.requireContext(), R.color.schema_one_red))
                newMarkerSelected = true

//                unSelectSegment()
//                trackEndMarker()
//                map.invalidate()
            } else {
                this.selectedMarkers.add(marker)
                this.selectedMarkersPathPosition.add(idx)
                marker.icon = ContextCompat.getDrawable(
                    this.requireContext(),
                    R.drawable.ic_brightness_1_black_12dp
                )?.mutate()
                marker.icon.setTint(ContextCompat.getColor(this.requireContext(), R.color.schema_one_red))
                newMarkerSelected = true

                Toast.makeText(
                    this.context,
                    "Marker id: ${this.trackObject.coords[this.selectedMarkersPathPosition.first()].id}, \n path position: ${this.selectedMarkersPathPosition.first()}",
                    Toast.LENGTH_LONG
                ).show()

                // add item to point
                this.map_toolbar_btn_addItem.alpha = 1.0f
                this.map_toolbar_btn_addItem.isEnabled = true

                // wayPoint item?
                //val wp =  trackObject.wayPoints.firstOrNull { (it.wayPointName.length < 0) }
                //if (wp != null) { }
            }

        }


        if (this.trackObject.trackSourceType == TrackSourceType.FILE) {
            this.onPathPointInfo(idx)
            this.map.invalidate()
        }

        if (this.trackObject.trackSourceType == TrackSourceType.DATABASE && newMarkerSelected) {
            this.map_toolbar_btn_movePoint.visibility = View.VISIBLE
            // show toolbar for selected marker
            if (this.map_toolbar_btn_movePoint.isEnabled && this.map_toolbar_btn_movePoint.alpha == 1.0f) {
                this.map_toolbar_btn_addPoint.isEnabled = false
                this.map_toolbar_btn_addPoint.alpha = 0.5f
            } else {
                this.map_toolbar_btn_removePoint.alpha = 1.0f
                this.map_toolbar_btn_removePoint.isEnabled = true
                this.map_toolbar_btn_movePoint.isEnabled = true

                this.map_toolbar_btn_addPoint.isEnabled = false
                this.map_toolbar_btn_addPoint.alpha = 0.5f
            }

            this.map.invalidate()
        }
        return true
    }

    /**
     * Disable track point move
     */
    private fun disableTrackPointMove() {
        this.scrollAction = false
        this.map_toolbar_btn_movePoint.isEnabled = false
        this.map_toolbar_btn_movePoint.alpha = 0.5f
        this.removeMapListener()
        this.map_toolbar_btn_removePoint.isEnabled = false
        this.map_toolbar_btn_removePoint.alpha = 0.5f
        this.map.invalidate()
    }


    /**
     * Put icons on top of GeoPoint's
     * This will aways clear all overlays of [TrackMarkerType.POINT]
     * That could be a problem
     *
     * @param points Start and end point of selected path segment
     * @param pointIdx Index in path.points of start and end point of selected path segment
     */
    private fun addIconToPoints(points: List<GeoPoint>, pointIdx: List<Int>) {

        //map.overlayManager.removeAll { it is Marker }

        for (m in this.map.overlays) {
            if (m is Marker) {
                if (m.type == TrackMarkerType.POINT) {
                    this.map.overlayManager.remove(m)
                }
            }
        }

//        println("icon.bounds: ${i?.bounds!!.right}")
//        println("icon.intrinsicWidth: ${i.intrinsicWidth}")
//        i?.setBounds(0, 0, (i.intrinsicWidth * 0.5).toInt(), (i.intrinsicHeight * 0.5).toInt())

        for ((index, p) in points.withIndex()) {
            val marker = Marker(this.map)
            val i =
                ContextCompat.getDrawable(this.requireContext(), R.drawable.ic_brightness_1_black_12dp)
            marker.id = "marker_selected"
            marker.position = p
            marker.setAnchor(0.5f, 0.5f)
            marker.icon = i
            marker.icon.setTint(ContextCompat.getColor(this.requireContext(), R.color.schema_one_dark))
            //marker.icon.updateBounds(0, 0, i?.bounds!!.right, i?.bounds.bottom)
            marker.setInfoWindow(null)
            marker.type = TrackMarkerType.POINT

            marker.setOnMarkerClickListener { clickedMarker: Marker, _: MapView ->
                println("Click on Marker with index $pointIdx")
                this.clickOnMarker(clickedMarker, pointIdx[index])
                true
            }

            this.map.overlays.add(marker)
        }

        //this.map.invalidate()
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
            this.activity?.runOnUiThread {
                Toast.makeText(
                    this.context,
                    "Track segment $pathIndex, distance: $pointsDistanceString",
                    Toast.LENGTH_LONG
                ).show()
            }

        }
    }



    /**
     * Toggles location service on and of?
     */
    private fun onGpsButton(btn: ImageButton) {
        //btn.isEnabled = !btn.isEnabled
        if (btn.alpha == 0.5f) {
            btn.alpha = 1.0f
            this.pathEditEnabled = true

            LocationTrackingService.startService(this.requireContext(), callback = { this.onLocationChangeFromService(it) })
        } else {
            btn.alpha = 0.5f
            this.pathEditEnabled = false

            LocationTrackingService.stopService(this.requireContext())
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

        Toast.makeText(
            this.context,
            " lat: ${location.latitude}, lon: ${location.longitude} ",
            Toast.LENGTH_SHORT
        ).show()

        if (this.trackObject.trackSourceType == TrackSourceType.FILE) {

        } else {
            this.trackObject.addCoord(
                GeoPoint(location.latitude, location.longitude),
                res = { this.updatePath(TrackActionType.AddAtEnd) })
        }
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
            if (this.selectedMarkers.isNotEmpty()) {
                val markerPosition = this.selectedMarkersPathPosition[0]
                this.trackObject.deleteCoord(
                    markerPosition,
                    res = {
                        activity?.runOnUiThread() {
                            this.updatePath(TrackActionType.RemoveSelected)
                            this.unSelectSegment()
                        }

                    })
            }
            //this.unSelectSegment()
            //this.map.invalidate()
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

        if (this.map_toolbar_btn_movePoint.alpha == 1.0f) {
            this.map_toolbar_btn_movePoint.alpha = 0.5f

            this.removeMapListener()
        } else {
            this.map_toolbar_btn_removePoint.alpha = 0.5f
            this.map_toolbar_btn_removePoint.isEnabled = false

            this.map_toolbar_btn_movePoint.alpha = 1.0f
            this.showWayPoints(false)
            this.addMapListener()
        }

    }

    /**
     * When a path segemnt is selected and no marker selected
     * add point to path segment
     *
     * @param btn
     */
    private fun onAddPointButton() {
        if (this.selectedSegment > -1 && this.selectedMarkers.isEmpty()) {
            val distToNext =
                GeoPoint(this.trackObject.coordsGpx[this.selectedSegment - 1]).distanceToAsDouble(
                    this.trackObject.coordsGpx[this.selectedSegment])
            val bearingToNext =
                GeoPoint(this.trackObject.coordsGpx[this.selectedSegment - 1]).bearingTo(this.trackObject.coordsGpx[this.selectedSegment])

            val pointInMiddle =
                GeoPoint(this.trackObject.coordsGpx[this.selectedSegment - 1]).destinationPoint(
                    distToNext / 2,
                    bearingToNext
                )

            this.trackObject.addCoord(
                pointInMiddle,
                this.selectedSegment + 1,
                res = { this.updatePath(TrackActionType.AddMarker) })
        }
    }


    private fun onStaticPathInfo() {
        println("Path distance in meter: ${this.path.distance}")
        var pathDistance = this.path.distance.toFloat()
        var pathDistanceString = "$pathDistance m"

        if (pathDistance >= 1000) {
            pathDistance /= 1000
            pathDistanceString = "%.2f".format(pathDistance) + " km"
        }

        var pathElevation = 0.0f

        val dialog = MapBottomSheetDialog.getInstance(
            pathDistanceString,
            pathElevation.toString()
           )  { this.bottomSheetDismiss() }
        dialog.show(this.requireFragmentManager(), MapBottomSheetDialog::class.java.simpleName)

        this.showWayPoints()
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
            computedDistance += GeoPoint(this.path.points[p]).distanceToAsDouble(this.path.points[p + 1])
                .toFloat()
        }

        var pathDistanceToEnd = this.path.distance.toFloat() - computedDistance

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
            ) { this.bottomSheetDismiss() }

        dialog.show(this.requireFragmentManager(), MapBottomSheetDialog::class.java.simpleName)

    }


    /**
     * Add / display a waypoint to last track point or selected track point
     *
     */
    private fun onAddPathWayPoint() {
        Log.i(OSM_LOG, "FragmentOsmMap onAddPathWayPoint()")
//        val dialog = WayPointNewBottomSheetDialog.getInstance()
//        dialog.show(requireFragmentManager(), WayPointNewBottomSheetDialog::class.java.simpleName)
//        if (this.trackObject.wayPoints.indexOfFirst { it.pointId == this.trackObject.coords[this.selectedMarkersPathPosition.first()].id} == -1) {
//            this.osmBottomSheet?.openOsmBottomSheet("WayPoint_New")
//        }

        if (this.selectedMarkersPathPosition.isEmpty()) {
            val lastPoint = this.trackObject.coords.last().trackPosition.toLong()

            if (this.trackObject.wayPoints.indexOfFirst { it.pointId == lastPoint } == -1 ) {
                this.osmBottomSheet?.openOsmBottomSheet("WayPoint_New")
            }

            return
        }

        val trackPosition = this.trackObject.coords[this.selectedMarkersPathPosition.first()].trackPosition.toLong()
        if (this.trackObject.wayPoints.indexOfFirst { it.pointId == trackPosition } == -1 ) {
            this.osmBottomSheet?.openOsmBottomSheet("WayPoint_New")
        } else {
            Log.i(OSM_LOG, "There is a item for trackPositon: $trackPosition")
            val waypointIdx = this.trackObject.wayPoints.indexOfFirst { it.pointId == trackPosition }
            val wp = this.trackObject.wayPoints[waypointIdx]
            this.osmBottomSheet?.openOsmBottomSheetWithContent("WayPoint", wp)
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
        if (this.selectedMarkers.isNotEmpty()) {
            this.selectedMarkers.first().icon =
                ContextCompat.getDrawable(this.requireContext(), R.drawable.ic_brightness_1_black_12dp)
            this.selectedMarkers.clear()
            this.selectedMarkersPathPosition.clear()
            this.map.invalidate()
        }
    }

    /**
     * Toggle display of current position.
     *
     */
    private fun onGpsButtonStatic(btn: ImageButton) {
        if (btn.alpha == 0.5f) {
            btn.alpha = 1.0f
            //this.pathEditEnabled = true

            currentLocationOverlay = MyLocationNewOverlay(map)
            map.overlays.add(currentLocationOverlay)
            currentLocationOverlay!!.enableMyLocation()
            currentLocationOverlay!!.enableFollowLocation()

            //LocationTrackingService.startService(this.requireContext(), callback = { this.onLocationChangeFromService(it) })
        } else {
            btn.alpha = 0.5f
            map.overlayManager.remove(currentLocationOverlay)
            //map.overlayManager.
            //this.pathEditEnabled = false

            //LocationTrackingService.stopService(this.requireContext())
        }
    }

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
            this.map_toolbar_btn_addPoint.isEnabled = state

            if (state == true) {
                this.map_toolbar_btn_addPoint.alpha = 1.0f
            } else {
                this.map_toolbar_btn_addPoint.alpha = 0.5f
            }
            return true
        }

        // no state requested, toogle state of add button
        // close any marker dialogs, are there any properties in ItemizedOverlayWithFocus which i can use?
        val dialogItem =
            this.map.overlayManager.first { it is ItemizedOverlayWithFocus<*> } as ItemizedOverlayWithFocus<*>
        dialogItem.unSetFocusedItem()

        this.map_toolbar_btn_addPoint.isEnabled = !this.map_toolbar_btn_addPoint.isEnabled

        if (this.map_toolbar_btn_addPoint.isEnabled) {
            this.map_toolbar_btn_addPoint.alpha = 1.0f
            this.activeMarkerUid = id
            //activeMarker = item
        } else {
            this.map_toolbar_btn_addPoint.alpha = 0.5f
            this.activeMarkerUid = null
        }

        return false
    }


    /**
     * Find address data for geoPoint (first one)
     * This one is used by new track creation
     *
     * @param geoPoint clicked point on map
     */
    fun getLocationForTap(geoPoint: GeoPoint) {

        val geoCoder = Geocoder(this.context)
        val addresses = geoCoder.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1)

        if (addresses.isNotEmpty()) {

            this.logAddress((addresses[0]))

            var addressSequence = arrayOf<CharSequence>()

            for ((index, address) in addresses.withIndex()) {

                addressSequence =
                    addressSequence.plusElement("${address.adminArea}, ${address.subLocality}")
            }

            addressSequence = addressSequence.plus("${addresses[0].getAddressLine(0)}")

            val dialogFragment =
                AdressesDialogFragment("Select Location", addressSequence, geoPoint)
            dialogFragment.show(this.parentFragmentManager, "mydia")

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
     * implement this interface in Activity, parent Fragment or test implementation
     */
    interface OsmBottomSheet {
        fun openOsmBottomSheet(type: String)
        fun openOsmBottomSheetWithContent(type: String, trackWayPointModel: TrackWayPointModel)
        //fun openOsmBottomSheetToEdit(type: String, trackWayPointModel: TrackWayPointModel)
    }
}

//private fun MapView.addOnLayoutChangeListener(ol: () -> Unit) {
//
//}


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
        this.map[thisRef] ?: this.setValue(thisRef, property, initializer(thisRef))

    operator fun setValue(thisRef: R, property: KProperty<*>, value: T): T {
        this.map[thisRef] = value
        return value
    }
}

