package com.life.lighter.ui.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.life.lighter.ui.database.Entity.WeightInfo;

import java.util.List;

/**
 * @author zhoujishi
 * @description
 * @date 2020/6/3
 */
@Dao
public interface UserDao {
    @Query("SELECT * FROM weight_record")
    List<WeightInfo> getAll();

    @Query("SELECT * FROM weight_record WHERE uid IN (:userIds)")
    List<WeightInfo> loadAllByIds(int[] userIds);


    @Insert
    void insertAll(WeightInfo... WeightInfo);

    @Delete
    void delete(WeightInfo WeightInfo);
}
