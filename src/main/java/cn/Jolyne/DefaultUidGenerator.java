package cn.Jolyne;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Represents an implementation of {@link UidGenerator}
 * 基于百度开源项目 uid-generator 的增强版，Snowflake Java 实现版本。项目 Github：https://github.com/baidu/uid-generator
 *
 * uid-generator 通过对 64 位数字分区来生成唯一 ID，由以下组成：
 *
 * -----------------------------------------------------------------------------------
 *  | sign   |  delta seconds  |   worker id   |    sequence
 *  -----------------------------------------------------------------------------------
 *    1bits         28bits          22bits           13bits
 * -----------------------------------------------------------------------------------
 * 其中 delta seconds 为 当前时间 - 指定起始时间。
 * 该版本有三个问题
 * 1. delta seconds 位数有限，28bits 也只能允许运行 8.7 年左右。
 * 2. worker id 生成号码为用后即弃，可容纳重启次数有限。
 * 3. 微服务分布式的情况下，无法使用统一数据源，则不同服务生成 worker id 时会重复
 *
 * 于是做出以下改进
 * 1. worker id 拆分成 data center id，每个服务通过约定指定自己的 data center id 。
 * 2. worker id 通过 redis 自增指定，设计为首尾相连的环形，自增数字达到设定的最大值时，会从0开始。
 * 2. 不限制使用 delta seconds 的位数，则实现了无限时间的使用。当位数增长到 64 为后，改用 BigInteger 的位运算实现。
 *
 * 经测试，BigInteger 实现时，性能降低 60% 左右，每秒发号约为 100w~150w。
 * 现在 uid 由以下组成
 * ---------------------------------------------------------------------------------------------------------
 *  | sign(length < 64)   |  delta seconds (unlimited)  |    data center id   |   worker id   |    sequence
 * ---------------------------------------------------------------------------------------------------------
 *    1bits                         28bits                      22bits             22bits           13bits
 * ---------------------------------------------------------------------------------------------------------
 *  其中 data center id + worker id + sequence 设定的位数不大于 63。
 *
 * 使用注意：
 * 1. 号码的位数不固定，会随着时间增长。data center id + worker id + sequence 总数设定越大，号码位数越长
 * 2. 各个分区的位数、起始时间一旦设定完成投入使用，则后续不能更改。否则会导致发号重复。
 *
 *
 *
 * @author zhiguo.liu
 */
@Component("defaultUidGenerator")
@ConditionalOnProperty(name = "data.center.id")
public class DefaultUidGenerator implements UidGenerator ,InitializingBean{
    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DAY_PATTERN = "yyyy-MM-dd";

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultUidGenerator.class);
    /**
     * Bits allocate
     */
    @Value("${uid.timeBits:28}")
    protected int timeBits;
    @Value("${uid.dataCenterIdBits:9}")
    protected int dataCenterIdBits;
    @Value("${uid.workerBits:13}")
    protected int workerBits;
    @Value("${uid.seqBits:13}")
    protected int seqBits;
    @Value("${data.center.id}")
    protected long dataCenterId;

    /**
     * Customer epoch, unit as second. For example 2018-03-01 (ms: 1463673600000)
     */
    protected String epochStr;
    protected long epochSeconds;

    /**
     * Stable fields after spring bean initializing
     */
    protected BitsAllocator bitsAllocator;
    protected long workerId;

    /**
     * Volatile fields caused by nextId()
     */
    protected long sequence = 0L;
    protected long lastSecond = -1L;

    /**
     * Spring property
     */
    @Resource
    protected DisposableWorkerIdAssigner disposableWorkerIdAssigner;

    public void afterPropertiesSet() throws Exception {
        // initialize bits allocator
        bitsAllocator = new BitsAllocator(timeBits, dataCenterIdBits, workerBits, seqBits);
        // initialize worker id
        workerId = disposableWorkerIdAssigner.assignWorkerId(dataCenterId, bitsAllocator);
        Assert.isTrue(workerId < bitsAllocator.getMaxWorkerId(), "workerId is too big");
        Assert.isTrue(dataCenterId < bitsAllocator.getMaxDataCenterId(), "dataCenterId is too big");
        LOGGER.info("Initialized bits dataCenterBits:{}, workerBits:{}, seqBits:{}", dataCenterIdBits, workerBits, seqBits);
        LOGGER.info("Initialized nodes, workerId:{}, dataCenterId:{}", workerId, dataCenterId);
    }

    @Override
    public String getUID(String preFix) throws UidGenerateException {
        try {
            return nextId(preFix);
        } catch (Exception e) {
            LOGGER.error("Generate unique id exception. ", e);
            throw new UidGenerateException(e);
        }
    }

    @Override
    public String parseUID(String uidStr) {
        BigInteger bigInteger = new BigInteger(uidStr);

        int totalBits = bigInteger.bitLength();
        long dataCenterIdBits = bitsAllocator.getDataCenterIdBits();
        long workerIdBits = bitsAllocator.getWorkerIdBits();
        long sequenceBits = bitsAllocator.getSequenceBits();
        if (totalBits < 64) {
            totalBits = 64;
            long uid = bigInteger.longValue();
            long sequence = (uid << (totalBits - sequenceBits)) >>> (totalBits - sequenceBits);
            long workerId = (uid << (totalBits - workerIdBits - sequenceBits)) >>> (totalBits - workerIdBits);
            long dataCenterId = (uid << (totalBits - dataCenterIdBits - workerIdBits - sequenceBits)) >>> (totalBits - dataCenterIdBits);
            long deltaSeconds = uid >>> (dataCenterIdBits + workerIdBits + sequenceBits);
            Date thatTime = new Date(TimeUnit.SECONDS.toMillis(epochSeconds + deltaSeconds));
            String thatTimeStr = DateFormatUtils.format(thatTime, DATETIME_PATTERN);
            return String.format("{\"UID\":\"%d\",\"timestamp\":\"%s\",\"dataCenterId\":\"%d\",\"workerId\":\"%d\",\"sequence\":\"%d\"}",
                    uid, thatTimeStr, dataCenterId, workerId, sequence);
        } else {
            BigInteger workerBig = getBigIntegerFromLength(workerIdBits).shiftLeft((int) sequenceBits).and(bigInteger);
            System.out.println(workerBig);
            long sequence = getBigIntegerFromLength(sequenceBits).and(bigInteger).longValue();
            long workerId = getBigIntegerFromLength(workerIdBits).and(bigInteger.shiftRight((int)sequenceBits)).longValue();
            long dataCenterId = getBigIntegerFromLength(dataCenterIdBits).and(bigInteger.shiftRight((int)sequenceBits+(int)workerIdBits)).longValue();
            long deltaSeconds = bigInteger.shiftRight((int) dataCenterIdBits + (int) workerIdBits + (int) sequenceBits).longValue();
            Date thatTime = new Date(TimeUnit.SECONDS.toMillis(epochSeconds + deltaSeconds));
            String thatTimeStr = DateFormatUtils.format(thatTime, DATETIME_PATTERN);
            return String.format("{\"UID\":\"%d\",\"timestamp\":\"%s\",\"dataCenterId\":\"%d\",\"workerId\":\"%d\",\"sequence\":\"%d\"}",
                    bigInteger, thatTimeStr, dataCenterId, workerId, sequence);
        }
    }

    private BigInteger getBigIntegerFromLength(long n) {
        return BigInteger.valueOf(-1).shiftLeft((int) n).not();
    }

    /**
     * Get UID
     *
     * @return UID
     * @throws UidGenerateException in the case: Clock moved backwards; Exceeds the max timestamp
     */
    protected synchronized String nextId(String preFix) {
        long currentSecond = getCurrentSecond();

        // Clock moved backwards, wait for newest time
        if (currentSecond < lastSecond) {
            getNextSecond(lastSecond);
        }

        // At the same second, increase sequence
        if (currentSecond == lastSecond) {
            sequence = (sequence + 1) & bitsAllocator.getMaxSequence();
            // Exceed the max sequence, we wait the next second to generate uid
            if (sequence == 0) {
                currentSecond = getNextSecond(lastSecond);
            }
            // At the different second, sequence restart from zero
        } else {
            sequence = 0L;
        }

        lastSecond = currentSecond;

        // 当前时间小于设定的最大时间，即总位数在 64 位以下，用 long 生成数字
        if (currentSecond - epochSeconds <= bitsAllocator.getMaxDeltaSeconds()) {
            return preFix + bitsAllocator.allocate(currentSecond - epochSeconds, dataCenterId, workerId, sequence);
        }
        return preFix + bitsAllocator.allocateBigInteger(currentSecond - epochSeconds, dataCenterId, workerId, sequence);
    }

    /**
     * Get next millisecond
     */
    private long getNextSecond(long lastTimestamp) {
        long timestamp = getCurrentSecond();
        while (timestamp <= lastTimestamp) {
            timestamp = getCurrentSecond();
        }

        return timestamp;
    }

    /**
     * Get current second
     */
    private long getCurrentSecond() {
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
    }

    public void setTimeBits(int timeBits) {
        if (timeBits > 0) {
            this.timeBits = timeBits;
        }
    }

    public void setWorkerBits(int workerBits) {
        if (workerBits > 0) {
            this.workerBits = workerBits;
        }
    }

    public void setSeqBits(int seqBits) {
        if (seqBits > 0) {
            this.seqBits = seqBits;
        }
    }

    @Value("${uid.epochStr:2018-04-01}")
    public void setEpochStr(String epochStr) {
        if (StringUtils.isNotBlank(epochStr)) {
            this.epochStr = epochStr;
            try {
                this.epochSeconds = TimeUnit.MILLISECONDS.toSeconds(DateUtils.parseDate(epochStr, new String[]{DAY_PATTERN}).getTime());
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }
}
