package com.example.deducechatbox;


import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MessageDao {

    @Insert
    void insertMessage(MessageEntity message);

    @Query("SELECT * FROM messages")
    List<MessageEntity> getAllMessages();

    @Query("DELETE FROM messages")
    void clearMessages();
}