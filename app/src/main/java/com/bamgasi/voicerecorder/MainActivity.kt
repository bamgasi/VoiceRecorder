package com.bamgasi.voicerecorder

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.storage.StorageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import java.io.File


class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private val TAG: String = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        navController = findNavController(R.id.nav_host_fragment)
        /*appBarConfiguration = AppBarConfiguration(setOf(
                R.id.navigation_record, R.id.navigation_list, R.id.navigation_settings))*/
        appBarConfiguration = AppBarConfiguration(navController.graph)

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        checkPermission()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    fun checkPermission() {
        val permissionlistener = object: PermissionListener {
            override fun onPermissionGranted() {
                /*val sdUri = getSDCardUri()
                if (sdUri == null) {
                    val sdPath = AppConfig.getExternalStoragePath()
                    if (sdPath != null) {
                        takeCardUriPermission(sdPath)
                        Log.e("SDCARD", "sdUri: ${sdUri.toString()}")
                    }
                }*/

                AppConfig.setSaveDir()
            }

            override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                finish()
            }
        }

        TedPermission.with(this)
            .setPermissionListener(permissionlistener)
            .setRationaleMessage(R.string.msg_rationale)
            .setDeniedMessage(R.string.msg_deny)
            .setPermissions(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.RECORD_AUDIO
            )
            .check()
    }

    //@RequiresApi(Build.VERSION_CODES.Q)
    fun takeCardUriPermission(sdCardRootPath: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val sdCard = File(sdCardRootPath)
            val storageManager = getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val storageVolume = storageManager.getStorageVolume(sdCard)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                storageVolume?.createOpenDocumentTreeIntent().also { intent ->
                    startActivityForResult(intent, 4010)
                }
            }else{
                storageVolume?.createAccessIntent(null).also { intent ->
                    startActivityForResult(intent, 4010)
                }
            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 4010 && resultCode == RESULT_OK) {
            val uri: Uri = data?.data as Uri
            /*grantUriPermission(
                applicationInfo.packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
            )*/
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver?.takePersistableUriPermission(uri, takeFlags)

        }
    }

    fun getSDCardUri(): Uri? {
        val persistedUriPermissions = contentResolver.persistedUriPermissions

        if (persistedUriPermissions.size > 0) {
            val uriPermission = persistedUriPermissions[0]
            return uriPermission.uri
        }
        return null
    }


}