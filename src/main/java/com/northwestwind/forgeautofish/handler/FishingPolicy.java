// New component for the derivative of NorthWestWind AutoFish for Forge 1.12.2.
// Modified by allenk416, 2026-10-09; see NOTICE.md for the modification notice.
// See NOTICE.md and LICENSE for provenance and license information.
package com.northwestwind.forgeautofish.handler;

/** Time-based policy with no Minecraft dependency; all times are monotonic milliseconds. */
public final class FishingPolicy {
    public enum Action { NONE, CAST, REEL }
    private long startupDelay, retryDelay, recastDelay, maxWait, dryWait;
    private boolean readyLastTick;
    private long castAllowedAt, reelAllowedAt, hookSince, observedHook = Long.MIN_VALUE;
    private long handledHook = Long.MIN_VALUE;

    public FishingPolicy(long startupDelay, long retryDelay, long recastDelay, long maxWait, long dryWait) {
        configure(startupDelay,retryDelay,recastDelay,maxWait,dryWait);
    }

    public void configure(long startupDelay, long retryDelay, long recastDelay, long maxWait, long dryWait) {
        this.startupDelay=startupDelay; this.retryDelay=retryDelay; this.recastDelay=recastDelay;
        this.maxWait=maxWait; this.dryWait=dryWait;
    }

    public void reset() { readyLastTick=false; castAllowedAt=0; reelAllowedAt=0; observedHook=Long.MIN_VALUE; handledHook=Long.MIN_VALUE; }

    public void actionFailed(long now, Action action) {
        if(action==Action.REEL) { handledHook=Long.MIN_VALUE; reelAllowedAt=now+250; }
    }

    public Action tick(long now, boolean ready, long hookId, boolean bite, boolean inWater) {
        if (!ready) { readyLastTick=false; return Action.NONE; }
        if (!readyLastTick) {
            readyLastTick=true;
            castAllowedAt=Math.max(castAllowedAt,now+startupDelay);
        }
        if (hookId!=observedHook) {
            observedHook=hookId; hookSince=now; handledHook=Long.MIN_VALUE; reelAllowedAt=now;
        }
        // Cast/retry timers must NEVER mask a bite on a new, fast-enchanted hook.
        if (hookId==Long.MIN_VALUE) {
            if(now<castAllowedAt) return Action.NONE;
            castAllowedAt=now+retryDelay; return Action.CAST;
        }
        if(handledHook==hookId || now<reelAllowedAt) return Action.NONE;
        long age=now-hookSince;
        if (bite || (inWater ? age>=maxWait : age>=dryWait)) {
            handledHook=hookId; castAllowedAt=now+recastDelay;
            return Action.REEL;
        }
        return Action.NONE;
    }
}
