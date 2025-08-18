package io.githun.mucute.qwq.kolomitm.vpnservicedemo.application

import android.app.Application


class AppContext : Application() {

    companion object {

        lateinit var instance: AppContext
            private set

    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

}