package com.ankitgupta.worldvision;

//Importing Necessary Libraries
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.ArrayList;
import android.content.res.AssetManager;
import android.annotation.SuppressLint;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import android.content.res.AssetFileDescriptor;
import java.nio.ByteOrder;
import java.util.List;
import android.graphics.Bitmap;
import org.tensorflow.lite.Interpreter;


public class TensorFlowImageClassifier implements Classifier {

    private static final int pix = 3;
    private static final float thresh = 0.1f;
    private static final int max = 3;
    private static final int batch = 1;

    private Interpreter inter;
    private int in;
    private List<String> labels;

    private TensorFlowImageClassifier() {

    }

    static Classifier create(AssetManager am, String path, String label, int sizein) throws IOException {

        TensorFlowImageClassifier classifier = new TensorFlowImageClassifier();
        classifier.inter = new Interpreter(classifier.loadModelFile(am, path));
        classifier.labels = classifier.loadLabelList(am, label);
        classifier.in = sizein;

        return classifier;
    }

    @Override
    public List<Recognition> recognizeImage(Bitmap bm) {
        ByteBuffer buff = convertBitmapToByteBuffer(bm);
        byte[][] result = new byte[1][labels.size()];
        inter.run(buff, result);
        return getSortedResult(result);
    }

    @Override
    public void close() {
        inter.close();
        inter = null;
    }

    private MappedByteBuffer loadModelFile(AssetManager manageasset, String path) throws IOException {
        AssetFileDescriptor fileDescriptor = manageasset.openFd(path);
        long start = fileDescriptor.getStartOffset();
        long length = fileDescriptor.getDeclaredLength();
        FileInputStream is = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fc = is.getChannel();
        return fc.map(FileChannel.MapMode.READ_ONLY, start, length);
    }

    private List<String> loadLabelList(AssetManager am, String path) throws IOException {
        List<String> labelhelping = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(am.open(path)));
        String line;
        while ((line = reader.readLine()) != null) { //Reading through the label list
            labelhelping.add(line);
        }
        reader.close();
        return labelhelping;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) { //Image Processing
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(batch * in * in * pix);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[in * in];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < in; ++i) {
            for (int j = 0; j < in; ++j) {
                final int val = intValues[pixel++];
                byteBuffer.put((byte) ((val >> 16) & 0xFF));
                byteBuffer.put((byte) ((val >> 8) & 0xFF));
                byteBuffer.put((byte) (val & 0xFF));
            }
        }
        return byteBuffer;
    }

    @SuppressLint("DefaultLocale")
    private List<Recognition> getSortedResult(byte[][] labelProbArray) {

        PriorityQueue<Recognition> pq =
                new PriorityQueue<>(
                        max,
                        new Comparator<Recognition>() {
                            @Override
                            public int compare(Recognition l, Recognition r) {
                                return Float.compare(r.getConfid(), l.getConfid());
                            }
                        });

        for (int i = 0; i < labels.size(); ++i) {
            float confid = (labelProbArray[0][i] & 0xff) / 255.0f;
            if (confid > thresh) {
                pq.add(new Recognition("" + i,
                        labels.size() > i ? labels.get(i) : "unknown",
                        confid));
            }
        }

        final ArrayList<Recognition> recognize = new ArrayList<>();
        int sizerecog = Math.min(pq.size(), max);
        for (int i = 0; i < sizerecog; ++i) {
            recognize.add(pq.poll());
        }

        return recognize;
    }

}
