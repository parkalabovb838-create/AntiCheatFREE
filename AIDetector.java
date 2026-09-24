package com.good.anticheat;

import ai.onnxruntime.*;

import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.Collections;

public class AIDetector {

    private OrtEnvironment env;
    private OrtSession sessionLSTM;
    private OrtSession sessionCNN;
    private OrtSession sessionRF;
    private boolean ready = false;

    public AIDetector(Path modelPath) {
        try {
            env = OrtEnvironment.getEnvironment();

            // Три модели для Ensemble
            Path lstmPath = modelPath.getParent().resolve("model_lstm.onnx");
            Path cnnPath = modelPath.getParent().resolve("model_cnn.onnx");
            Path rfPath = modelPath.getParent().resolve("model_rf.onnx");

            if (lstmPath.toFile().exists()) {
                sessionLSTM = env.createSession(lstmPath.toString(), new OrtSession.SessionOptions());
            }
            if (cnnPath.toFile().exists()) {
                sessionCNN = env.createSession(cnnPath.toString(), new OrtSession.SessionOptions());
            }
            if (rfPath.toFile().exists()) {
                sessionRF = env.createSession(rfPath.toString(), new OrtSession.SessionOptions());
            }

            // Fallback — одна модель
            if (sessionLSTM == null && sessionCNN == null && sessionRF == null) {
                sessionLSTM = env.createSession(modelPath.toString(), new OrtSession.SessionOptions());
            }

            ready = true;
        } catch (Exception e) {
            ready = false;
        }
    }

    public boolean isReady() { return ready; }

    public float predict(float[] features) {
        if (!ready) return 0f;

        float lstm = runModel(sessionLSTM, features);
        float cnn = runModel(sessionCNN, features);
        float rf = runModel(sessionRF, features);

        // Голосование: среднее
        int count = 0;
        float sum = 0;
        if (lstm > 0) { sum += lstm; count++; }
        if (cnn > 0) { sum += cnn; count++; }
        if (rf > 0) { sum += rf; count++; }

        return count > 0 ? sum / count : 0f;
    }

    private float runModel(OrtSession session, float[] features) {
        if (session == null) return 0f;
        try {
            long[] shape = {1, features.length};
            OnnxTensor tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(features), shape);
            OrtSession.Result res = session.run(Collections.singletonMap("input", tensor));
            float[][] out = (float[][]) res.get(0).getValue();
            return out[0][0];
        } catch (Exception e) {
            return 0f;
        }
    }
}