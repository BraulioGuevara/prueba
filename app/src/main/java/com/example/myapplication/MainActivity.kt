package com.example.myapplication

import android.app.AppOpsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import java.io.File
import java.io.FileWriter

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var appAdapter: AppAdapter
    private val appsList = mutableListOf<String>()

    private val foregroundAppReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val appName = intent?.getStringExtra("appName")
            if (appName != null) {
                appsList.add(appName)
                appAdapter.notifyItemInserted(appsList.size - 1)
                saveAppsListToJson()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Configurar RecyclerView
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        appAdapter = AppAdapter(appsList)
        recyclerView.adapter = appAdapter

        // Verificar y solicitar permisos
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED
            ) {
                requestLocationPermissions()
            } else {
                if (!hasUsageStatsPermission()) {
                    requestUsageStatsPermission()
                } else {
                    startForegroundService()
                }
            }
        } else {
            if (!hasUsageStatsPermission()) {
                requestUsageStatsPermission()
            } else {
                startForegroundService()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasUsageStatsPermission()) {
            startForegroundService()
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), packageName
            )
        } else {
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    private fun saveAppsListToJson() {
        try {
            val gson = Gson()
            val jsonString = gson.toJson(appsList)
            val file = File(filesDir, "appsList.json")
            FileWriter(file).use { writer ->
                writer.write(jsonString)
            }
            Log.d("MainActivity", "Archivo JSON guardado exitosamente")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error al guardar el archivo JSON: ${e.message}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStart() {
        super.onStart()
        registerReceiver(foregroundAppReceiver, IntentFilter("com.example.myapplication.FOREGROUND_APP_UPDATED"),
            RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(foregroundAppReceiver)
    }

    private fun startForegroundService() {
        val serviceIntent = Intent(this, ForegroundAppService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.FOREGROUND_SERVICE
            ),
            1
        )
    }
}
