package com;

import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import proto.Paxoskv;
import proto.PaxosKVGrpc;

import static com.KVServer.ge;

public class KvClient {

    public static final int AcceptorBasePort = 3333;

    private Paxoskv.Proposer p;

    public enum PaxosAction {
        PREPARE,
        ACCEPT
    }

    public KvClient(Paxoskv.Proposer p) {
        this.p = p;
    }

    public Phase1Response phase1(List<Long> acceptorIds, int quorum) {
        List<Paxoskv.Acceptor> replies = this.rpcToAll(acceptorIds, PaxosAction.PREPARE);
        int ok = 0;
        Paxoskv.BallotNum higherBal = p.getBal();
        Paxoskv.Acceptor maxVoted = Paxoskv.Acceptor.getDefaultInstance();

        for (Paxoskv.Acceptor r : replies) {
            if (!ge(p.getBal(), r.getLastBal())) {
                higherBal = r.getLastBal();
                continue;
            }

            if (ge(r.getVBal(), maxVoted.getVBal())) {
                maxVoted = r;
            }

            ok += 1;

            //达到多数派，Phase1 完成
            if (ok == quorum) {
                return new Phase1Response(maxVoted.getVal());
            }
        }
        //多Proposer并发运行造成冲突，最大ballot number
        return new Phase1Response(higherBal);
    }

    public Paxoskv.BallotNum phase2(List<Long> acceptorIds, int quorum) {
        List<Paxoskv.Acceptor> replies = this.rpcToAll(acceptorIds, PaxosAction.ACCEPT);
        int ok = 0;
        Paxoskv.BallotNum higherBal = p.getBal();
        for (Paxoskv.Acceptor r : replies) {
            if (!ge(p.getBal(), r.getLastBal())) {
                higherBal = r.getLastBal();
                continue;
            }
            ok += 1;
            if (ok == quorum) {
                return null;
            }
        }
        return higherBal;
    }

    /**
     * @param acceptorIds id数组
     * @param action      PREPARE/ACCEPT
     */
    public List<Paxoskv.Acceptor> rpcToAll(List<Long> acceptorIds, PaxosAction action) {
        List<Paxoskv.Acceptor> replies = new ArrayList<>();
        for (Long aid : acceptorIds) {
            String address = String.format("127.0.0.1:%d", AcceptorBasePort + aid);
            ManagedChannelBuilder<?> channelBuilder = Grpc.newChannelBuilder(address, InsecureChannelCredentials.create());
            ManagedChannel channel = channelBuilder.build();
            PaxosKVGrpc.PaxosKVBlockingStub blockingStub = PaxosKVGrpc.newBlockingStub(channel);

            try {
                Paxoskv.Acceptor reply = Paxoskv.Acceptor.newBuilder().build();
                switch (action) {
                    case PREPARE:
                        reply = blockingStub.prepare(this.p);
                        break;
                    case ACCEPT:
                        reply = blockingStub.accept(this.p);
                        break;
                }
                if (reply != null) {
                    replies.add(reply);
                }
            } finally {
                try {
                    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return replies;
    }

    public Paxoskv.Value runPaxos(List<Long> acceptorIds, Paxoskv.Value value) {
        int quorum = acceptorIds.size() / 2 + 1;
        while (true) {

            // 如果 value 对象本身没有设置 Vi64 字段，说明这是一个读请求，
            // 在 Paxos 流程结束后，我们应该返回从多数派学习到的值。
            // 如果设置了 Vi64 字段，说明是写请求，我们提议写入这个值。
            if (!value.hasVi64()) {
                // 这是一个读操作，我们仍然需要走完Paxos流程来学习已经达成共识的值。
                // 将 value 设置为一个默认的空值，而不是 null，以继续执行。
                value = Paxoskv.Value.getDefaultInstance();
            }

            this.p = p.toBuilder().setBal(Paxoskv.BallotNum.newBuilder()).setVal(value).build();
            Phase1Response p1 = phase1(acceptorIds, quorum);
            Paxoskv.Value maxVotedVal = p1.getValue();
            Paxoskv.BallotNum p1BallotNum = p1.getBallotNum();

            if (maxVotedVal == null || maxVotedVal.getSerializedSize() == 0) {
                String format = String.format("Proposer: no voted value seen, propose my value: %d", value.getVi64());
                System.out.println(format);
            } else {
                this.p = p.toBuilder().setVal(maxVotedVal).build();
            }


            Paxoskv.BallotNum higherBal = phase2(acceptorIds, quorum);
            if (higherBal != null) {
                Paxoskv.BallotNum ballotNum = p.getBal().toBuilder().setN(higherBal.getN() + 1).build();
                this.p = p.toBuilder().setBal(ballotNum).build();
                continue;
            }
            return p.getVal();
        }
    }
}
