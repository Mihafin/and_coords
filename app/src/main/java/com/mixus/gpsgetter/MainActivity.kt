package com.mixus.gpsgetter

import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.location.Location
import android.location.Location.FORMAT_DEGREES
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import kotlinx.android.synthetic.main.activity_main.*
import pub.devrel.easypermissions.EasyPermissions

object AppConstants {
    const val LOCATION_REQUEST = 1000
//    const val GPS_REQUEST = 1001
    const val LOCATION_IS_OPENED_CODE = 1002
}

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    companion object {
        private const val TAG = "FragmentActivity"
    }

    private var mCurrentLocation: Location? = null
    private lateinit var mLocationCallback: LocationCallback
    private var mRequestingLocationUpdates: Boolean = false;
    private var mLocationRequest: LocationRequest? = null
    private var mLocationSettingsRequest: LocationSettingsRequest? = null
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mSettingsClient: SettingsClient? = null
    private var textView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        //Create location callback when it's ready.
        createLocationCallback()

        //createing location request, how mant request would be requested.
        createLocationRequest()

        //Build check request location setting request
        buildLocationSettingsRequest()

        //FusedLocationApiClient which includes location
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        //Location setting client
        mSettingsClient = LocationServices.getSettingsClient(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.lblCenter)
        textView?.setText(R.string.Waiting)


        Log.i(TAG, "======> Start check permisions.")
        if (!EasyPermissions.hasPermissions(
                this@MainActivity,
                Manifest.permission.ACCESS_FINE_LOCATION)
        ) {
            textView?.setText(R.string.NeedPermision)
            requestPermissionsRequired()
        }
        else {
            //If you have the permission we should check location is opened or not
            textView?.setText(R.string.Hi);
            checkLocationIsTurnedOn()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "======> onActivityResult")

        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            AppConstants.LOCATION_IS_OPENED_CODE -> {
                if (resultCode == AppCompatActivity.RESULT_OK) {
                    Log.d(TAG, "======> Location result is OK")
                }
                else {
                    Log.d(TAG, "======> Location result is NOT OK")
                    //activity?.finish()
                }
            }
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Log.d(TAG, "======> onPermissionsDenied")
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        Log.d(TAG, "======> onPermissionsGranted")
        checkLocationIsTurnedOn()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        Log.d(TAG, "======> onRequestPermissionsResult")
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults, this
        )
    }


    private fun requestPermissionsRequired() {
        EasyPermissions.requestPermissions(
            this,
            getString(R.string.location_is_required_msg),
            AppConstants.LOCATION_REQUEST,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private fun checkLocationIsTurnedOn() { // Begin by checking if the device has the necessary location settings.
        mSettingsClient!!.checkLocationSettings(mLocationSettingsRequest)
            .addOnSuccessListener(this) {
                Log.i(TAG, "======> All location settings are satisfied.")
                startLocationUpdates()
            }
            .addOnFailureListener(this) { e ->
                val statusCode = (e as ApiException).statusCode
                when (statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        try {
                            val rae = e as ResolvableApiException
                            rae.startResolutionForResult(this@MainActivity, AppConstants.LOCATION_IS_OPENED_CODE)
                        } catch (sie: IntentSender.SendIntentException) {
                        }
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        mRequestingLocationUpdates = false
                    }
                }
            }
    }

    private fun buildLocationSettingsRequest() {
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest!!)
        mLocationSettingsRequest = builder.build()
        builder.setAlwaysShow(true)
    }

    private fun createLocationRequest() {
        mLocationRequest = LocationRequest.create()
        mLocationRequest!!.interval = 0
        mLocationRequest!!.fastestInterval = 0
        mLocationRequest!!.numUpdates = 1
        mLocationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private fun startLocationUpdates() {
        mFusedLocationClient!!.requestLocationUpdates(
            mLocationRequest,
            mLocationCallback, null
        )
    }

    private fun createLocationCallback() {
        //Here the location will be updated, when we could access the location we got result on this callback.
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                mCurrentLocation = locationResult.lastLocation
                if (mCurrentLocation != null) {
                    lblCenter.text = String.format("%s x %s",
                        Location.convert(mCurrentLocation!!.latitude, FORMAT_DEGREES),
                        Location.convert(mCurrentLocation!!.longitude, FORMAT_DEGREES))
                }
            }
        }
    }
}
