package com.codelixir.ioioservo

import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.codelixir.ioioservo.HelloIOIOService.IHelloIOIOService
import com.codelixir.ioioservo.HelloIOIOService.IOIOBinder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), IHelloIOIOService {
    private lateinit var toggleButton: ToggleButton
    lateinit var btnStart: Button
    lateinit var progressBar: ProgressBar
    lateinit var seekBar: SeekBar
    lateinit var tvProgress: TextView

    private lateinit var mService: HelloIOIOService
    private var mBound: Boolean = false

    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as HelloIOIOService.IOIOBinder
            mService = binder.service
            mService.attachListener(this@MainActivity)
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mService.detachListener()
            mBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.action != null && intent.action == "stopActivity") {
            finish()
            return
        }
        enableBluetooth()
        setContentView(R.layout.activity_main)
        toggleButton = findViewById(R.id.ToggleButton)
        toggleButton.setOnCheckedChangeListener { buttonView, isChecked ->
            if (mBound) mService.toggleLed()
        }
        btnStart = findViewById(R.id.btnStart)
        btnStart.setOnClickListener {
            val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (mBluetoothAdapter.isEnabled) {
                startIOIOService()
            }
        }

        progressBar = findViewById(R.id.progressBar)
        tvProgress = findViewById(R.id.tvProgress)
        seekBar = findViewById(R.id.seekBar)

        seekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                mService.seek = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })

        if (isServiceRunning) {
            startIOIOService()
        }
    }

    fun setSeek(view: View) {
        val seekStart = (view as TextView).text.toString().toInt()
        val seekEnd = (view as TextView).tag.toString().toInt()
        lifecycleScope.launch {
            if (seekStart < seekEnd) {
                for (i in seekStart..seekEnd) {
                    mService.seek = i
                    delay(10)
                }
            } else {
                for (i in seekStart downTo seekEnd) {
                    mService.seek = i
                    delay(10)
                }
            }
        }
    }

    private val isServiceRunning: Boolean
        get() {
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
        bindService(intent, mConnection, BIND_AUTO_CREATE)
        startService(intent)

        btnStart.visibility = View.GONE
        toggleButton.visibility = View.VISIBLE
    }

    private fun stopIOIOService() {
        if (mBound) unbindService(mConnection)
        val intent = Intent(this, HelloIOIOService::class.java)
        stopService(intent)
    }

    private fun enableBluetooth() {
        BluetoothAdapter.getDefaultAdapter().run { if (!isEnabled) enable() }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == "stopActivity") finish()
    }

    override fun onDestroy() {
        stopIOIOService()
        super.onDestroy()
    }

    override fun onConnect() {
        runOnUiThread {
            Toast.makeText(applicationContext, "IOIO Connected!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRollChanged(roll: Int) {
        runOnUiThread {
            progressBar.progress = roll
            tvProgress.text = roll.toString()
        }
    }

    override fun onDisconnect() {
        runOnUiThread {
            Toast.makeText(applicationContext, "IOIO Disconnected!", Toast.LENGTH_LONG).show()
        }
    }
}