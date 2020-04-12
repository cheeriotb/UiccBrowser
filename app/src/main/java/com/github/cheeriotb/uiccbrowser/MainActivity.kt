package com.github.cheeriotb.uiccbrowser

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.cheeriotb.uiccbrowser.repository.CardRepository
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_PHONE_STATE)) {
                Toast.makeText(this, "The permission was not granted", Toast.LENGTH_SHORT).show()
            } else {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_PHONE_STATE), 0)
            }
            return
        }

        execute()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 0) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                execute()
            }
            return
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun execute() {
        val repository = CardRepository.from(this, 0)
        runBlocking {
            repository!!.initialize()
            repository.cacheFileControlParameters(fileId = "3F00")
            repository.cacheFileControlParameters(fileId = "2F05")
            repository.cacheFileControlParameters(
                "A0000000871002FFFFFFFF8907090000",
                "7FFF",
                "5F3B"
            )
            repository.cacheFileControlParameters(
                "A0000000871002FFFFFFFF8907090000",
                "7FFF5F3B",
                "4F20"
            )
            repository.cacheFileControlParameters(
                "A0000000871002FFFFFFFF8907090000",
                "7FFF5F3B",
                "4F52"
            )
            val list0 = repository.queryFileControlParameters()
            val list1 = repository.queryFileControlParameters(
                "A0000000871002FFFFFFFF8907090000",
                "7FFF"
            )
            val list2 = repository.queryFileControlParameters(
                "A0000000871002FFFFFFFF8907090000",
                "7FFF5F3B"
            )
            Log.d("UiccBrowser", "$list0")
            Log.d("UiccBrowser", "$list1")
            Log.d("UiccBrowser", "$list2")
            repository.readBinary("A0000000871002FFFFFFFF8907090000", "7FFF", "6F46", 0)
            repository.readRecord(CardRepository.AID_NONE, CardRepository.LEVEL_MF, "2F00", 1)
            repository.readRecord(CardRepository.AID_NONE, CardRepository.LEVEL_MF, "2F00", 2)
            repository.dispose()
        }
    }

    private fun b(byte: Int) = byte.toByte()
}
