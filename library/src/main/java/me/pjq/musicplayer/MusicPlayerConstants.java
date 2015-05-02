package me.pjq.musicplayer;

public interface MusicPlayerConstants {

    public static final String BIND_ACTION = "me.pjq.musicplayer.MusicPlayerService";

    // 1s
    public static final int PROGRESS_REFRESH_INTERVAL = 1000;

    public static final int STEP_FREQUENCY_REFRESH_INTERVAL = 5000;

    public static final int PROGRESS_REFRESH_INTERVAL_SECOND = 1;

    /**
     * 防抖动,seek时间和当前更新进度时间差
     */
    public static final int PROGRESS_SHAKE_DURATION_MS = 3000;

    /**
     * 防抖动，刷新播放进度时进度和seek时进度差
     */
    public static final int PROGRESS_SKIP_DURATION = 5;

    public static final String KEY_PLAYER_COMMAND = "player_command";

    public static final String KEY_URL = "url";

    public static final String KEY_POSOTION = "position";

    public static final String KEY_ITEM = "player_item";

    public static final String KEY_TYPE = "type";

    public static final String KEY_COUNT = "count";

    public static final String KEY_FREQ = "freq";

    public static final String KEY_AVG_FREQ = "avg_freq";

    public static final String KEY_INDEX = "index";

    public static final String KEY_MESSAGE = "msg";

    public static final String KEY_PERCENT = "percent";

    public static final String KEY_WIDTH = "width";

    public static final String KEY_HEIGHT = "height";

    public static final String KEY_WHAT = "what";

    public static final String KEY_EXTRA = "extra";

    public static final String KEY_ACTION = "action";

    public static final String KEY_URI = "uri";

    public static final String KEY_LIST = "list";

    public static final String KEY_BOOK = "book";

    public static final String KEY_START_SOURCE_FROM = "start_source_from";

    public static final int COMMAND_PLAYER_INVALID = -1;

    public static final int COMMAND_PLAYER_START = 1;

    public static final int COMMAND_PLAYER_STOP = COMMAND_PLAYER_START + 1;

    public static final int COMMAND_PLAYER_PAUSE = COMMAND_PLAYER_STOP + 1;

    public static final int COMMAND_PLAYER_SEEK = COMMAND_PLAYER_PAUSE + 1;

    public static final int COMMAND_PLAYER_NEXT = COMMAND_PLAYER_SEEK + 1;

    public static final int COMMAND_PLAYER_PREV = COMMAND_PLAYER_NEXT + 1;

    public static final int COMMAND_APPEND_ITEM = COMMAND_PLAYER_PREV + 1;

    public static final int COMMAND_APPEND_LIST = COMMAND_APPEND_ITEM + 1;

    public static final int COMMAND_UPDATE_LIST = COMMAND_APPEND_LIST + 1;

    public static final int COMMAND_UPDATE_BOOK = COMMAND_UPDATE_LIST + 1;

    public static final int COMMAND_UPDATE_AUTHED_LIST = COMMAND_UPDATE_BOOK + 1;

    public static final int COMMAND_UPDATE_AUTHED_LIST_AND_START_PLAYER = COMMAND_UPDATE_AUTHED_LIST + 1;

    public static final int COMMAND_DELETE_ITEM = COMMAND_UPDATE_AUTHED_LIST_AND_START_PLAYER + 1;

    public static final int COMMAND_DESTROY_SERVICE = COMMAND_DELETE_ITEM + 1;

    public static final int COMMAND_REQUIRE_NOTIFICATION_CONTROLLER = COMMAND_DESTROY_SERVICE + 1;

    public static final int COMMAND_RETURN_NOTIFICATION_CONTROLLER = COMMAND_REQUIRE_NOTIFICATION_CONTROLLER + 1;

    public static final int COMMAND_SHOW_NOTIFICATION = COMMAND_RETURN_NOTIFICATION_CONTROLLER + 1;

    public static final int COMMAND_DISMISS_NOTIFICATION = COMMAND_SHOW_NOTIFICATION + 1;

    public static final int COMMAND_CLEAR_PLAYLIST = COMMAND_DISMISS_NOTIFICATION + 1;

    public static final int COMMAND_JUMP_TO_ITEM = COMMAND_CLEAR_PLAYLIST + 1;

    public static final int COMMAND_UPDATE_TO_ITEM = COMMAND_JUMP_TO_ITEM + 1;

    public static final int COMMAND_JUMP_TO_INDEX = COMMAND_UPDATE_TO_ITEM + 1;

    public static final int COMMAND_UPDATE_POSITION_TO_ITEM = COMMAND_JUMP_TO_INDEX + 1;

    public static final int COMMAND_STOP_SERVICE_WITHOUT_SAVE = COMMAND_UPDATE_POSITION_TO_ITEM + 1;

    public static final int COMMAND_AUTO_PLAYING_STATUS_INTERRUPTED = COMMAND_STOP_SERVICE_WITHOUT_SAVE + 1;

    public static final int COMMAND_RESET_STEP_COUNT = COMMAND_AUTO_PLAYING_STATUS_INTERRUPTED + 1;

    /**
     * 更新播放器启动来源
     */
    public static final int COMMAND_UPDATE_START_PLAYER_SOURCE = COMMAND_RESET_STEP_COUNT + 1;

    public static final int MESSAGE_UPDATE_PLAYING_PROGRESS = COMMAND_UPDATE_START_PLAYER_SOURCE + 1;

    public static final int MESSAGE_UPDATE_STEP_FREQUENCY = MESSAGE_UPDATE_PLAYING_PROGRESS + 1;

    public static final int MESSAGE_UPDATE_STEP_COUNT = MESSAGE_UPDATE_STEP_FREQUENCY + 1;

    public static final int MESSAGE_NOT_WIFI = MESSAGE_UPDATE_STEP_COUNT + 1;

    public static final int MESSAGE_NO_NETWORK_AVAILABLE = MESSAGE_NOT_WIFI + 1;

    public static final int MESSAGE_FILE_NOT_EXIST = MESSAGE_NO_NETWORK_AVAILABLE + 1;

    public static final int MESSAGE_URI_IS_EMPTY = MESSAGE_FILE_NOT_EXIST + 1;

    public static final int MESSAGE_NEED_ORDER = MESSAGE_URI_IS_EMPTY + 1;

    // public static final int STATUS_PLAYING = MESSAGE_NOT_WIFI + 1;

    // public static final int STATUS_PAUSED = STATUS_PLAYING + 1;

    public static final int STATUS_INVALID = MESSAGE_NEED_ORDER + 1;

    public static final int STATUS_IDLE = STATUS_INVALID + 1;

    public static final int STATUS_INITIALIZED = STATUS_IDLE + 1;

    public static final int STATUS_PREPARED = STATUS_INITIALIZED + 1;

    public static final int STATUS_STARTED = STATUS_PREPARED + 1;

    public static final int STATUS_PAUSED = STATUS_STARTED + 1;

    public static final int STATUS_PLACKBACK_COMPLETED = STATUS_PAUSED + 1;

    public static final int STATUS_STOPPED = STATUS_PLACKBACK_COMPLETED + 1;

    public static final int STATUS_ERROR = STATUS_STOPPED + 1;

    // stat:
    // {Idle, Initialized, Prepared, Started, Paused, Stopped,
    // PlaybackCompleted, Error}

    public static final int TYPE_URL = STATUS_ERROR + 1;

    public static final int TYPE_LOCAL = TYPE_URL + 1;

    /**
     * 无效时长
     */
    public static final int ENVALID_TIME_DURATION = -1;

    /**
     * 播放器准备状态，准备前
     */
    public static final int PREPARING_STATUS_PRE = TYPE_LOCAL + 1;

    /**
     * 播放器准备状态，准备完毕
     */
    public static final int PREPARING_STATUS_DONE = PREPARING_STATUS_PRE + 1;

    public static final int MESSAGE_ON_STOP = 1;

    public static final int MESSAGE_ON_START = MESSAGE_ON_STOP + 1;

    public static final int MESSAGE_ON_SEEK_TO = MESSAGE_ON_START + 1;

    public static final int MESSAGE_ON_PAUSE = MESSAGE_ON_SEEK_TO + 1;

    public static final int MESSAGE_ON_COMPLETION = MESSAGE_ON_PAUSE + 1;

    public static final int MESSAGE_ON_PRE = MESSAGE_ON_COMPLETION + 1;

    public static final int MESSAGE_ON_NEXT = MESSAGE_ON_PRE + 1;

    public static final int MESSAGE_ON_PRE_PLAYING = MESSAGE_ON_NEXT + 1;

    public static final int MESSAGE_ON_START_PLAYING = MESSAGE_ON_PRE_PLAYING + 1;

    public static final int MESSAGE_ON_UPDATE_PLAYING_PROGRESS = MESSAGE_ON_START_PLAYING + 1;

    public static final int MESSAGE_ON_SHOW_MESSAGE = MESSAGE_ON_UPDATE_PLAYING_PROGRESS + 1;

    public static final int MESSAGE_ON_PREPARED = MESSAGE_ON_SHOW_MESSAGE + 1;

    public static final int MESSAGE_ON_BUFFERING_UPDATE = MESSAGE_ON_PREPARED + 1;

    public static final int MESSAGE_ON_VIDEO_SIZE_CHANGED = MESSAGE_ON_BUFFERING_UPDATE + 1;

    public static final int MESSAGE_ON_SEEK_COMPLETE = MESSAGE_ON_VIDEO_SIZE_CHANGED + 1;

    public static final int MESSAGE_ON_INFO = MESSAGE_ON_SEEK_COMPLETE + 1;

    public static final int MESSAGE_ON_ERROR = MESSAGE_ON_INFO + 1;

    public static final int MESSAGE_ON_PLAYERLIST_CHANGE = MESSAGE_ON_ERROR + 1;

    public static final int MESSAGE_ON_SAVE_PLAYING_PROGRESS = MESSAGE_ON_PLAYERLIST_CHANGE + 1;

    public static final int MESSAGE_SET_LISTENER = MESSAGE_ON_SAVE_PLAYING_PROGRESS + 1;


    /**
     * 重新设置PlayListener的时间间隔
     */
    public static final int SET_LISTENER_STEP = 1000;

    public static final int BOOLEAN_TRUE = 1;

    public static final int BOOLEAN_FALSE = -1;

    /**
     * 控制在点击播放时是否启动下载
     */
    public static final boolean START_DOWNLOAD_WHEN_START_PLAYER = false;

    /**
     * 控制是否记住当前播放列表中每一章的播放进度
     */
    public static final boolean REMEMBER_EVERY_CHAPTER_PLAYING_POSITION = false;
}
