import com.sysinfo.statistics.Main;
import junit.framework.TestCase;
import org.json.simple.JSONObject;

/**
 * Created by fariakalim on 11/24/16.
 */
public class MyTest extends TestCase {

    public static void testRespond() {
        Main.InfoHandler handler = new Main.InfoHandler();
        JSONObject object = handler.respond();
        System.out.println(object.get("recentLoad"));
    }
}
