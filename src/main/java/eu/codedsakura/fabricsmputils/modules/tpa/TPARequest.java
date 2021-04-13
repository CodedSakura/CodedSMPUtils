package eu.codedsakura.fabricsmputils.modules.tpa;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

class TPARequest {
    ServerPlayerEntity tFrom;
    ServerPlayerEntity tTo;

    ServerPlayerEntity rFrom;
    ServerPlayerEntity rTo;

    boolean tpaHere;
    long timeout;

    Timer timer;

    public TPARequest(ServerPlayerEntity tFrom, ServerPlayerEntity tTo, boolean tpaHere, int timeoutMS) {
        this.tFrom = tFrom;
        this.tTo = tTo;
        this.tpaHere = tpaHere;
        this.timeout = timeoutMS;
        this.rFrom = tpaHere ? tTo : tFrom;
        this.rTo = tpaHere ? tFrom : tTo;
    }

    void setTimeoutCallback(Timeout callback) {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                callback.onTimeout();
            }
        }, timeout);
    }

    void cancelTimeout() {
        timer.cancel();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TPARequest that = (TPARequest) o;
        return tFrom.equals(that.tFrom) &&
                tTo.equals(that.tTo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tFrom, tTo);
    }

    @Override
    public String toString() {
        return "TPARequest{" + "tFrom=" + tFrom +
                ", tTo=" + tTo +
                ", rFrom=" + rFrom +
                ", rTo=" + rTo +
                ", tpaHere=" + tpaHere +
                '}';
    }

    public void refreshPlayers() {
        this.tFrom = tFrom.server.getPlayerManager().getPlayer(tFrom.getUuid());
        this.tTo = tTo.server.getPlayerManager().getPlayer(tTo.getUuid());
        this.rFrom = this.tpaHere ? tTo : tFrom;
        this.rTo = this.tpaHere ? tFrom : tTo;
        assert tFrom != null && tTo != null;
    }
}
