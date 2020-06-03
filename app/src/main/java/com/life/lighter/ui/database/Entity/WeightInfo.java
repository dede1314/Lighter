package com.life.lighter.ui.database.Entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * @author zhoujishi
 * @description
 * @date 2020/6/3
 */

@Entity(tableName = "weight_record")
public
class WeightInfo {
    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo(name = "weight")
    public float weight;

    @ColumnInfo(name = "date")
    public String date;

    @ColumnInfo(name = "time")
    public String time;
}
