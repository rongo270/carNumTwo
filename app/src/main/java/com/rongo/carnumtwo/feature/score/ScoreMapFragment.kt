package com.rongo.carnumtwo.feature.score

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.rongo.carnumtwo.R
import com.rongo.carnumtwo.core.storage.ScoreStorage

class ScoreMapFragment : Fragment(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_score_map, container, false)

        // Initialize Map Fragment inside this fragment
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        return view
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Load scores and place markers
        val storage = ScoreStorage(requireContext())
        val scores = storage.getTopScores()

        for (item in scores) {
            // Only add marker if coordinates are valid (not 0,0 typically implies no GPS)
            if (item.lat != 0.0 || item.lon != 0.0) {
                val position = LatLng(item.lat, item.lon)
                map.addMarker(MarkerOptions().position(position).title("Score: ${item.score}"))
            }
        }
    }

    // Public method to move camera to specific location
    fun focusOnLocation(lat: Double, lon: Double) {
        if (lat == 0.0 && lon == 0.0) return // Ignore invalid locations
        val pos = LatLng(lat, lon)
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f))
    }
}