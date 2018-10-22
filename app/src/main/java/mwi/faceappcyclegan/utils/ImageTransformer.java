package mwi.faceappcyclegan.utils;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import static mwi.faceappcyclegan.activities.DetectionActivity.GRAPH_NAME;
import static mwi.faceappcyclegan.activities.DetectionActivity.imageColor;
import static mwi.faceappcyclegan.activities.DetectionActivity.imageColors;

public class ImageTransformer {

    static TensorFlowInferenceInterface tensorflow;
    static final String GRAPH_PATH = "file:///android_asset/" + GRAPH_NAME;
    static final String INPUT_NODE_NAME = "strided_slice:0";  // import/ prefix
    static final String OUTPUT_NODE_NAME = "generatorA2B/Tanh:0";  // import/ prefix
    public static final int IMAGE_INPUT_SIZE = imageColor == imageColors[0] ? 64 : 128;
    public static final int IMAGE_OUTPUT_SIZE = IMAGE_INPUT_SIZE;
    static final int COLOR_CHANNELS = 3;


    /** One time initialization: */
    public static TensorFlowInferenceInterface initTensorflow(AssetManager assets) {
        tensorflow = new TensorFlowInferenceInterface(assets, GRAPH_PATH);
        return tensorflow;
    }


    public static float[] produceFakeImage(TensorFlowInferenceInterface tensorflow, float[] input) {
        float[] output = new float[IMAGE_OUTPUT_SIZE * IMAGE_OUTPUT_SIZE * COLOR_CHANNELS];

        // loading new input
        tensorflow.feed(INPUT_NODE_NAME, input, 1L, IMAGE_INPUT_SIZE, IMAGE_INPUT_SIZE, COLOR_CHANNELS);

        // running inference for given input and reading output
        String[] outputNodes = {OUTPUT_NODE_NAME};
        tensorflow.run(outputNodes);
        tensorflow.fetch(OUTPUT_NODE_NAME, output);

        return output;
    }


    public static float[] preprocessImageToNormalizedFloats(Bitmap bitmap) {
        int[] bitmapPixels = new int [IMAGE_INPUT_SIZE * IMAGE_INPUT_SIZE];
        float[] normalizedPixels = new float[IMAGE_INPUT_SIZE * IMAGE_INPUT_SIZE * COLOR_CHANNELS];

        bitmap.getPixels(bitmapPixels, 0, IMAGE_INPUT_SIZE, 0, 0, IMAGE_INPUT_SIZE, IMAGE_INPUT_SIZE);

        int imageMean = 128;
        float imageStd = 128.0f;
        for (int i=0; i<bitmapPixels.length; i++) {
            int pixel = bitmapPixels[i];
            normalizedPixels[i * 3] = (Color.red(pixel) - imageMean) / imageStd;
            normalizedPixels[i * 3 + 1] = (Color.green(pixel) - imageMean) / imageStd;
            normalizedPixels[i * 3 + 2] = (Color.blue(pixel) - imageMean) / imageStd;
        }
        return normalizedPixels;
    }

    public static Bitmap processImageFromNormalizedFloats(float[] normalizedPixels) {
        int[] bitmapPixels = new int [IMAGE_INPUT_SIZE * IMAGE_INPUT_SIZE];

        int RGB_MASK = 0x00FFFFFF;
        int MASK = 0xFF000000;
        int imageMean = 128;
        float imageStd = 128.0f;
        int R, G, B;
        for (int i=0; i<bitmapPixels.length; i++) {
            R = (int)(normalizedPixels[i * 3] * imageStd) + imageMean;
            G = (int)(normalizedPixels[i * 3 + 1] * imageStd) + imageMean;
            B = (int)(normalizedPixels[i * 3 + 2] * imageStd) + imageMean;
            bitmapPixels[i] = (R << 16) | (G << 8) | B;
            bitmapPixels[i] |= MASK;


//            bitmapPixels[i] *= -1;
//            bitmapPixels[i] ^= RGB_MASK;
        }

        Bitmap fakeImage = Bitmap.createBitmap(bitmapPixels, IMAGE_OUTPUT_SIZE, IMAGE_OUTPUT_SIZE, Bitmap.Config.ARGB_8888);
        return fakeImage;
    }


}
