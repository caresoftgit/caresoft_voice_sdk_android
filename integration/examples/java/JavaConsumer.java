package vn.caresoft.voice.consumer;

import android.content.Context;
import java.util.Collections;
import java.util.List;
import vn.caresoft.voice.*;
import vn.caresoft.voice.ui.VoiceCallView;

/** Compile-only Java integration: backend supplies tokens asynchronously; no embedded secrets. */
public final class JavaConsumer {
    public CaresoftVoiceClient createClient(Context context, String domain, String appId, String api,
            String installationId, CallbackTokenProvider backend) {
        return new CaresoftVoiceClient(context, new VoiceOptions(domain, appId, api,
                installationId, TokenProviders.fromCallback(backend), Collections.emptyList(), "1",
                new AppPushConfig(context.getPackageName(), "default")));
    }
    public void connect(CaresoftVoiceClient client, VoiceCallback<kotlin.Unit> callback) { client.connect(callback); }
    public void callingLines(CaresoftVoiceClient client, VoiceCallback<List<CallingLine>> callback) { client.getCallingLines(callback); }
    public VoiceCall create(CaresoftVoiceClient client, String number, VoiceCallView view) {
        VoiceCall call = client.createCall(new CallOptions(number));
        view.bind(call);
        return call;
    }
    public void start(VoiceCall call, VoiceCallback<kotlin.Unit> callback) { call.start(callback); }
    public void answer(VoiceCall call, VoiceCallback<kotlin.Unit> callback) { call.answer(callback); }
    public void mute(VoiceCall call, boolean value, VoiceCallback<kotlin.Unit> callback) { call.setMuted(value, callback); }
    public void hold(VoiceCall call, boolean value, VoiceCallback<kotlin.Unit> callback) { call.setHeld(value, callback); }
    public void dtmf(VoiceCall call, String digits, VoiceCallback<kotlin.Unit> callback) { call.sendDTMF(digits, callback); }
    public void routes(VoiceCall call, VoiceCallback<List<AudioRoute>> callback) { call.getAudioRoutes(callback); }
    public void route(VoiceCall call, String id, VoiceCallback<kotlin.Unit> callback) { call.setAudioRoute(id, callback); }
    public void stats(VoiceCall call, VoiceCallback<CallStats> callback) { call.getStats(callback); }
    public void reject(VoiceCall call, VoiceCallback<EndResult> callback) { call.reject(callback); }
    public void hangup(VoiceCall call, VoiceCallback<EndResult> callback) { call.hangup(callback); }
    public void readiness(CaresoftVoiceClient client, VoiceCallback<Readiness> callback) { client.checkReadiness(callback); }
    public void available(CaresoftVoiceClient client, VoiceCallback<kotlin.Unit> callback) { client.setAvailability(Availability.AVAILABLE, callback); }
    public void mobile(CaresoftVoiceClient client, VoiceCallback<kotlin.Unit> callback) { client.selectCallDevice(CallDevice.MOBILE, callback); }
    public void register(CaresoftVoiceClient client, PushRegistration registration, VoiceCallback<kotlin.Unit> callback) { client.registerPushToken(registration, callback); }
    public void logout(CaresoftVoiceClient client, VoiceCallback<LogoutResult> callback) { client.logout(callback); }
    public void dispose(CaresoftVoiceClient client, VoiceCallback<kotlin.Unit> callback) { client.dispose(callback); }
    public void detach(VoiceCallView view) { view.unbind(); }
}
