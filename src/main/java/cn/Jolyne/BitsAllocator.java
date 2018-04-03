package cn.Jolyne;


import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.util.Assert;

import java.math.BigInteger;

/**
 * Allocate 64 bits for the UID(long)<br>
 * sign (fixed 1bit) -> deltaSecond -> workerId -> sequence(within the same second)
 */
public class BitsAllocator {
    /**
     * Total 64 bits
     * dataCenterIdBits + workerIdBits + sequenceBits
     */
    public static final int TOTAL_BITS = 1 << 6;

    private final int timestampBits;
    private final int dataCenterIdBits;
    private final int workerIdBits;
    private final int sequenceBits;
    /**
     * Max value for dataCenterId & workerId & sequence
     */
    private final long maxDeltaSeconds;
    private final long maxDataCenterId;
    private final long maxWorkerId;
    private final long maxSequence;
    /**
     * Shift for dataCenterId & workerId & sequence
     */
    private final int timestampShift;
    private final int dataCenterIdShift;
    private final int workerIdShift;

    /**
     * Constructor with timestampBits, workerIdBits, sequenceBits<br>
     * The highest bit used for sign, so <code>63</code> bits for timestampBits, workerIdBits, sequenceBits
     */
    public BitsAllocator(int timestampBits, int dataCenterIdBits, int workerIdBits, int sequenceBits) {
        // make sure allocated 64 bits
        int allocateTotalBits = dataCenterIdBits + workerIdBits + sequenceBits;
        Assert.isTrue(allocateTotalBits + 1 < TOTAL_BITS, "allocate greater than 64 bits");

        // initialize bits
        this.timestampBits = timestampBits;
        this.dataCenterIdBits = dataCenterIdBits;
        this.workerIdBits = workerIdBits;
        this.sequenceBits = sequenceBits;

        // initialize max value
        this.maxDeltaSeconds = ~(-1L << timestampBits);
        this.maxDataCenterId = ~(-1L << dataCenterIdBits);
        this.maxWorkerId = ~(-1L << workerIdBits);
        this.maxSequence = ~(-1L << sequenceBits);

        // initialize shift
        this.timestampShift = dataCenterIdBits + workerIdBits + sequenceBits;
        this.dataCenterIdShift = workerIdBits + sequenceBits;
        this.workerIdShift = sequenceBits;
    }

    /**
     * Allocate bits for UID according to delta seconds & workerId & sequence<br>
     *
     * @param deltaSeconds
     * @param workerId
     * @param sequence
     * @return
     */
    public long allocate(long deltaSeconds, long dataCenterId, long workerId, long sequence) {
        return (deltaSeconds << timestampShift) | (dataCenterId << dataCenterIdShift) | (workerId << workerIdShift) | sequence;
    }

    public BigInteger allocateBigInteger(long deltaSeconds, long dataCenterId, long workerId, long sequence) {
        return BigInteger.ZERO.or(BigInteger.valueOf(deltaSeconds).shiftLeft(timestampShift))
                .or(BigInteger.valueOf(dataCenterId).shiftLeft(dataCenterIdShift))
                .or(BigInteger.valueOf(workerId).shiftLeft(workerIdShift))
                .or(BigInteger.valueOf(sequence));
    }

    public int getTimestampBits() {
        return timestampBits;
    }

    public int getDataCenterIdBits() {
        return dataCenterIdBits;
    }

    public int getWorkerIdBits() {
        return workerIdBits;
    }

    public int getSequenceBits() {
        return sequenceBits;
    }

    public long getMaxDeltaSeconds() {
        return maxDeltaSeconds;
    }

    public long getMaxDataCenterId() {
        return maxDataCenterId;
    }

    public long getMaxWorkerId() {
        return maxWorkerId;
    }

    public long getMaxSequence() {
        return maxSequence;
    }

    public int getTimestampShift() {
        return timestampShift;
    }

    public int getWorkerIdShift() {
        return workerIdShift;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}