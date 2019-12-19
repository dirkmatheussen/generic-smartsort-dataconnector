package com.art4l.dataconnector.module.vt100.service.impl.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BenchmarkUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkUtil.class);

    private static final int NS_TO_MS_DIVIDER = 1000000;

    public static Benchmark startBenchmark(final String prefix) {
        final Benchmark benchmark = new Benchmark(prefix);
        benchmark.setStartTime(System.nanoTime());
        return benchmark;
    }

    public static Benchmark startBenchmark() {
        final Benchmark benchmark = new Benchmark();
        benchmark.setStartTime(System.nanoTime());
        return benchmark;
    }

    public static Benchmark stopBenchmark(final Benchmark benchmark) {
        benchmark.setEndTime(System.nanoTime());
//        benchmark.logTimeNanos();
//        benchmark.logTimeMillis();
        benchmark.logTimeCombined();
        return benchmark;
    }

    public static class Benchmark {

        private long startTime;
        private long endTime;
        private long duration = -1;

        private String prefix;

        Benchmark(final String prefix) {
            this.prefix = prefix;
        }

        Benchmark() {}

        void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        void setEndTime(long endTime) {
            this.endTime = endTime;
        }

        long getDurationNanos() {
            if (duration < 0) {
                duration = this.endTime - this.startTime;
            }
            return duration;
        }

        long getDurationMillis() {
            return getDurationNanos() / NS_TO_MS_DIVIDER;
        }

        private String getLogTimeNanos() {
            return String.format("(s: %d ns, e: %d ns) duration: %d ns", this.startTime, this.endTime, this.getDurationNanos());
        }

        private String getLogTimeMillis() {
            return String.format("(s: %d ms, e: %d ms) duration: %d ms", this.startTime / NS_TO_MS_DIVIDER, this.endTime / NS_TO_MS_DIVIDER, this.getDurationMillis());
        }

        public void logTimeNanos() {
            final String output = (this.prefix != null && !this.prefix.isEmpty()) ? this.prefix + " " + "Benchmark " + getLogTimeNanos() : "Benchmark " + getLogTimeNanos();
            LOGGER.info(output);
        }

        public void logTimeMillis() {
            final String output = (this.prefix != null && !this.prefix.isEmpty()) ? this.prefix + " " + "Benchmark " + getLogTimeMillis() :  "Benchmark " + getLogTimeMillis();
            LOGGER.info(output);
        }

        public void logTimeCombined() {
            final String outputNanos = getLogTimeNanos();
            final String outputMillis = getLogTimeMillis();
            LOGGER.info((this.prefix != null && !this.prefix.isEmpty()) ? this.prefix + " " + "Benchmark " + outputNanos + "\t" + outputMillis : "Benchmark " + outputNanos + "\t" + outputMillis);
        }

    }
}
