package com.sh1r0.caffe_android_demo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.sh1r0.caffe_android_lib.CaffeMobile;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;


public class MainActivity extends Activity implements CNNListener, MqttCallback {
    private static String[] IMAGENET_CLASSES = null;
    private static final String LOG_TAG = "MainActivity";
    public static final int MEDIA_TYPE_IMAGE = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 100;
    private static final int REQUEST_IMAGE_SELECT = 200;

    //    MqttAndroidClient mqttAndroidClient;
    MqttAndroidClient mqttAndroidClient;
    private String LOGTAG = "logging";
    private String MQTTHOST = "tcp://demo.thingsboard.io:1883";
    private String PASSWORD = "";
    private String USERNAME = "Ch7zTnHUab5jqdTGqhDd";

    private Bitmap bmp;
    private Button btnCamera;
    private Button btnSelect;
    private CaffeMobile caffeMobile;
    private ProgressDialog dialog;
    private Uri fileUpload;
    private Uri fileUri;
    private ImageView ivCaptured;
    private String linkImage = "";
    private String tenBenh = "";

    File sdcard = Environment.getExternalStorageDirectory();

    String modelDir = (sdcard.getAbsolutePath() + "/caffe_mobile/bvlc_reference_caffenet");
    String modelProto = (modelDir + "/deploy.prototxt");
    String modelBinary = (modelDir + "/plantvillage.caffemodel");

    private FirebaseStorage storage;
    private StorageReference storageReference;
    private TextView tvLabel;

    static {
        System.loadLibrary("caffe");
        System.loadLibrary("caffe_jni");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.storage = FirebaseStorage.getInstance();
        this.storageReference = this.storage.getReference();

        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), MQTTHOST, MqttClient.generateClientId());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(this.USERNAME);
        options.setPassword(this.PASSWORD.toCharArray());

        try {
            IMqttToken token = mqttAndroidClient.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(MainActivity.this, "Failed to connect", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
        }


        ivCaptured = (ImageView) findViewById(R.id.ivCaptured);
        tvLabel = (TextView) findViewById(R.id.tvLabel);

        btnCamera = (Button) findViewById(R.id.btnCamera);
        btnCamera.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                initPrediction();
                fileUri = getOutputMediaFileUri(1);
                fileUpload = fileUri;
                Intent i = new Intent("android.media.action.IMAGE_CAPTURE");
                i.putExtra("output", fileUri);
                startActivityForResult(i, MainActivity.REQUEST_IMAGE_CAPTURE);
            }
        });

        btnSelect = (Button) findViewById(R.id.btnSelect);
        btnSelect.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                initPrediction();
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, REQUEST_IMAGE_SELECT);
            }
        });

        // TODO: implement a splash screen(?
        caffeMobile = new CaffeMobile();
        caffeMobile.setNumThreads(4);
        caffeMobile.loadModel(modelProto, modelBinary);

        float[] meanValues = {104, 117, 123};
        caffeMobile.setMean(meanValues);

        AssetManager am = this.getAssets();
        try {
            InputStream is = am.open("synset_words.txt");
            Scanner sc = new Scanner(is);
            List<String> lines = new ArrayList<String>();
            while (sc.hasNextLine()) {
                final String temp = sc.nextLine();
                lines.add(temp.substring(temp.indexOf(" ") + 1));
            }
            IMAGENET_CLASSES = lines.toArray(new String[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == REQUEST_IMAGE_CAPTURE || requestCode == REQUEST_IMAGE_SELECT) && resultCode == RESULT_OK) {
            String imgPath;

            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                imgPath = fileUri.getPath();
            } else {
                Uri selectedImage = data.getData();
                fileUpload = selectedImage;
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                imgPath = cursor.getString(columnIndex);
                cursor.close();
            }

            bmp = BitmapFactory.decodeFile(imgPath);
            Log.d("PATH", imgPath);
            Log.d(LOG_TAG, imgPath);
            Log.d(LOG_TAG, String.valueOf(bmp.getHeight()));
            Log.d(LOG_TAG, String.valueOf(bmp.getWidth()));

            dialog = ProgressDialog.show(MainActivity.this, "Predicting...", "Wait for one sec...", true);

            CNNTask cnnTask = new CNNTask(MainActivity.this);
            cnnTask.execute(imgPath);
        } else {
            btnCamera.setEnabled(true);
            btnSelect.setEnabled(true);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void uploadImage(Uri filePath) {
        if (filePath != null) {
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading...");
            progressDialog.show();
            StorageReference ref = storageReference.child("images/" + UUID.randomUUID().toString());
            ref.putFile(filePath).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    linkImage = taskSnapshot.getDownloadUrl().toString();
                    Log.d("LINK DOWNLOAD: ", taskSnapshot.getDownloadUrl().toString());
                    publishMessage(linkImage, tenBenh);
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Failed " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    progressDialog.setMessage("Uploaded " + ((int) ((100.0d * ((double) taskSnapshot.getBytesTransferred())) / ((double) taskSnapshot.getTotalByteCount()))) + "%");
                }
            });
        }
    }

    public void publishMessage(String linkAnh, String tenBenh) {
        Long tsLong = System.currentTimeMillis() / 1000;
        String ts = tsLong.toString();

        final String publishMessage = "{\n" +
                "  \"CameraMobile\": [\n" +
                "    {\n" +
                "      \"ts\": " + ts + "000,\n" +
                "      \"values\": {\n" +
                "        \"Ảnh\":\"" + linkAnh + "\",\n" +
                "        \"Tên Bệnh\":\"" + tenBenh + "\"" + "\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        Log.d("HEHE", publishMessage);
        String publishTopic = "v1/gateway/telemetry";
        try {
            mqttAndroidClient.publish(publishTopic, publishMessage.getBytes(), 0, false);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void initPrediction() {
        btnCamera.setEnabled(false);
        btnSelect.setEnabled(false);
        tvLabel.setText("");
    }

    @Override
    public void connectionLost(Throwable cause) {

    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

    private class CNNTask extends AsyncTask<String, Void, Integer> {
        private CNNListener listener;
        private long startTime;

        public CNNTask(CNNListener listener) {
            this.listener = listener;
        }

        @Override
        protected Integer doInBackground(String... strings) {
            startTime = SystemClock.uptimeMillis();
            return caffeMobile.predictImage(strings[0])[0];
        }

        @Override
        protected void onPostExecute(Integer integer) {
            Log.i(LOG_TAG, String.format("elapsed wall time: %d ms", SystemClock.uptimeMillis() - startTime));
            listener.onTaskCompleted(integer);
            super.onPostExecute(integer);
        }
    }

    @Override
    public void onTaskCompleted(int result) {
        ivCaptured.setImageBitmap(bmp);
        tvLabel.setText(IMAGENET_CLASSES[result]);
        tenBenh = IMAGENET_CLASSES[result];
        if (!tenBenh.equals("lá cà chua khỏe mạnh")) {
            uploadImage(fileUpload);
        }

        btnCamera.setEnabled(true);
        btnSelect.setEnabled(true);

        if (dialog != null) {
            dialog.dismiss();
        }
    }

    /**
     * Create a file Uri for saving an image or video
     */
    private static Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * Create a File for saving an image or video
     */
    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Caffe-Android-Demo");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else {
            return null;
        }

        return mediaFile;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
