package net.wbz.selectrix4java.device.serial;

import org.junit.Assert;

/**
 * @author Daniel Tuerk
 */
public class BaseFeedbackDataTest extends BaseFeedbackTest<FeedbackData> {

    public BaseFeedbackDataTest(String recordFilePath, int playbackSpeed) {
        super(recordFilePath, playbackSpeed);
    }

    private static void assertEquals(FeedbackData data, String field, Object expected, Object actual) {
        Assert.assertEquals(String.format("wrong %s (%s)", field, data), expected, actual);
    }

    protected synchronized void check(boolean enter, int blockNumber, int train, boolean forward) {
        try {
            FeedbackData data = nextFromQueue();

            Assert.assertNotNull("no more data in queue", data);

            assertEquals(data, "block", data.blockNumber, blockNumber);
            assertEquals(data, "state", data.enter, enter);
            assertEquals(data, "train", data.train, train);
            assertEquals(data, "direction", data.forward, forward);
        } catch (AssertionError e) {
            add(e);
        }
    }
}
