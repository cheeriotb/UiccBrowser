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
import com.github.cheeriotb.uiccbrowser.repository.FileId
import com.github.cheeriotb.uiccbrowser.repository.ReadAllRecordsParams
import com.github.cheeriotb.uiccbrowser.repository.ReadBinaryParams
import com.github.cheeriotb.uiccbrowser.util.byteArrayToHexString
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
            repository.cacheFileControlParameters(FileId(fileId = "3F00"))
            repository.cacheFileControlParameters(FileId(fileId = "2F05"))
            repository.cacheFileControlParameters(FileId(
                "A0000000871002FFFFFFFF8907090000",
                "7FFF",
                "5F3B"
            ))
            repository.cacheFileControlParameters(FileId(
                "A0000000871002FFFFFFFF8907090000",
                "7FFF5F3B",
                "4F20"
            ))
            repository.cacheFileControlParameters(FileId(
                "A0000000871002FFFFFFFF8907090000",
                "7FFF5F3B",
                "4F52"
            ))
            val list0 = repository.queryFileControlParameters(FileId())
            val list1 = repository.queryFileControlParameters(FileId(
                "A0000000871002FFFFFFFF8907090000",
                "7FFF"
            ))
            val list2 = repository.queryFileControlParameters(FileId(
                "A0000000871002FFFFFFFF8907090000",
                "7FFF5F3B"
            ))
            Log.d("UiccBrowser", "$list0")
            Log.d("UiccBrowser", "$list1")
            Log.d("UiccBrowser", "$list2")
            val readBinaryParams = ReadBinaryParams.Builder(
                    FileId.Builder("A0000000871002FFFFFFFF8907090000", "7FFF", "6F46").build())
                    .build()
            repository.readBinary(readBinaryParams)
            val readAllRecordParams = ReadAllRecordsParams.Builder(
                    FileId.Builder(FileId.AID_NONE, FileId.PATH_MF, "2F00").build())
                    .numberOfRecords(2)
                    .build()
            val records = repository.readAllRecords(readAllRecordParams)
            Log.d("UiccBrowser", "#1 : " + byteArrayToHexString(records[0].data) + " "
                    + records[0].sw.toString(16))
            Log.d("UiccBrowser", "#2 : " + byteArrayToHexString(records[1].data) + " "
                    + records[1].sw.toString(16))
            repository.dispose()
        }
    }

    private fun b(byte: Int) = byte.toByte()
}
