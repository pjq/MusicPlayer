package me.pjq.musicplayer;

import java.io.Serializable;

/**
 * Created by pjq on 4/12/15.
 */
public class MusicAlbumObject implements Serializable {
    private String listId;
    private String listName;
    private boolean online;

    public String getListId() {
        return listId;
    }

    public void setListId(String listId) {
        this.listId = listId;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public String getListName() {
        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
    }
}
