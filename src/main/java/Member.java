import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class Member implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private final String userName;
    private final long userID;
    private boolean isOnline;
    private final AtomicLong points = new AtomicLong(0);

    public Member(String name, long id){
        this.userName = name;
        this.userID = id;
        isOnline = true;

    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public String getUserName() {
        return userName;
    }

    public long getUserID() {
        return userID;
    }

    public void addPoints(long points){
        this.points.addAndGet(points);
    }

    public long getPoints(){
        return points.get();
    }

    public void deductPoints(long points){
        this.points.addAndGet(-points);
    }

    public synchronized void setOnline() {
        synchronized (this) {
            this.isOnline = true;
        }
    }

    public synchronized void setOffline() {
        synchronized (this) {
            this.isOnline = false;
        }
    }
}
