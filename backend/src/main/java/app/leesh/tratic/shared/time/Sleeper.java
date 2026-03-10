package app.leesh.tratic.shared.time;

import java.time.Duration;

public interface Sleeper {
    void sleep(Duration duration) throws InterruptedException;
}
