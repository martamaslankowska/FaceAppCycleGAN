package mwi.faceappcyclegan.activities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import org.tensorflow.Graph;
import org.tensorflow.Operation;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import mwi.faceappcyclegan.R;

import static mwi.faceappcyclegan.utils.ImageLoader.compressImage;
import static mwi.faceappcyclegan.utils.ImageLoader.correctBoundingBox;
import static mwi.faceappcyclegan.utils.ImageLoader.getPath;
import static mwi.faceappcyclegan.utils.ImageLoader.rotateImage;
import static mwi.faceappcyclegan.utils.ImageLoader.toGrayscale;
import static mwi.faceappcyclegan.utils.ImageTransformer.IMAGE_INPUT_SIZE;
import static mwi.faceappcyclegan.utils.ImageTransformer.initTensorflow;
import static mwi.faceappcyclegan.utils.ImageTransformer.preprocessImageToNormalizedFloats;
import static mwi.faceappcyclegan.utils.ImageTransformer.produceFakeImage;


public class DetectionActivity extends AppCompatActivity {

    Bitmap bitmap, grayCroppedBitmap = null;
    Uri imageUri;
    ImageView imageView, realFaceImageView, fakeFaceImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection);

        Bundle bundle = getIntent().getExtras();
        String photoPath = String.valueOf(bundle.get("photoUri"));
        imageUri = Uri.parse(photoPath);

        try {
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            String filePath = getPath(this, imageUri);
            bitmap = compressImage(filePath);
            bitmap = rotateImage(bitmap, filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        FirebaseApp.initializeApp(this);

        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setModeType(FirebaseVisionFaceDetectorOptions.ACCURATE_MODE)
                        .setLandmarkType(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                        .setClassificationType(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .setMinFaceSize(0.05f)
                        .setTrackingEnabled(true)
                        .build();

        final Context context = getApplicationContext();
        imageView = findViewById(R.id.photoImageView);
        realFaceImageView = findViewById(R.id.realFaceImageView);
        fakeFaceImageView = findViewById(R.id.fakeFaceImageView);

        imageView.setImageBitmap(bitmap);


        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionFaceDetector detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(options);



        detector.detectInImage(image).addOnSuccessListener(
                new OnSuccessListener<List<FirebaseVisionFace>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionFace> firebaseVisionFaces) {
                        Toast.makeText(context, "SUCCESS ^.^  --> " + String.valueOf(firebaseVisionFaces.size() + " face(s)"), Toast.LENGTH_SHORT).show();

                        loadRealFace(firebaseVisionFaces);

                        if (grayCroppedBitmap != null) {
                            Bitmap face = Bitmap.createScaledBitmap(grayCroppedBitmap, IMAGE_INPUT_SIZE, IMAGE_INPUT_SIZE, false);
                            float[] tensorflowInput = preprocessImageToNormalizedFloats(face);

                            float[] tensorflowOutput = produceFakeFace(tensorflowInput);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Task failed with an exception
                        Toast.makeText(context, "ŹLEEE....." + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

    }

    private FirebaseVisionFaceDetectorOptions faceDetectorOptions(float minFaceSize, boolean trackingEnabled) {
        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setModeType(FirebaseVisionFaceDetectorOptions.ACCURATE_MODE)
                        .setLandmarkType(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                        .setClassificationType(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .setMinFaceSize(minFaceSize)
                        .setTrackingEnabled(trackingEnabled)
                        .build();

        return options;
    }


    public void loadRealFace(List<FirebaseVisionFace> firebaseVisionFaces) {
        Bitmap facesWithBoundingBox = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(facesWithBoundingBox);

        for (int i=0; i<firebaseVisionFaces.size(); i++) {
            FirebaseVisionFace face = firebaseVisionFaces.get(i);

            Rect rect = face.getBoundingBox();

            Paint paint = new Paint();
            paint.setColor(Color.GREEN);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5);

            canvas.drawRect(rect, paint);


            rect = correctBoundingBox(rect, bitmap.getHeight());

            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5);

            canvas.drawRect(rect, paint);

            if (i == 0) {
                Bitmap croppedFace = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                croppedFace = Bitmap.createBitmap(croppedFace, rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top);
                realFaceImageView.setImageBitmap(toGrayscale(croppedFace));
                fakeFaceImageView.setImageBitmap(croppedFace);

                grayCroppedBitmap = toGrayscale(croppedFace);
                preprocessImageToNormalizedFloats(grayCroppedBitmap);
            }
        }
        imageView.setImageBitmap(facesWithBoundingBox);
    }

    public float[] produceFakeFace(float[] input) {
//        AssetManager assetManager = getAssets();
//        try {
//            InputStream g = assetManager.open("kozaczix.pb");
//
//            // We guarantee that the available method returns the total
//            // size of the asset...  of course, this does mean that a single
//            // asset can't be more than 2 gigs.
//            int size = g.available();
//
//            // Read the entire asset into a local byte buffer.
//            byte[] buffer = new byte[size];
//            g.read(buffer);
//            g.close();
//
//            // Convert the buffer into a string.
//            String text = new String(buffer);
//            Log.d("MWI debugging", text);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//
////        try {
////            String[] imgPath = assetManager.list("");
////            Toast.makeText(this, "....", Toast.LENGTH_SHORT).show();
////        } catch (IOException e) {
////            e.printStackTrace();
////        }
        /** One time initialization: */
        TensorFlowInferenceInterface tensorflow = initTensorflow(getAssets());

        Graph g = tensorflow.graph();
        Iterator<Operation> it = g.operations();
        while (it.hasNext()) {
            String name = it.next().name();
            Log.v("MWI graph operations", name);
        }

        float[] output = produceFakeImage(tensorflow, input);
        return output;
    }

}
