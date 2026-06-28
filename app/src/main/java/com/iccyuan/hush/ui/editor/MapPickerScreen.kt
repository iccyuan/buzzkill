package com.iccyuan.hush.ui.editor

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.model.CircleOptions
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.MyLocationStyle

/**
 * 内嵌在条件弹窗里的高德地图选点：开启「我的位置」蓝点 + 定位按钮，打开时自动定位到当前位置；
 * 点地图落点回调 [onPick]。MapView 生命周期需手动转发。
 */
@Composable
fun LocationMap(
    lat: Double,
    lng: Double,
    radiusMeters: Int,
    onPick: (Double, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }
    // 是否已经把镜头移到过当前位置（仅首次自动居中，避免反复抢镜头）。
    val centeredOnce = remember { booleanArrayOf(false) }

    LaunchedEffect(Unit) {
        runCatching {
            MapsInitializer.updatePrivacyShow(context, true, true)
            MapsInitializer.updatePrivacyAgree(context, true)
        }
        val needed = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .filter { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
        if (needed.isNotEmpty()) permLauncher.launch(needed.toTypedArray())
    }

    val mapView = remember { MapView(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        mapView.onCreate(null)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    LaunchedEffect(mapView) {
        val map = mapView.map
        map.uiSettings.isZoomControlsEnabled = false
        map.uiSettings.isMyLocationButtonEnabled = true
        // 我的位置蓝点（不自动居中，由下方监听首次定位手动居中）。
        map.myLocationStyle = MyLocationStyle()
            .myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
            .interval(2000)
        map.isMyLocationEnabled = true

        if (lat != 0.0 || lng != 0.0) {
            centeredOnce[0] = true
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 16f))
        }
        // 首次定位成功时，若用户尚未选点，自动把镜头移到当前位置。
        map.setOnMyLocationChangeListener { loc ->
            if (!centeredOnce[0] && loc != null && (loc.latitude != 0.0 || loc.longitude != 0.0)) {
                centeredOnce[0] = true
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 16f))
            }
        }
        map.setOnMapClickListener { p -> onPick(p.latitude, p.longitude) }
    }

    // 选点/半径变化时重绘标记 + 半径圈。
    LaunchedEffect(lat, lng, radiusMeters) {
        val map = mapView.map
        map.clear(true)
        if (lat != 0.0 || lng != 0.0) {
            val point = LatLng(lat, lng)
            map.addMarker(MarkerOptions().position(point))
            map.addCircle(
                CircleOptions().center(point).radius(radiusMeters.toDouble())
                    .strokeWidth(3f).strokeColor(0xFFFF3B30.toInt()).fillColor(0x33FF3B30)
            )
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier.fillMaxSize())
}
