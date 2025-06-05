package br.org.eldorado.hiaac.datacollector.data;

import androidx.room.Dao;
import androidx.room.RawQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

@Dao
public interface DbConfig {
    @RawQuery
    String setPragma(SupportSQLiteQuery query);
}
