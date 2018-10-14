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
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
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

import java.io.IOException;
import java.util.List;

import mwi.faceappcyclegan.R;

import static mwi.faceappcyclegan.utils.ImageLoader.compressImage;
import static mwi.faceappcyclegan.utils.ImageLoader.getPath;
import static mwi.faceappcyclegan.utils.ImageLoader.rotateImage;

public class DetectionActivity extends AppCompatActivity {

    Bitmap bitmap = null;
    Uri imageUri;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection);

        Bundle bundle = getIntent().getExtras();
        String photoPath = String.valueOf(bundle.get("photoUri"));
        imageUri = Uri.parse(photoPath);
//        byte[] byteArray = bundle.getByteArray("bitmap");
//        bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);

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
        imageView.setImageBitmap(bitmap);




        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionFaceDetector detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(options);



        detector.detectInImage(image).addOnSuccessListener(
                new OnSuccessListener<List<FirebaseVisionFace>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionFace> firebaseVisionFaces) {
                        Toast.makeText(context, "SUCCESS ^.^  --> " + String.valueOf(firebaseVisionFaces.size() + " faces"), Toast.LENGTH_SHORT).show();

                        Bitmap facesWithBoundingBox = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                        Canvas canvas = new Canvas(facesWithBoundingBox);

                        for (int i=0; i<firebaseVisionFaces.size(); i++) {
                            FirebaseVisionFace face = firebaseVisionFaces.get(i);
                            Rect rect = face.getBoundingBox();

                            Paint paint = new Paint();
                            paint.setColor(Color.GREEN);
                            paint.setStyle(Paint.Style.STROKE);
                            paint.setStrokeWidth(10);

                            canvas.drawRect(rect, paint);

//                            Bitmap croppedFace = bitmap.copy(Bitmap.Config.ARGB_8888, true);
//                            croppedFace = Bitmap.createBitmap(croppedFace, rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top);
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
