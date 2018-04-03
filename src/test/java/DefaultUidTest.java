import cn.Jolyne.Bootstrap;
import cn.Jolyne.UidGenerator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import javax.annotation.Resource;

/**
 * Created by zhiguo.liu on 2018/4/3
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Bootstrap.class)
@WebAppConfiguration
public class DefaultUidTest {

    @Resource
    private UidGenerator uidGenerator;

    @Test
    public void test() {
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 10000000; i++) {
            uidGenerator.getUID("");
        }
        System.out.println(System.currentTimeMillis() - begin + "ms");
    }

    @Test
    public void testBigInteger() {
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 10000000; i++) {
            uidGenerator.getUID("");
        }
        System.out.println(System.currentTimeMillis() - begin + "ms");
    }
}
