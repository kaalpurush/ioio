package com.codelixir.ioioservo

import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.codelixir.ioioservo.HelloIOIOService.IHelloIOIOService
import com.codelixir.ioioservo.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : BaseActivity<ActivityMainBinding>(), IHelloIOIOService {
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

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED == intent.action) {
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
                    BluetoothAdapter.STATE_OFF -> {
                        stopIOIOService()
                        binding.btnBluetooth.visibility = View.VISIBLE
                    }
                    BluetoothAdapter.STATE_TURNING_ON -> {
                    }
                    BluetoothAdapter.STATE_ON -> {
                        startIOIOService()
                        binding.btnBluetooth.visibility = View.GONE
                    }
                    BluetoothAdapter.STATE_TURNING_OFF -> {
                    }
                }
            }
        }
    }

    override fun getViewBinding(inflater: LayoutInflater)= ActivityMainBinding.inflate(inflater)

    override fun onResume() {
        super.onResume()
        registerReceiver(mReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mReceiver);
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.action != null && intent.action == "stopActivity") {
            finish()
            return
        }

        binding.toggleButton.setOnCheckedChangeListener { buttonView, isChecked ->
            if (mBound) mService.toggleLed()
        }

        binding.seekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (mBound)
                    mService.seek = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })

        enableBluetoothAndStartService()
    }

    fun enableBluetoothAndStartService() {
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter.isEnabled) {
            startIOIOService()
            binding.btnBluetooth.visibility = View.GONE
        } else {
            enableBluetooth()
        }
    }

    fun setSeek(view: View) {
        if (!mBound) return
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
    }

    private fun stopIOIOService() {
        if (mBound) unbindService(mConnection)
        val intent = Intent(this, HelloIOIOService::class.java)
        stopService(intent)
    }

    fun enableBluetooth(view: View? = null) {
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

            binding.layoutConnected.visibility = View.VISIBLE
            binding.layoutDisconnected.visibility = View.GONE
        }
    }

    override fun onRollChanged(roll: Int) {
        runOnUiThread {
            binding.progressBar.progress = roll
            binding.tvProgress.text = roll.toString()
        }
    }

    override fun onDisconnect() {
        runOnUiThread {
            Toast.makeText(applicationContext, "IOIO Disconnected!", Toast.LENGTH_LONG).show()

            binding.layoutConnected.visibility = View.GONE
            binding.layoutDisconnected.visibility = View.VISIBLE
        }
    }
}