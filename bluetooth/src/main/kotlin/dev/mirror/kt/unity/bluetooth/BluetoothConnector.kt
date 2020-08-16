package dev.mirror.kt.unity.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.unity3d.player.UnityPlayer
import com.unity3d.player.UnityPlayerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.coroutines.CoroutineContext

class BluetoothConnector: UnityPlayerActivity(), CoroutineScope {
    companion object {
        private const val FLAG_REQUEST_ENABLE_BT = 1
        private const val FLAG_BTEVENT_START_SEARCH = 1
        private val UUID_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    }

    private lateinit var adapter: BluetoothAdapter
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private var job: Job = Job()
    override val coroutineContext: CoroutineContext = job + Dispatchers.IO

    override fun onCreate(savedInstanceState: Bundle?) {
        checkBluetoothInternal()
        super.onCreate(savedInstanceState)
    }

    private fun checkBluetoothInternal() {
        adapter = BluetoothAdapter.getDefaultAdapter() ?: run {
            sendError("Bluetooth not supported.")
            return
        }

        if (!adapter.isEnabled) {
            sendError("Bluetooth not enabled.")
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(intent, FLAG_REQUEST_ENABLE_BT)

            return
        }


    }

    // 現在のBluetooth modeを確認
    private fun getLocalInformation(adapter: BluetoothAdapter) {
        when(adapter.scanMode) {
            BluetoothAdapter.SCAN_MODE_CONNECTABLE -> sendSuccess("ScanMode CONNECTABLE")
            BluetoothAdapter.SCAN_MODE_NONE -> sendSuccess("ScanMode NONE")
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE -> sendSuccess("ScanMode CONNECTABLE_DISCOVERABLE")
        }
        
        when(adapter.state) {
            BluetoothAdapter.STATE_OFF -> sendSuccess("State OFF")
            BluetoothAdapter.STATE_ON -> sendSuccess("State ON")
            BluetoothAdapter.STATE_TURNING_OFF -> sendSuccess("State TURNING_OFF")
            BluetoothAdapter.STATE_TURNING_ON -> sendSuccess("State TURNING_ON")
        }
    }

    private fun discoverDevices(adapter: BluetoothAdapter) {
        sendSuccess("Searching Bluetooth Devices")
        adapter.startDiscovery()

    }

    class BluetoothReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            sendSuccess("Bluetooth Device found.")
            val action = intent.action
            if(BluetoothDevice.ACTION_FOUND == action) {
                val device = intent .getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return
                sendSuccess("device found [${device.name}]")
            }
        }
    }

    fun connect(deviceAddress: String) {
        adapter.cancelDiscovery()

        val device = adapter.bondedDevices
            .find { it.address == deviceAddress }
            ?: return
        val socket = device.createRfcommSocketToServiceRecord(UUID_SPP)
        socket.connect()

        inputStream = socket.inputStream
        outputStream = socket.outputStream

        launch {
            val stream = inputStream ?: return@launch
            val bytes = ByteArray(16)
            var read = 0
            while (stream.read(bytes, 0, bytes.size).also { read = it } != -1) {
                // 配列bytesに読み込まれたバイナリデータで何かをする
                // 配列bytesには、インデックスreadまでデータが読みこまれている
                // Android側からUnityへ送れる値はStringしかないっぽい…？要調査
            }
        }
    }
}

private fun sendSuccess(message: String) {
    UnityPlayer.UnitySendMessage("BluetoothConnector", "OnSuccess", message)
}

private fun sendError(message: String) {
    UnityPlayer.UnitySendMessage("BluetoothConnector", "OnError", message)
}