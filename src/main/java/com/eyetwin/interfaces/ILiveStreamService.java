package com.eyetwin.interfaces;

import com.eyetwin.entities.LiveStream;
import com.eyetwin.entities.User;

import java.sql.SQLException;
import java.util.List;

public interface ILiveStreamService {

    List<LiveStream> getAvailableStreams() throws SQLException;

    List<LiveStream> getStreamsByCoach(User coach) throws SQLException;

    LiveStream getById(int id) throws SQLException;

    LiveStream createStream(User coach, String title, String description, int coinPrice) throws SQLException;

    boolean startStream(int liveId, User coach) throws SQLException;

    boolean endStream(int liveId, User coach) throws SQLException;

    boolean userHasAccess(User user, LiveStream live) throws SQLException;

    boolean grantPaidAccess(User user, LiveStream live) throws SQLException;

    boolean grantFreeAccess(User user, LiveStream live) throws SQLException;
}
