import com.KvClient;
import io.grpc.Server;
import org.junit.jupiter.api.Test;
import proto.Paxoskv;

import java.util.Arrays;
import java.util.List;

import static com.KVServer.serveAcceptors;

public class TestSetAndGetByKeyVer {
    @Test
    void test() {
        List<Long> acceptorIds = Arrays.asList(0L, 1L, 2L);
        List<Server> servers = serveAcceptors(acceptorIds);
        //初始化proposer
        Paxoskv.PaxosInstanceId id = Paxoskv.PaxosInstanceId.newBuilder().setKey("foo").setVer(0).build();
        Paxoskv.BallotNum.Builder bal = Paxoskv.BallotNum.newBuilder().setN(0).setProposerId(2);
        Paxoskv.Proposer proposer = Paxoskv.Proposer.newBuilder().setId(id).setBal(bal).build();
        //写
        KvClient client = new KvClient(proposer);
        Paxoskv.Value value = Paxoskv.Value.newBuilder().setVi64(5).build();
        Paxoskv.Value v = client.runPaxos(acceptorIds, value);
        System.out.println("v:" + v.getVi64());
        //读
        v = client.runPaxos(acceptorIds, value);
        for (Server server : servers) {
            server.shutdown();
        }
    }
}
