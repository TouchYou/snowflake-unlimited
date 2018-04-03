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
    public void test(){
        String order = uidGenerator.getUID("");
        System.out.println(order);
        System.out.println(uidGenerator.parseUID(order));
    }
}
