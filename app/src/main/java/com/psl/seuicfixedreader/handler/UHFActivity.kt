package com.psl.seuicfixedreader.handler

import aidl.IReadListener
import android.content.ContentValues.TAG
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.psl.seuicfixedreader.TagReadingActivity
import com.psl.seuicfixedreader.bean.TagBean
import com.psl.seuicfixedreader.helper.SharedPreferencesUtils
import com.seuic.androidreader.bean.AntPowerData
import com.seuic.androidreader.bean.TagInfo
import com.seuic.androidreader.sdk.Constants
import com.seuic.androidreader.sdk.ReaderErrorCode
import com.seuic.androidreader.sdk.UhfReaderSdk
import com.seuic.androidreader.sdk.UhfReaderSdk.registerReadListener
import com.seuic.androidreader.sdk.UhfReaderSdk.unregisterReadListener
import com.seuic.uhfandroid.ext.currentAntennaArray
import com.seuic.uhfandroid.ext.getTagType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Timer
import java.util.TimerTask

open class UHFActivity() : AppCompatActivity() {
    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    private var workMode = 0
    var connAnt = MutableLiveData<Int>()

    val connectedAntennas = MutableLiveData<IntArray>()

    var clearTagList = MutableLiveData<Boolean>()

    // 单次寻卡得到的数据
    var tagData = MutableLiveData<TagBean>()
    var errData = MutableLiveData<ReaderErrorCode>()
    //var tagListData = MutableLiveData<MutableList<TagBean>>()
    var statenvtick = 0L
    private var startSearching = true
    private val lastTagPostTime = HashMap<String, Long>() // Stores last posted timestamp for each tag
    private val postInterval = 500 // Interval in milliseconds
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initData();

        Constants.connectState.observe(this, {
            if (it.state == Constants.CONNECTED) {
                registerListener()
                //TODO this should be configured
                val abc = setPower(SharedPreferencesUtils(this).getPower())//changed
                Log.e("POWER", "" + abc)
//                Handler().postDelayed({
//                    startInventory()
//                }, 2000)

            }
        })
    }

    fun initData() {
        //绿灯常亮亮灯且蜂鸣器响一声
        Constants.connectState.observeForever { it ->
            if (it.state == 1) {
                //Log.e(TAG, "AIDL已连接")
                MainScope().launch(Dispatchers.IO) {
                    var isOpen = UhfReaderSdk.isReaderOpen();
                    if (isOpen == null || !isOpen) {
                        UhfReaderSdk.connectReader().let {
                            when (it) {
                                ReaderErrorCode.MT_OK_ERR.value -> {
                                    //Log.e(TAG, "连接读写器成功")
                                    //connectResult.postValue(0)
                                    UhfReaderSdk.setLed(2, true)
                                    UhfReaderSdk.setLed(3, false)
                                }
                                else -> withContext(Dispatchers.Main) {
                                    //Log.e(TAG, "连接读写器错误")
                                    //showDialogTip("连接读写器错误${it.toString()}")
                                    UhfReaderSdk.setLed(2, false)
                                    UhfReaderSdk.setLed(3, true)
                                }
                            }
                        }
                    } else {
                        UhfReaderSdk.setLed(2, true)
                        UhfReaderSdk.setLed(3, false)
                    }
                }
            }
            when (it.state) {
                0 -> {
                    //Log.e(TAG, "AIDL未连接")
                    UhfReaderSdk.setLed(2, false)
                    UhfReaderSdk.setLed(3, false)
                }
                2 -> {
                    // Log.e(TAG, "AIDL连接中")
                    UhfReaderSdk.setLed(2, false)
                    UhfReaderSdk.setLed(3, false)
                }

            }
        }

    }
    fun registerListener() {
        tagData.postValue(null)
        clearTagList.postValue(true)
        registerReadListener(readListener)
    }

    fun unregisterListener() {
        unregisterReadListener(readListener)
    }

    private val readListener by lazy {
        object : IReadListener.Stub() {
            override fun tagRead(tags: List<TagInfo>) {
                val currentTime = System.currentTimeMillis()
                for (bean in tags) {
                    when (workMode) {
                        1 -> {
                            // 单次寻卡
                            val tagBean = TagBean(
                                bean.getEpcStr(),
                                bean.getEpcStr(),
                                bean.RSSI,
                                1,
                                bean.getAntennaIDStr(),
                                bean.getEmbeddedStr(),
                                bean.getEpcStr().getTagType(),
                                simpleDateFormat.format(Date())

                            )
                            // 通知标签盘存界面
                            validateAndDoActionsOnRFID(tagBean)
                        }
                        2 -> {
                            // 连续寻卡
                            val epcId = bean.getEpcStr() // Unique identifier for the tag
                            if (!lastTagPostTime.containsKey(epcId) || currentTime - lastTagPostTime[epcId]!! >= postInterval) {
                                lastTagPostTime[epcId] = currentTime // Update last posted time
                                Log.e("TagPostingTime", currentTime.toString())
                                val tagBean = TagBean(
                                    bean.getEpcStr(),
                                    bean.getEpcStr(),
                                    bean.RSSI,
                                    1,
                                    bean.AntennaID.toString(),
                                    bean.getEmbeddedStr(),
                                    bean.getEpcStr().getTagType(),
                                    simpleDateFormat.format(Date())
                                )
                                validateAndDoActionsOnRFID(tagBean)
                            }
                        }
                    }
                }
            }

            override fun tagReadException(errorCode: Int) {
                errData.postValue(ReaderErrorCode.valueOf(errorCode))
            }

            override fun getTag(): String {
                return "" + this.hashCode()
            }
        }
    }


    private fun validateAndDoActionsOnRFID(tagBean:TagBean){
        //setListData(tagBean)
        //create method and check according to our logic
        tagData.postValue(tagBean)
        // 通知标签盘存界面
    }





    fun setPower(power: Int): Boolean {
        var cPower = power
        if (cPower < 5) cPower = 5
        val rp = IntArray(8)
        for ((index, _) in rp.withIndex()) {
            rp[index] = cPower
        }
        val array = mutableListOf<AntPowerData>()
        for (i in 0..7) {
            val antPowerData = AntPowerData().apply {
                antid = i + 1
                readPower = ((100 * rp[i]).toShort())
                writePower = ((100 * rp[i]).toShort())
            }
            array.add(antPowerData)
        }
        val er = UhfReaderSdk.setPower(array.toTypedArray())
        if (er == ReaderErrorCode.MT_OK_ERR.value) {

            return true
        } else {

            return false
        }
    }


    open fun startBuzzer(){
        UhfReaderSdk.startBZ();
    }

    open fun stopBuzzer(){
        UhfReaderSdk.stopBZ();
    }

    open fun buzzerForWrongTag(){
        UhfReaderSdk.startBZ();
        val delayMillis = 5000 // Delay in milliseconds (e.g., 5000 milliseconds = 5 seconds)
        // Schedule the function to be called after the delay
        Timer().schedule(object : TimerTask() {
            override fun run() {
                // Call your function here
                UhfReaderSdk.stopBZ();
            }
        }, delayMillis.toLong())
    }


    open fun buzzerForCorrectTag(){
        UhfReaderSdk.startBZ();
        val delayMillis = 2000 // Delay in milliseconds (e.g., 2000 milliseconds = 2 seconds)
        // Schedule the function to be called after the delay
        Timer().schedule(object : TimerTask() {
            override fun run() {
                // Call your function here
                UhfReaderSdk.stopBZ();
            }
        }, delayMillis.toLong())
    }

    open fun startInventory() {
        //TODO this Antenas should be from server and configured
        currentAntennaArray = intArrayOf()
        val ltp = arrayListOf<Int>()
        ltp.add(1)//Add in list multiple antenas ex if want bto use first and second antena then ltp.add(1) ltp.add(2)
        ltp.add(2) //Add in list multiple antenas ex if want bto use first and second antena then ltp.add(1) ltp.add(2)
        ltp.add(3) //Add in list multiple antenas ex if want bto use first and second antena then ltp.add(1) ltp.add(2)
        ltp.add(4) //Add in list multiple antenas ex if want bto use first and second antena then ltp.add(1) ltp.add(2)
        ltp.add(5) //Add in list multiple antenas ex if want bto use first and second antena then ltp.add(1) ltp.add(2)
        ltp.add(6) //Add in list multiple antenas ex if want bto use first and second antena then ltp.add(1) ltp.add(2)
        ltp.add(7) //Add in list multiple antenas ex if want bto use first and second antena then ltp.add(1) ltp.add(2)
        ltp.add(8) //Add in list multiple antenas ex if want bto use first and second antena then ltp.add(1) ltp.add(2)
        val ants = ltp.toTypedArray()
        currentAntennaArray = IntArray(ants.size)
        for ((index, _) in ants.withIndex()) {
            currentAntennaArray[index] = ants[index]
        }
        workMode = 2
        val abc =  UhfReaderSdk.inventoryStart(currentAntennaArray);
        statenvtick = System.currentTimeMillis()
        //UhfReaderSdk.startBZForSearching()
    }


    open fun stopInventory(): Boolean {
        workMode = 0
        startSearching = false
        //UhfReaderSdk.stopBZForSearching()
        UhfReaderSdk.inventoryStop()
        Log.e("STOP", "Stopped Here");
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        stopInventory()
        unregisterListener()
    }
    open fun getInvantAnt() {
        val activity = this as? TagReadingActivity
        if(activity?.isRFIDActive()==true){
            Log.e("AntennaCheck", "Skipping antenna check because RFID is active")
            return
        }
        UhfReaderSdk.getInvantAnt()?.let { result ->
            if (result.err == ReaderErrorCode.MT_OK_ERR.value) {
                result.data?.let {
                    try {
                        val ltp = arrayListOf<Int>()
                        for (element in 0 until it.antcnt) {
                            when (it.connectedants[element]) {
                                1 -> ltp.add(1)
                                2 -> ltp.add(2)
                                3 -> ltp.add(3)
                                4 -> ltp.add(4)
                                5 -> ltp.add(5)
                                6 -> ltp.add(6)
                                7 -> ltp.add(7)
                                8 -> ltp.add(8)
                            }
                        }
                        val ants = ltp.toTypedArray()
                        currentAntennaArray = IntArray(ants.size)
                        for ((index, _) in ants.withIndex()) {
                            currentAntennaArray[index] = ants[index]
                        }
                        connAnt.postValue(1)
                        // Update the connected antenna array in the SharedViewModel
                        Log.e("HereAnt", currentAntennaArray.toString())
//                        val connectedAntennaArray = it.connectedants
//                        viewModel._connectedAntennaArray.postValue(connectedAntennaArray)
                        connectedAntennas.postValue(currentAntennaArray)

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                Log.e(TAG, "getInvantAnt: 获取失败")
            }
        }
    }
    var power = MutableLiveData<Map<Int, Int>>()
    open fun getAntsPower(antennaIds: List<Int>) {
        UhfReaderSdk.getPower()?.let { result ->
            if (result.err == ReaderErrorCode.MT_OK_ERR.value) {
                val powerMap = mutableMapOf<Int, Int>()
                result.data?.filterNotNull()?.forEach { antPowerData ->
                    if (antennaIds.contains(antPowerData.antid)) {
                        powerMap[antPowerData.antid] = antPowerData.readPower.toInt()
                        Log.i(
                            TAG, "getPower: antId:" + antPowerData.antid.toString()
                                    + " readPower:" + antPowerData.readPower.toString()
                        )
                    }
                }
                power.postValue(powerMap)
            } else {
                Log.e(TAG, "getPower: 获取失败")
            }
        }
    }
}