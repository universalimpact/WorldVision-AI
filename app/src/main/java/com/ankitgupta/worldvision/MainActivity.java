package com.ankitgupta.worldvision;

//All Necessary imports
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import com.wonderkiln.camerakit.CameraKitError;
import java.util.concurrent.Executor;
import com.wonderkiln.camerakit.CameraKitImage;
import android.widget.TextView;
import com.wonderkiln.camerakit.CameraKitEventListener;
import android.graphics.Bitmap;
import android.os.Bundle;
import com.wonderkiln.camerakit.CameraKitVideo;
import java.util.concurrent.Executors;
import android.speech.tts.TextToSpeech;
import com.wonderkiln.camerakit.CameraView;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import java.util.List;
import com.wonderkiln.camerakit.CameraKitEvent;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    private static final String pathmodel = "mobilenetmodel.tflite";
    private static final String pathlabel = "possibletags.txt";
    private static final int sizehelper = 224;

    private Classifier classifier;

    private Executor executor = Executors.newSingleThreadExecutor();
    private TextView textres;
    private Button detect, toggle;
    private ImageView imageres;
    private CameraView cam;
    private TextToSpeech t1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cam = findViewById(R.id.viewcam);
        imageres = findViewById(R.id.imageresult);
        textres = findViewById(R.id.viewtext);
        textres.setMovementMethod(new ScrollingMovementMethod());
        toggle = findViewById(R.id.revcambutton);
        detect = findViewById(R.id.detectbutton);

        t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() { //Initialize Text To Speech
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.US);
                }
            }
        });

        cam.addCameraKitListener(new CameraKitEventListener() {
            @Override
            public void onEvent(CameraKitEvent cameraKitEvent) { }

            @Override
            public void onError(CameraKitError cameraKitError) { }

            @Override
            public void onImage(CameraKitImage cameraKitImage) {

                Bitmap bitmap = cameraKitImage.getBitmap();

                bitmap = Bitmap.createScaledBitmap(bitmap, sizehelper, sizehelper, false);

                imageres.setImageBitmap(bitmap);

                //Recognizing, then Outputting
                final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);
                String title = results.get(0).getName();
                String confidence = String.format("(%.1f%%) ", results.get(0).getConfid() * 100.0f);
                String torettext = "Object Name: " + title + ", Confidence: " + confidence;
                textres.setText(torettext);
                String tosay = "This is a " + title + " with " + confidence + "confidence";
                t1.speak(tosay, TextToSpeech.QUEUE_FLUSH, null);
            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) { }
        });

        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cam.toggleFacing();
            }
        });

        detect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cam.captureImage();
            }
        });

        initTensorFlowAndLoadModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cam.start();
    }

    @Override
    protected void onPause() {
        cam.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                classifier.close();
            }
        });
    }

    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier = TensorFlowImageClassifier.create(getAssets(), pathmodel, pathlabel, sizehelper);
                    makeButtonVisible();
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }

    private void makeButtonVisible() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                detect.setVisibility(View.VISIBLE);
            }
        });
    }
}