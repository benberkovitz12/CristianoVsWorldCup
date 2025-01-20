package com.example.cristianovsworldcup

import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Locale

class MapFragment : Fragment(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        Log.d("MapFragment", "Google Map is ready.")
    }


    fun zoomToLocation(locationName: String) {
        if (googleMap == null) {
            Log.e("MapFragment", "Google Map is not ready.")
            return
        }

        try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses = geocoder.getFromLocationName(locationName, 1)

            if (addresses.isNullOrEmpty()) {
                Log.e("MapFragment", "No matching location found for: $locationName")
                Toast.makeText(requireContext(), "Location not found: $locationName", Toast.LENGTH_SHORT).show()
                return
            }

            val address = addresses.first()
            val latLng = LatLng(address.latitude, address.longitude)


            googleMap?.clear()
            googleMap?.addMarker(MarkerOptions().position(latLng).title(locationName))
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12f))

            Log.d("MapFragment", "Zoomed to location: $locationName at $latLng")
        } catch (e: Exception) {
            Log.e("MapFragment", "Error finding location: ${e.message}")
            Toast.makeText(requireContext(), "Error finding location: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}