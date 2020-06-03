package com.life.lighter.ui.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.life.lighter.ui.database.Entity.WeightInfo;

/**
 * @author zhoujishi
 * @description
 * @date 2020/6/3
 */
@Database(entities = {WeightInfo.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    //volatile防止重排序
    private static volatile AppDatabase mAppDatabase;

    public abstract UserDao userDao();

    public static AppDatabase getSingleton(Context context) {
        if (mAppDatabase == null) {
            synchronized (AppDatabase.class) {
                if (mAppDatabase == null) {
                    //主线程使用.allowMainThreadQueries().build();创建
                    mAppDatabase = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "user.db")
                            .allowMainThreadQueries()
                            .build();

                }
            }
        }
        return mAppDatabase;
    }
}