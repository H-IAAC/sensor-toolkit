package br.org.eldorado.hiaac.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class LabelConfig {
    @NonNull
    @PrimaryKey
    public String label;

    public LabelConfig(@NonNull String label) {
        this.label = label;
    }
}
