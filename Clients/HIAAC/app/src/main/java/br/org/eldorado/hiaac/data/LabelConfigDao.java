package br.org.eldorado.hiaac.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface LabelConfigDao {
    @Query("SELECT * FROM LabelConfig ORDER BY label ASC")
    LiveData<List<LabelConfig>> getAll();

    @Query("SELECT label FROM LabelConfig ORDER BY label ASC")
    LiveData<List<String>> getAllLabels();

    @Query("SELECT * FROM LabelConfig WHERE label=:id")
    LiveData<LabelConfig> getLabelConfigById(String id);

    @Insert
    public void insert(LabelConfig labelConfig);

    @Update
    public void update(LabelConfig labelConfig);

    @Delete
    public void delete(LabelConfig labelConfig);
}
