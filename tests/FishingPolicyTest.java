import com.northwestwind.forgeautofish.handler.FishingPolicy;
import static com.northwestwind.forgeautofish.handler.FishingPolicy.Action.*;

public class FishingPolicyTest {
    private static int checks;
    private static FishingPolicy policy() {return new FishingPolicy(5000,5000,2000,180000,20000);}
    private static void eq(Object expected,Object actual,String description) {
        checks++; if(expected!=actual) throw new AssertionError(description+": "+actual+", expected "+expected);
    }
    public static void main(String[] args) {
        FishingPolicy p=policy();
        eq(NONE,p.tick(0,true,Long.MIN_VALUE,false,false),"Login waits");
        eq(NONE,p.tick(4999,true,Long.MIN_VALUE,false,false),"No early cast");
        eq(CAST,p.tick(5000,true,Long.MIN_VALUE,false,false),"Initial cast without manual click");
        eq(NONE,p.tick(9999,true,Long.MIN_VALUE,false,false),"Failed cast throttled");
        eq(CAST,p.tick(10000,true,Long.MIN_VALUE,false,false),"Retry failed cast");
        eq(NONE,p.tick(11000,true,100,false,true),"New hook waits for fish");
        eq(NONE,p.tick(16000,true,100,false,true),"Does not interrupt healthy cast");
        eq(REEL,p.tick(17000,true,100,true,true),"Bite reels");
        eq(NONE,p.tick(18999,true,Long.MIN_VALUE,false,false),"Recast delay");
        eq(CAST,p.tick(19000,true,Long.MIN_VALUE,false,false),"Recast after catch");
        eq(NONE,p.tick(20000,false,Long.MIN_VALUE,false,false),"Disconnect never clicks");
        p.reset();
        eq(NONE,p.tick(320000,false,Long.MIN_VALUE,false,false),"Offline for five minutes");
        eq(NONE,p.tick(321000,true,Long.MIN_VALUE,false,false),"New world waits");
        eq(CAST,p.tick(326000,true,Long.MIN_VALUE,false,false),"Reconnect resumes");
        eq(NONE,p.tick(330000,false,Long.MIN_VALUE,false,false),"No rod/durability guard");
        eq(NONE,p.tick(340000,true,Long.MIN_VALUE,false,false),"Re-equipping waits");
        eq(CAST,p.tick(345000,true,Long.MIN_VALUE,false,false),"Resume after re-equipping");
        p=policy();
        eq(NONE,p.tick(0,true,7,false,true),"Watchdog observes hook");
        eq(NONE,p.tick(179999,true,7,false,true),"No premature timeout");
        eq(REEL,p.tick(180000,true,7,false,true),"Stuck hook reset");
        p=policy(); p.tick(0,true,8,false,false);
        eq(REEL,p.tick(20000,true,8,false,false),"Hook stuck outside water");
        p=policy(); p.tick(0,true,9,false,true);
        eq(REEL,p.tick(10000,true,10,true,true),"Fresh hook bite is not age-gated");
        eq(NONE,p.tick(10001,true,10,true,true),"Same hook is not reeled twice");
        p=new FishingPolicy(5000,5000,100,180000,20000);
        p.tick(0,true,Long.MIN_VALUE,false,false);
        eq(CAST,p.tick(5000,true,Long.MIN_VALUE,false,false),"Fast cycle initial cast");
        eq(REEL,p.tick(5010,true,20,true,true),"Cast retry does not mask first fast bite");
        eq(NONE,p.tick(5109,true,Long.MIN_VALUE,false,false),"100ms recast still respected");
        eq(CAST,p.tick(5110,true,Long.MIN_VALUE,false,false),"100ms recast");
        eq(REEL,p.tick(5120,true,21,true,true),"Consecutive new hook catches immediately");
        p.actionFailed(5120,REEL);
        eq(NONE,p.tick(5369,true,21,true,true),"Rejected item use is throttled");
        eq(REEL,p.tick(5370,true,21,true,true),"Rejected reel can retry on same hook");
        p=policy();
        eq(REEL,p.tick(0,true,22,true,true),"Existing hook bite not blocked by startup delay");
        p=new FishingPolicy(5000,5000,500,10000,20000);
        p.tick(0,true,30,false,false);
        eq(NONE,p.tick(10000,true,30,false,false),"Water timeout does not override dry-hook timeout");
        eq(NONE,p.tick(19999,true,30,false,false),"Dry hook waits for its separate deadline");
        eq(REEL,p.tick(20000,true,30,false,false),"Dry hook uses configured 20s timeout");
        p=new FishingPolicy(5000,5000,500,10000,20000);
        p.tick(0,true,31,false,true);
        eq(NONE,p.tick(9999,true,31,false,true),"Wet hook waits for 10s timeout");
        eq(REEL,p.tick(10000,true,31,false,true),"Wet hook uses configured 10s timeout");
        System.out.println("PASS: "+checks+" behavior checks");
    }
}
