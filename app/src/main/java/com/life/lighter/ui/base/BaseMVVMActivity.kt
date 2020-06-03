package com.life.lighter.ui.base

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding

/**
 * @author liaopeijian
 * @Date 2020-02-04
 */
abstract class BaseMVVMActivity<V : BaseViewModel,D : ViewDataBinding> : BaseActivity() {
    protected lateinit var  viewMode : V
    protected lateinit var  dataBinding : D
    abstract fun obtainViewModel():V
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewMode = obtainViewModel()
        dataBinding = DataBindingUtil.bind(mContentView)!!
        dataBinding.setLifecycleOwner(this)
    }
}