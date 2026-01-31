package app.leesh.tratic.shared.time;

import java.time.Duration;

public class ThreadSleeper implements Sleeper {
    @Override
    public void sleep(Duration duration) throws InterruptedException {
        Thread.sleep(duration.toMillis());
    }
}
