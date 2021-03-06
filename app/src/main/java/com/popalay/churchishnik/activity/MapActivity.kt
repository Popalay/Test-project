package com.popalay.churchishnik.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.method.ScrollingMovementMethod
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.popalay.churchishnik.R
import com.popalay.churchishnik.bindView
import com.popalay.churchishnik.util.Api
import com.popalay.churchishnik.util.LocationListener
import kotlin.properties.Delegates


class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {

        private const val PERMISSION_REQUEST_CODE = 121
    }

    private var map: GoogleMap by Delegates.notNull()
    private var locationListener: LocationListener by Delegates.notNull()
    private val textLastMessage: TextView by bindView(R.id.text_last_message)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        locationListener = LocationListener(applicationContext)
        textLastMessage.movementMethod = ScrollingMovementMethod()
        listenLastMessage()
        textLastMessage.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        locationListener.startLocationUpdates()
        listenPoints()
    }

    override fun onPause() {
        super.onPause()
        locationListener.stopLocationUpdates()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap.apply {
            if (ActivityCompat.checkSelfPermission(this@MapActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                requestMyLocation(this)
            } else {
                requestPermission()
            }
            uiSettings.apply {
                isZoomControlsEnabled = true
                isZoomControlsEnabled = true
                isRotateGesturesEnabled = true
                isMyLocationButtonEnabled = true
            }
            setOnInfoWindowClickListener {
                val index = it.tag as Int
                startActivity(PointActivity.getIntent(this@MapActivity, index))
            }
        }
        listenPoints()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestMyLocation(map)
                locationListener.startLocationUpdates()
            } else {
                Toast.makeText(this, "Ох и дурак, ну заходи теперь заново..", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            Toast.makeText(this, "Без локации придется на мхе ориентироваться!", Toast.LENGTH_LONG).show()
        }
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
    }

    @SuppressLint("MissingPermission")
    private fun requestMyLocation(map: GoogleMap) {
        map.isMyLocationEnabled = true
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let {
            LatLng(it.latitude, it.longitude)
        }?.run {
            map.moveCamera(CameraUpdateFactory.newLatLng(this))
        }
    }

    private fun listenPoints() {
        Api.fetchPoints { points ->
            map.clear()
            val nextIndex = Api.getNextPoint()
            if (nextIndex >= points.size) {
                AlertDialog.Builder(this)
                        .setTitle("ПОБЕДА!")
                        .setMessage("Прошел по всем хуям! Прикатуй добухивать и внимать зеленого друга!")
                        .show()
                return@fetchPoints
            }
            points.find { it.index == nextIndex }?.let { currentPoint ->
                val options = MarkerOptions().apply {
                    title("Точка number $nextIndex")
                    snippet("Жми сюда, ало бля")
                    position(LatLng(currentPoint.location.latitude, currentPoint.location.longitude))
                }
                map.addMarker(options).apply { tag = currentPoint.index }
                map.moveCamera(CameraUpdateFactory.newLatLng(options.position))
            }
        }
    }

    private fun listenLastMessage() {
        Api.fetchLastMessage { message ->
            textLastMessage.text = message.text
        }
    }
}
