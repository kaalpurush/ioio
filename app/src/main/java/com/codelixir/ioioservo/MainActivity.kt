package com.codelixir.ioioservo

import android.app.Activity
import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.*
import com.codelixir.ioioservo.HelloIOIOService
import com.codelixir.ioioservo.HelloIOIOService.IHelloIOIOService
import com.codelixir.ioioservo.HelloIOIOService.IOIOBinder

class MainActivity : Activity(), IHelloIOIOService {
    lateinit var toggleButton_: ToggleButton
    var mHelloIOIOService: HelloIOIOService? = null
    var isBound = false
    lateinit var btnStart: Button
    val myConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as IOIOBinder
            mHelloIOIOService = binder.service
            mHelloIOIOService!!.attachListener(this@MainActivity)
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mHelloIOIOService!!.detachListener()
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.action != null && intent.action == "stopActivity") {
            finish()
            return
        }
        enableBluetooth()
        setContentView(R.layout.main)
        toggleButton_ = findViewById(R.id.ToggleButton)
        toggleButton_.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked -> if (isBound) mHelloIOIOService!!.toggleLed() })
        btnStart = findViewById(R.id.btnStart)
        btnStart.setOnClickListener(View.OnClickListener {
            val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (mBluetoothAdapter.isEnabled) {
                startIOIOService()
            }
        })
        if (isServiceRunning) {
            startIOIOService()
        }
    }

    private val isServiceRunning: Boolean
        private get() {
            val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (HelloIOIOService::class.java.canonicalName == service.service.className) {
                    return true
                }
            }
            return false
        }

    private fun startIOIOService() {
        val intent = Intent(this, HelloIOIOService::class.java)
        bindService(intent, myConnection, BIND_AUTO_CREATE)
        startService(intent)
        btnStart!!.visibility = View.GONE
        toggleButton_!!.visibility = View.VISIBLE
    }

    private fun enableBluetooth() {
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!mBluetoothAdapter.isEnabled) {
            mBluetoothAdapter.enable()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action != null && intent.action == "stopActivity") finish()
    }

    override fun onDestroy() {
        if (isBound) unbindService(myConnection)
        super.onDestroy()
    }

    override fun onConnect() {}
    override fun onRollChanged(roll: Int) {
        runOnUiThread {
            val progressBar = findViewById<ProgressBar>(R.id.progressBar)
            progressBar.progress = roll
        }
    }

    override fun onDisconnect() {
        runOnUiThread {
            Toast.makeText(applicationContext, "IOIO Disconnected!", Toast.LENGTH_LONG).show()
        }
    }
}