package com.example.notelivedata

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_main.*
import java.util.logging.Logger
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private val viewModel by viewModels<MainViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // the data of ViewModel changed, here is update
        viewModel.mutableLiveData.observe(this, object : Observer<String> {
            override fun onChanged(t: String?) {
                tvText.text = t
            }
        })

        btnChange.setOnClickListener {
            viewModel.changeName("SunnyDay")
        }

        btnReset.setOnClickListener {
            viewModel.cleanName()
        }

        btnCountDown.setOnClickListener {
            viewModel.changeNameByTimes("Tom")
        }

        thread {
          //  viewModel.mutableLiveData.value = "Test"
            viewModel.mutableLiveData.postValue("Test")
        }

    }

    //注意：手机back键不会立即走这个方法。显示调用finish 或者杀进程才立即走这个。
    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity","onDestroy")
    }

}