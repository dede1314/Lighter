package com.life.lighter.ui.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel

/**
 * @author liaopeijian
 * @Date 2020-02-04
 */
open class BaseViewModel(application: Application) : AndroidViewModel(application) {

    open fun initData(){}

    open fun destory(){}

}