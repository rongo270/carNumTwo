package com.rongo.carnumtwo.feature.game

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.rongo.carnumtwo.R

class LocationPickerActivity : AppCompatActivity(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null
    private var selectedLocation: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_picker)

        // Initialize Map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_picker) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Confirm Button Logic
        findViewById<Button>(R.id.btn_confirm_location).setOnClickListener {
            if (selectedLocation != null) {
                // Return result to GameActivity
                val resultIntent = Intent()
                resultIntent.putExtra("lat", selectedLocation!!.latitude)
                resultIntent.putExtra("lon", selectedLocation!!.longitude)
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } else {
                Toast.makeText(this, "Please select a location on the map first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Default view (e.g., center on a general region)
        val defaultLoc = LatLng(32.0853, 34.7818) // Tel Aviv roughly
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLoc, 6f))

        // Handle Map Taps
        map.setOnMapClickListener { latLng ->
            selectedLocation = latLng

            // Visual Feedback: Clear old markers and add new one
            map.clear()
            map.addMarker(MarkerOptions().position(latLng).title("Selected Location"))
        }
    }
}