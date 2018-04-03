package cn.Jolyne;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component("disposableWorkerIdAssigner")
@ConditionalOnClass(RedisTemplate.class)
public class DisposableWorkerIdAssigner {
    private static final String REDIS_WORK_ID_KEY = "GLOBAL:WORK:ID:";

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * Assign worker id base on Redis.<p>
     * 使用 Redis 的 incr 命令，最后结果为 incr % maxWorkerId
     *
     * @param dataCenterId
     * @param bitsAllocator
     * @return assigned worker id
     */
    public long assignWorkerId(long dataCenterId, BitsAllocator bitsAllocator) {
        return redisTemplate.opsForValue().increment(REDIS_WORK_ID_KEY + dataCenterId, 1) % bitsAllocator.getMaxWorkerId();
    }


}
