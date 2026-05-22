package com.primetv.app

import android.app.Application
import com.primetv.app.data.db.AppDatabase
import com.primetv.app.data.repository.TmdbRepository
import com.primetv.app.util.Prefs

class App : Application() {

    lateinit var prefs: Prefs
        private set
    lateinit var db: AppDatabase
        private set
    lateinit var tmdb: TmdbRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = Prefs(this)
        db = AppDatabase.get(this)
        tmdb = TmdbRepository(db.contentDao())
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
