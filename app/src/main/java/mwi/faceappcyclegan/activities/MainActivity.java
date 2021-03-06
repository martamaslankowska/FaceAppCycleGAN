package mwi.faceappcyclegan.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.transition.ArcMotion;
import android.support.transition.ChangeBounds;
import android.support.transition.Transition;
import android.support.transition.TransitionManager;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import mwi.faceappcyclegan.R;
import mwi.faceappcyclegan.utils.ImageLoader;

import static mwi.faceappcyclegan.utils.ImageLoader.compressImage;
import static mwi.faceappcyclegan.utils.ImageLoader.getPath;
import static mwi.faceappcyclegan.utils.ImageLoader.rotateImage;

public class MainActivity extends Activity {

    private int REQUEST_CAMERA = 0, SELECT_FILE = 1;
    private Button btnSelect;
    private ImageView happyImageView, neutralImageView, emptyImageView;
    private String userChoosenTask;
    private Uri photoUri;
    Bitmap bitmap;
    ViewGroup transitionsContainer;

    boolean returnAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        View view = findViewById(android.R.id.content);
        transitionsContainer = (ViewGroup) view.findViewById(R.id.mainLayout);

        btnSelect = findViewById(R.id.btnSelectPhoto);
        btnSelect.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                selectImage();
            }
        });

        happyImageView = findViewById(R.id.happyImageView);
        neutralImageView = findViewById(R.id.neutralImageView);
        emptyImageView = findViewById(R.id.emptyImageView);

        emptyImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                returnAnimation = !returnAnimation;
                happyImageView.setImageResource(returnAnimation ? R.drawable.neutral_emoji : R.drawable.happy_emoji);
                neutralImageView.setImageResource(returnAnimation ? R.drawable.happy_emoji : R.drawable.neutral_emoji);
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case ImageLoader.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(userChoosenTask.equals(getString(R.string.take_photo)))
                        cameraIntent();
                    else if(userChoosenTask.equals(getString(R.string.choose_gallery)))
                        galleryIntent();
                } else {
                    //code for deny
                }
                break;
        }
    }

    private void selectImage() {
        final CharSequence[] items = { getString(R.string.take_photo), getString(R.string.choose_gallery)};

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(getString(R.string.select_title));
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                boolean result = ImageLoader.checkPermission(MainActivity.this);
                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(builder.build());

                if (items[item].equals(getString(R.string.take_photo))) {
                    userChoosenTask = getString(R.string.take_photo);
                    if(result)
                        cameraIntent();

                } else if (items[item].equals(getString(R.string.choose_gallery))) {
                    userChoosenTask = getString(R.string.choose_gallery);
                    if (result)
                        galleryIntent();
                }
            }
        });
        builder.show();
    }

    private void galleryIntent()
    {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);//
        startActivityForResult(Intent.createChooser(intent, "Select File"),SELECT_FILE);
    }

    private Uri getUriFromFilePath(Context context, String path) {
        File file = new File(path);
        Uri uri;
        try {
            uri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".uri", file);
        } catch (Exception e) {
            uri = Uri.fromFile(file);
        }
        return uri;
    }

    private void cameraIntent()
    {
        String photoPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/${System.currentTimeMillis()}.png";
        photoUri = getUriFromFilePath(this, photoPath);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivityForResult(intent, REQUEST_CAMERA);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SELECT_FILE)
                onSelectFromGalleryResult(data);
            else if (requestCode == REQUEST_CAMERA)
                onCaptureImageResult();

//            bitmap = compressImage(photoUri.getPath());

            Intent anotherIntent = new Intent(this, DetectionActivity.class);
            anotherIntent.putExtra("photoUri", photoUri);
            anotherIntent.putExtra("whichDirection", returnAnimation);
            startActivity(anotherIntent);
//            finish();

        }
    }

    private void onCaptureImageResult() {
        String filePath = getPath(this, photoUri);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        bitmap = BitmapFactory.decodeFile(filePath, options);
    }

    @SuppressWarnings("deprecation")
    private void onSelectFromGalleryResult(Intent data) {
        if (data != null) {
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), data.getData());
                photoUri = data.getData();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
