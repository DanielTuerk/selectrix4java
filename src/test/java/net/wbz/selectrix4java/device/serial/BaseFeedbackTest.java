package net.wbz.selectrix4java.device.serial;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

/**
 * @author Daniel Tuerk
 */
abstract class BaseFeedbackTest<Event> extends BaseRecordingTest {

    private List<AssertionError> exceptions = new ArrayList<>();

    /**
     * Queue to execute the tasks as FIFO.
     */
    private final Deque<Event> queue = new LinkedBlockingDeque<>();

    public BaseFeedbackTest(String recordFilePath) {
        super(recordFilePath);
    }

    public BaseFeedbackTest(String recordFilePath, int playbackSpeed) {
        super(recordFilePath, playbackSpeed);
    }

    @Before
    public void init() {
        queue.clear();
        initTest();
    }

    @After
    public void afterTest() {
        if (!exceptions.isEmpty()) {
            throw exceptions.iterator().next();
        }
        Assert.assertEquals("data remaining in queue", 0, queue.size());
    }

    protected Event nextFromQueue() {
        return queue.poll();
    }

    protected void initTest() {

    }

    protected void add(AssertionError assertionError) {
        exceptions.add(assertionError);
    }

    protected void appendToQueue(Event event) {
        queue.add(event);
    }
}
