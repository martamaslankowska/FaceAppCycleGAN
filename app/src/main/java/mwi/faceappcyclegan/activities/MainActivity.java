package mwi.faceappcyclegan.activities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;

import java.util.List;

import mwi.faceappcyclegan.R;

public class MainActivity extends AppCompatActivity {

    Bitmap facesBitmap;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        imageView = findViewById(R.id.imageView);
        final ImageView boundingBoxImageView = findViewById(R.id.boundingBoxImageView);

        Drawable drawable = context.getResources().getDrawable(R.drawable.harry);
        facesBitmap = ((BitmapDrawable)drawable).getBitmap();

        imageView.setImageBitmap(facesBitmap);

        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(facesBitmap);
        FirebaseVisionFaceDetector detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(options);


//        try {
//            Thread.sleep(20000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        detector.detectInImage(image).addOnSuccessListener(
                new OnSuccessListener<List<FirebaseVisionFace>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionFace> firebaseVisionFaces) {
                        Toast.makeText(context, "SUCCESS ^.^  --> " + String.valueOf(firebaseVisionFaces.size() + " faces"), Toast.LENGTH_SHORT).show();

                        Bitmap facesWithBoundingBox = facesBitmap.copy(Bitmap.Config.ARGB_8888, true);
                        Canvas canvas = new Canvas(facesWithBoundingBox);

                        for (int i=0; i<firebaseVisionFaces.size(); i++) {
                            FirebaseVisionFace face = firebaseVisionFaces.get(i);
                            Rect rect = face.getBoundingBox();

                            Paint paint = new Paint();
                            paint.setColor(Color.GREEN);
                            paint.setStyle(Paint.Style.STROKE);
                            paint.setStrokeWidth(10);

                            canvas.drawRect(rect, paint);

                            Bitmap croppedFace = facesBitmap.copy(Bitmap.Config.ARGB_8888, true);
                            croppedFace = Bitmap.createBitmap(croppedFace, rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top);
                            boundingBoxImageView.setImageBitmap(croppedFace);
                        }
                        imageView.setImageBitmap(facesWithBoundingBox);

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Task failed with an exception
                        // ...
                        Toast.makeText(context, "ŹLEEE....." + e.getMessage(), Toast.LENGTH_LONG).show();

                    }
                });



//        Task<List<FirebaseVisionFace>> result =
//                detector.detectInImage(image)
//                        .addOnSuccessListener(
//                                new OnSuccessListener<List<FirebaseVisionFace>>() {
//                                    @Override
//                                    public void onSuccess(List<FirebaseVisionFace> faces) {
//                                        // Task completed successfully
//                                        // ...
//
//                                        Toast.makeText(context, "SUKCES ^.^", Toast.LENGTH_SHORT).show();
//
//
//                                        for (FirebaseVisionFace face : faces) {
//                                            Rect bounds = face.getBoundingBox();
//                                            float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
//                                            float rotZ = face.getHeadEulerAngleZ();  // Head is tilted sideways rotZ degrees
//
//                                            Toast.makeText(context, bounds.centerX() + ", " + bounds.centerY(), Toast.LENGTH_SHORT).show();
//
//                                            // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
//                                            // nose available):
//                                            FirebaseVisionFaceLandmark leftEar = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR);
//                                            if (leftEar != null) {
//                                                FirebaseVisionPoint leftEarPos = leftEar.getPosition();
//                                            }
//
//                                            // If classification was enabled:
//                                            if (face.getSmilingProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
//                                                float smileProb = face.getSmilingProbability();
//                                            }
//                                            if (face.getRightEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
//                                                float rightEyeOpenProb = face.getRightEyeOpenProbability();
//                                            }
//
//                                            // If face tracking was enabled:
//                                            if (face.getTrackingId() != FirebaseVisionFace.INVALID_ID) {
//                                                int id = face.getTrackingId();
//                                            }
//                                        }
//
//                                    }
//                                })
//                        .addOnFailureListener(
//                                new OnFailureListener() {
//                                    @Override
//                                    public void onFailure(@NonNull Exception e) {
//                                        // Task failed with an exception
//                                        // ...
//
//                                        Toast.makeText(context, "PORAŻKA... -.-", Toast.LENGTH_SHORT).show();
//
//                                    }
//                                });
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

}
