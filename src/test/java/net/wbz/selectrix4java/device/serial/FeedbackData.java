package net.wbz.selectrix4java.device.serial;

/**
 * @author Daniel Tuerk
 */
class FeedbackData {

    int blockNumber;
    boolean enter;
    int train;
    boolean forward;

    FeedbackData(int blockNumber, boolean enter, int train, boolean forward) {
        this.blockNumber = blockNumber;
        this.enter = enter;
        this.train = train;
        this.forward = forward;
    }

    int getBlockNumber() {
        return blockNumber;
    }

    void setBlockNumber(int blockNumber) {
        this.blockNumber = blockNumber;
    }

    public boolean isEnter() {
        return enter;
    }

    public void setEnter(boolean enter) {
        this.enter = enter;
    }

    public int getTrain() {
        return train;
    }

    public void setTrain(int train) {
        this.train = train;
    }

    public boolean isForward() {
        return forward;
    }

    public void setForward(boolean forward) {
        this.forward = forward;
    }
}
