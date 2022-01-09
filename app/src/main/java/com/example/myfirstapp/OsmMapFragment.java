package com.example.myfirstapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link OsmMapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OsmMapFragment extends Fragment {

    private MapView mMapView = null;

    public OsmMapFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment OsmMapFragment.
     */
    public static OsmMapFragment newInstance() {
        OsmMapFragment fragment = new OsmMapFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Debug.
        Log.i(getLogId(), "Map fragment onCreate called.");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Debug.
        Log.i(getLogId(), "Map fragment onCreateView called.");

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_osm_map, container, false);
        mMapView = view.findViewById(R.id.osm_map);

        // Set up the map.
        setUpMap();

        // Return the view.
        return view;
    }

    /**
     * Set up the OSM map.
     */
    private void setUpMap() {
        // Check that map exists.
        MapView map = mMapView;
        assert map != null;

        // Set the tile source.
        map.setTileSource(TileSourceFactory.MAPNIK);

        // Get permission to write to storage.
        // TODO: Move this to the main activity.
//        requestPermissionsIfNecessary(new String[]{
//                Manifest.permission.WRITE_EXTERNAL_STORAGE
//        });

        // Add default zoom buttons.
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.ALWAYS);

        // Add ability to zoom with 2 fingers (multi-touch).
        map.setMultiTouchControls(true);

        // Set zoom level.
        IMapController mapController = map.getController();
        mapController.setZoom(8.0);

        // Set camera location.
        GeoPoint startPoint = new GeoPoint(48.8583, 2.2944);
        mapController.setCenter(startPoint);
    }

    /**
     * Get name for log.
     * @return The name for the log.
     */
    private String getLogId() {
        return "Ping";
    }

    /**
     * Get the map view.
     * @return The map view.
     */
    public MapView getMapView() {
        return mMapView;
    }

    @Override
    public void onResume() {
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        mMapView.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    public void onPause() {
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        mMapView.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }
}