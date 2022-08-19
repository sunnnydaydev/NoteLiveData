package com.example.notelivedata

import android.os.CountDownTimer
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * Create by SunnyDay /08/19 11:41:49
 */
class MainViewModel : ViewModel() {
    private var count = 0

    companion object {
        private const val DEFAULT = "defaultName"
    }

    val mutableLiveData: MutableLiveData<String> = MutableLiveData()

    init {
        mutableLiveData.value = DEFAULT
    }

    fun changeName(name: String) {
        mutableLiveData.value = "$name:${++count}"
    }

    fun cleanName() {
        mutableLiveData.value = DEFAULT
        count = 0
    }

    fun changeNameByTimes(name: String){
        object : CountDownTimer(1000*60,1000) {
            override fun onTick(millisUntilFinished: Long) {
                  mutableLiveData.value = "$name:${millisUntilFinished/1000}"
            }

            override fun onFinish() {
               cleanName()
            }
        }.start()
    }
}