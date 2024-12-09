import com.KvClient;
import io.grpc.Server;
import org.junit.jupiter.api.Test;
import proto.Paxoskv;

import java.util.Arrays;
import java.util.List;

import static com.BuildUtils.buildProposer;
import static com.BuildUtils.buildValue;
import static com.KVServer.serveAcceptors;

public class TestSetAndGetByKeyVer {
    @Test
    void test() {
        List<Long> acceptorIds = Arrays.asList(0L, 1L, 2L);
        List<Server> servers = serveAcceptors(acceptorIds);

        Paxoskv.Proposer proposer = buildProposer("foo", 0L, 0L, 2L);
        KvClient client = new KvClient(proposer);
        Paxoskv.Value v = client.runPaxos(acceptorIds, buildValue(5L));
        System.out.println("written v:" + v.getVi64());

        Paxoskv.Proposer proposer1 = buildProposer("foo", 0L, 0L, 2L);
        KvClient client1 = new KvClient(proposer1);
        Paxoskv.Value v1 = client1.runPaxos(acceptorIds, null);
        System.out.println("read v:" + v1.getVi64());

        Paxoskv.Proposer proposer2 = buildProposer("foo", 1L, 0L, 2L);
        KvClient client2 = new KvClient(proposer2);
        Paxoskv.Value v2 = client2.runPaxos(acceptorIds, buildValue(6L));
        System.out.println("written v:" + v2.getVi64());

        Paxoskv.Proposer proposer3 = buildProposer("foo", 1L, 0L, 2L);
        KvClient client3 = new KvClient(proposer3);
        Paxoskv.Value v3 = client3.runPaxos(acceptorIds, null);
        System.out.println("read v:" + v3.getVi64());
        for (Server server : servers) {
            server.shutdown();
        }
    }

}
