package com.example.myfirstapp;

/**
 * Proxy class for map markers, to enable handling Google Maps and OSM.
 */
public class MarkerProxy {
    private com.google.android.gms.maps.model.Marker mGoogleMapsMarker = null;
    private org.osmdroid.views.overlay.Marker mOsmMarker = null;

    /**
     * Construct from Google Map marker.
     * @param marker The marker.
     */
    public MarkerProxy(com.google.android.gms.maps.model.Marker marker) {
        mGoogleMapsMarker = marker;
    }

    /**
     * Construct from OSM marker.
     * @param marker The marker.
     */
    public MarkerProxy(org.osmdroid.views.overlay.Marker marker) {
        mOsmMarker = marker;
    }

    /**
     * Set marker visibility.
     * @param visible Whether marker should be visible.
     */
    public void setVisible(boolean visible) {
        if (mGoogleMapsMarker != null) {
            mGoogleMapsMarker.setVisible(visible);
        }

        if (mOsmMarker != null) {
            mOsmMarker.setVisible(visible);
        }
    }
}
