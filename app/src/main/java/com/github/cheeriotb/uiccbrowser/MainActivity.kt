package com.github.cheeriotb.uiccbrowser

import android.Manifest
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.cheeriotb.uiccbrowser.cardio.Command
import com.github.cheeriotb.uiccbrowser.cardio.TelephonyInterface
import com.github.cheeriotb.uiccbrowser.util.byteArrayToHexString
import java.lang.Exception
import java.security.MessageDigest

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

    @RequiresApi(Build.VERSION_CODES.P)
    private fun execute() {
        val packageInfo = packageManager.getPackageInfo(packageName,
                PackageManager.GET_SIGNING_CERTIFICATES)
        var signatures = packageInfo.signatures
        val signingInfo = packageInfo.signingInfo
        if (signingInfo != null) {
            signatures = signingInfo.signingCertificateHistory
            if (signingInfo.hasMultipleSigners()) {
                signatures = signingInfo.apkContentsSigners
            }
        }
        for (signature in signatures) {
            Log.d("UiccBrowser", "signature = " + byteArrayToHexString(getCertHash(signature)))
        }

        val tif = TelephonyInterface.from(this, 0)
        tif.openChannel(TelephonyInterface.BASIC_CHANNEL_AID)
        val response = tif.transmit(Command(0xA4, 0x00, 0x04, byteArrayOf(b(0x3F), b(0x00))))
        Log.d("UiccBrowser", "response.data = " + byteArrayToHexString(response.data))
        Log.d("UiccBrowser", "response.sw = " + response.sw.toString(16))
        Toast.makeText(this, response.sw.toString(16), Toast.LENGTH_SHORT).show()
        tif.closeRemainingChannel()
        tif.dispose()
    }

    private fun getCertHash(signature: Signature): ByteArray {
        try {
            val md = MessageDigest.getInstance("SHA-1")
            return md.digest(signature.toByteArray())
        } catch (e: Exception) {
        }
        return ByteArray(0)
    }

    private fun b(byte: Int) = byte.toByte()
}
