package ru.dmkuranov.aspects_util.utils.temporal.quantedInterval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QuantedIntervalSnapshot {
    private long quantMs;
    private long[] quantCountArray;
    private long[] quantCountArraySorted;
    private int currentQuantIndex;
    private long currentQuantLastMs;

    QuantedIntervalSnapshot(long quantMs, long[] quantCountArray, int currentQuantIndex, long currentQuantLastMs) {
        this.quantMs = quantMs;
        this.quantCountArray = quantCountArray;
        quantCountArraySorted = Arrays.copyOf(quantCountArray, quantCountArray.length);
        Arrays.sort(quantCountArraySorted);
        this.currentQuantIndex = currentQuantIndex;
        this.currentQuantLastMs = currentQuantLastMs;
    }

    public long getTotal() {
        long sum = 0;
        for (int i = 0; i < quantCountArraySorted.length; i++) {
            sum += quantCountArraySorted[i];
        }
        return sum;
    }

    public long getMin() {
        return quantCountArraySorted[0];
    }

    public long getMax() {
        if (quantCountArraySorted.length != 0) {
            return quantCountArraySorted[quantCountArraySorted.length - 1];
        } else {
            return 0;
        }
    }

    public long getAverage() {
        return quantCountArraySorted.length != 0 ? (getTotal() / quantCountArraySorted.length) : 0;
    }

    public long getPercentile50() {
        return getPercentileValue(0.5);
    }

    public long getPercentile95() {
        return getPercentileValue(0.95);
    }

    public long getPercentile99() {
        return getPercentileValue(0.99);
    }

    public long getPercentile995() {
        return getPercentileValue(0.995);
    }

    public long getPercentile999() {
        return getPercentileValue(0.999);
    }

    public long getReversePercentile50() {
        return getPercentileValue(0.5, true);
    }

    public long getReversePercentile95() {
        return getPercentileValue(0.95, true);
    }

    public long getReversePercentile99() {
        return getPercentileValue(0.99, true);
    }

    public long getReversePercentile999() {
        return getPercentileValue(0.999, true);
    }

    public long getReversePercentile995() {
        return getPercentileValue(0.995, true);
    }

    public long getPerSecondAverage() {
        return (getAverage() * 1000) / quantMs;
    }

    public long getPerSecondPercentile95() {
        return (getReversePercentile95() * 1000) / quantMs;
    }

    public long getPerSecondPercentile99() {
        return (getReversePercentile99() * 1000) / quantMs;
    }

    public long getPerSecondPercentile995() {
        return (getReversePercentile995() * 1000) / quantMs;
    }

    public long getPerSecondPercentile999() {
        return (getReversePercentile999() * 1000) / quantMs;
    }

    private int getPercentileIndex(double percentileRatio, boolean reversed) {
        if (quantCountArraySorted.length != 0) {
            if (!reversed) {
                return Double.valueOf(percentileRatio * (quantCountArraySorted.length - 1)).intValue();
            } else {
                return quantCountArraySorted.length - 1 - getPercentileIndex(percentileRatio, false);
            }
        } else {
            return 0;
        }
    }

    private int getPercentileIndex(double percentileRatio) {
        return getPercentileIndex(percentileRatio, false);
    }

    private long getPercentileValue(double percentileRatio, boolean reversed) {
        if (quantCountArraySorted.length != 0) {
            return quantCountArraySorted[getPercentileIndex(percentileRatio, reversed)];
        } else {
            return 0;
        }
    }

    private long getPercentileValue(double percentileRatio) {
        return getPercentileValue(percentileRatio, false);
    }

    public List<QuantEntry> getEntries() {
        List<QuantEntry> entries = new ArrayList<QuantEntry>();
        long currentLastMs = currentQuantLastMs;
        for (int i = currentQuantIndex + 1; i < quantCountArray.length; i++) {
            QuantEntry entry = new QuantEntry(getStartQuantMs(currentLastMs), currentLastMs, quantCountArray[i]);
            entries.add(entry);
            currentLastMs = currentLastMs + quantMs;
        }
        for (int i = 0; i <= currentQuantIndex; i++) {
            QuantEntry entry = new QuantEntry(getStartQuantMs(currentLastMs), currentLastMs, quantCountArray[i]);
            entries.add(entry);
            currentLastMs = currentLastMs + quantMs;
        }
        return entries;
    }

    public static class QuantEntry {
        long firstMs;
        long lastMs;
        long count;

        private QuantEntry(long firstMs, long lastMs, long count) {
            this.firstMs = firstMs;
            this.lastMs = lastMs;
            this.count = count;
        }

        public long getFirstMs() {
            return firstMs;
        }

        public long getLastMs() {
            return lastMs;
        }

        public long getCount() {
            return count;
        }

        @Override
        public String toString() {
            return String.format("%d -> %d = %d", getFirstMs(), getLastMs(), getCount());
        }
    }

    private long getStartQuantMs(long endQuantMs) {
        return endQuantMs - quantMs + 1;
    }

    public long getQuantMs() {
        return quantMs;
    }

    public long getCurrentQuantLastMs() {
        return currentQuantLastMs;
    }
}
