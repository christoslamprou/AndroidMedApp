package data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class Note {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String title;
    public String body;
    public long createdAt;

    public Note(String title, String body, long createdAt) {
        this.title = title;
        this.body = body;
        this.createdAt = createdAt;
    }
}

