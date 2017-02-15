package com.coccoc.haversinedistance;

import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private SupportMapFragment mMapFragment;
    private GoogleMap mMap;
    private TextView resultText;
    private int width;
    private int height;
    private int padding;
    private ArrayList<Marker> markerList;
    private Polyline polyline;
    private View targetView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);

        resultText = (TextView) findViewById(R.id.result);

        width = getResources().getDisplayMetrics().widthPixels;
        height = getResources().getDisplayMetrics().heightPixels;
        padding = (int) (width * 0.12); // offset from edges of the map 12% of screen

        markerList = new ArrayList<>(2);

        registerViewHeightChanged();

        // show tutorial layer
        showHelpForView(findViewById(android.R.id.content));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mMap.clear();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;

        // Add marker 1
        LatLng point1 = new LatLng(21.0227431, 105.8194541);
        String add1 = getCompleteAddressString(point1);
        final MarkerOptions markerOp1 = new MarkerOptions().position(point1).title(add1).draggable(true);
        markerList.add(mMap.addMarker(markerOp1)); // save marker to list for using later

        // Add marker 1
        LatLng point2 = new LatLng(13.7248946, 100.4930262);
        String add2 = getCompleteAddressString(point2);
        MarkerOptions markerOp2 = new MarkerOptions().position(point2).title(add2).draggable(true);
        markerList.add(mMap.addMarker(markerOp2)); // save marker to list for using later

        // Draw line between two points
        polyline = drawLine(googleMap, point1, point2);

        // Zoom out map to bounds of two points
        zoomOutCamera(googleMap, point1, point2);

        // Show snackbar result
        showResult(point1, point2);

        // Handle drag and drop marker
        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {

            }

            @Override
            public void onMarkerDrag(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                marker.setTitle(getCompleteAddressString(marker.getPosition()));

                reloadMap();
            }
        });
    }

    private void reloadMap() {
        // recalculate result bases on new position
        LatLng point1 = markerList.get(0).getPosition();
        LatLng point2 = markerList.get(1).getPosition();
        polyline.remove(); // remove old line
        polyline = drawLine(mMap, point1, point2); // draw new line bases on new position

        // Zoom out map to bounds of two points
        zoomOutCamera(mMap, point1, point2);

        // Show snackbar result
        showResult(point1, point2);
    }

    private Polyline drawLine(GoogleMap map, LatLng point1, LatLng point2) {
        PolylineOptions line =
                new PolylineOptions().add(point1, point2)
                        .width(5).color(Color.RED);

        return map.addPolyline(line);
    }

    private void zoomOutCamera(final GoogleMap map, LatLng point1, LatLng point2) {
        // Zoom out map to bounds including two points
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(point1);
        builder.include(point2);
        final LatLngBounds bounds = builder.build();

        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);
        map.moveCamera(cu);
    }

    private void showResult(LatLng point1, LatLng point2) {
        double distance = HaversineDistance.distance(point2.latitude, point2.longitude, point1.latitude, point1.longitude);
        String add1 = getCompleteAddressString(point1);
        String add2 = getCompleteAddressString(point2);

        String distanceStr = "DISTANCE BETWEEN\n";
        String andStr = "\n=====AND=====\n";
        String isStr = "\n=====IS=====\n";
        String kmStr = "km";

        StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
        ForegroundColorSpan redSpan = new ForegroundColorSpan(Color.RED);
        SpannableStringBuilder ssb = new SpannableStringBuilder();

        // DISTANCE BETWEEN
        ssb.append(distanceStr)
                .append(add1)
                .append(andStr)
                .append(add2)
                .append(isStr);

        int start = 0;
        SpannableString ss = new SpannableString(String.format("%.3f", distance));
        ss.setSpan(boldSpan, start, ss.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(redSpan, start, ss.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.append(ss)
                .append(kmStr);

        resultText.setText(ssb);
    }

    private String getCompleteAddressString(LatLng point) {
        String strAdd = "";
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(point.latitude, point.longitude, 1);
            if (addresses != null) {
                Address returnedAddress = addresses.get(0);
                StringBuilder strReturnedAddress = new StringBuilder("");

                for (int i = 0; i < returnedAddress.getMaxAddressLineIndex(); i++) {
                    strReturnedAddress.append(returnedAddress.getAddressLine(i)).append(", ");
                }
                strAdd = strReturnedAddress.toString();
            } else {
                Log.w("My Current location", "No Address returned!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.w("My Current location", "Can't get Address!");
        }
        return strAdd;
    }

    private void registerViewHeightChanged() {
        ViewTreeObserver viewTreeObserver = resultText.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                        // only for gingerbread and newer versions
                        resultText.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                    height -= resultText.getHeight();

                    // reload map for matching with new height
                    reloadMap();
                }
            });
        }
    }

    private void showHelpForView(View viewTarget) {
        new ShowcaseView.Builder(this)
                .withMaterialShowcase()
                .setTarget(new ViewTarget(viewTarget))
                .setContentTitle(R.string.title_tutorial)
                .setContentText(R.string.desc_tutorial)
                .setStyle(R.style.CustomShowcaseTheme)
                .build();
    }
}