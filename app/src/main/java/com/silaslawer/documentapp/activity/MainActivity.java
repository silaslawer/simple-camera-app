package com.silaslawer.documentapp.activity;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.koushikdutta.async.http.AsyncSSLSocketMiddleware;
import com.koushikdutta.ion.Ion;
import com.silaslawer.documentapp.R;
import com.silaslawer.documentapp.util.ConnectionDetector;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import dmax.dialog.SpotsDialog;

import static com.silaslawer.documentapp.util.APILinks.FILE_UPLOAD_ENDPOINT;

public class MainActivity extends AppCompatActivity {

    Button btnTakeImage;
    Button btnRetake;
    Button btnUpload;

    ImageView imageViewDocument;

    Bitmap thumbnail;

    final int PERMISSIONS_MULTIPLE_REQUEST = 999;

    String imagePath = "";

    ConstraintLayout imageDisplayVisibility;


    SharedPreferences prefs;
    SharedPreferences.Editor edit;
    private SpotsDialog progressDialog;

    ConnectionDetector connectionDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkAndroidVersion();


        btnTakeImage = findViewById(R.id.btnTakeImage);
        imageDisplayVisibility = findViewById(R.id.imageDisplayVisibility);

        imageViewDocument = findViewById(R.id.imageViewDocument);

        prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        edit = prefs.edit();


        btnRetake = findViewById(R.id.btnRetake);
        btnUpload = findViewById(R.id.btnUpload);

        progressDialog = new SpotsDialog(this, R.style.Custom);
        progressDialog.setCancelable(true);
        connectionDetector = new ConnectionDetector(this);

        getUserId();

        btnTakeImage.setOnClickListener(v -> {

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, PERMISSIONS_MULTIPLE_REQUEST);
        });

        btnRetake.setOnClickListener(v -> {
            btnTakeImage.setVisibility(View.VISIBLE);
            imageDisplayVisibility.setVisibility(View.GONE);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, PERMISSIONS_MULTIPLE_REQUEST);
        });

        btnUpload.setOnClickListener(v -> {

            try {
                validationDocument();
            } catch (UnrecoverableKeyException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (KeyStoreException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CertificateException e) {
                e.printStackTrace();
            }
        });
    }

    public void getUserId() {
        long randomId = (long) (Math.random()*Math.pow(10,10));

        edit.putString("userId", String.valueOf(randomId));
        edit.commit();
    }

    private void checkAndroidVersion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission();

        } else {
            // write your logic here
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (requestCode == PERMISSIONS_MULTIPLE_REQUEST  && resultCode == RESULT_OK && data != null) {

                onCaptureImageResult(data);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission();

        } else {
            // write your logic here
            onCaptureImageResult(data);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_MULTIPLE_REQUEST:
                if (grantResults.length > 0) {
                    boolean cameraPermission = grantResults[2] == PackageManager.PERMISSION_GRANTED;
                    boolean writeExternalFile = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean readExternalFile = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if(cameraPermission && writeExternalFile && readExternalFile)
                    {
                        // write your logic here
                    } else {
                        Snackbar.make(MainActivity.this.findViewById(android.R.id.content),
                                "Please Grant Permissions to upload profile photo",
                                Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                                v -> {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        requestPermissions(
                                                new String[]{Manifest.permission
                                                        .READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA},
                                                PERMISSIONS_MULTIPLE_REQUEST);
                                    }
                                }).show();
                    }
                }
                break;
        }
    }

    private void onCaptureImageResult(Intent data) {
        thumbnail = (Bitmap) data.getExtras().get("data");

        Uri tempUri = getImageUri(getApplicationContext(), thumbnail);

         imagePath =  getRealPathFromURI(tempUri);

        System.out.println(getRealPathFromURI(tempUri));


        //set Image from Camera
        File image_file = new File(imagePath);
        if (image_file.exists()) {
            btnTakeImage.setVisibility(View.GONE);
            imageDisplayVisibility.setVisibility(View.VISIBLE);
            Picasso.with(this).load(new File(imagePath)).into(imageViewDocument);
            edit.putString("image_string", imagePath);
            edit.commit();
        }
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) + ContextCompat
                .checkSelfPermission(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)+ ContextCompat
                .checkSelfPermission(MainActivity.this,
                        Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale
                    (MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale
                            (MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale
                            (MainActivity.this, Manifest.permission.CAMERA)) {

                Snackbar.make(MainActivity.this.findViewById(android.R.id.content),
                        "Please Grant Permissions to upload profile photo",
                        Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                        v -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                requestPermissions(
                                        new String[]{Manifest.permission
                                                .READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA},
                                        PERMISSIONS_MULTIPLE_REQUEST);
                            }
                        }).show();
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(
                            new String[]{Manifest.permission
                                    .READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA},
                            PERMISSIONS_MULTIPLE_REQUEST);
                }
            }
        } else {
            // write your logic code if permission already granted
        }
    }

    public String getRealPathFromURI(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
        return cursor.getString(idx);
    }

    private void validationDocument() throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, URISyntaxException, IOException, CertificateException {
        connectionDetector = new ConnectionDetector(this);
        progressDialog.show();
        if (connectionDetector.isNetworkAvailable()) {

            TrustManager[] wrappedTrustManagers = new TrustManager[]{
                    new X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        }

                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[]{};
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, wrappedTrustManagers, null);

            AsyncSSLSocketMiddleware sslMiddleWare = Ion.getDefault(this).getHttpClient().getSSLSocketMiddleware();
            sslMiddleWare.setTrustManagers(wrappedTrustManagers);
            sslMiddleWare.setHostnameVerifier((hostname, session) -> true);
            sslMiddleWare.setSSLContext(sslContext);

            File imageFile = new File(prefs.getString("image_string", ""));
            System.out.println(prefs.getString("image_string", ""));
            Ion.with(this)
                    .load("POST", FILE_UPLOAD_ENDPOINT)
                    .setLogging("Document Logs", Log.DEBUG)
                    .setMultipartParameter("user_id", prefs.getString("userId", ""))
                    .setMultipartFile("document", imageFile)
                    .asJsonObject()
                    .withResponse()
                    .setCallback((e, result) -> {
                        Log.d("Results", String.valueOf(result.getHeaders().code()));
                        if (result.getHeaders().code() == 200) {
                            try {
                                    progressDialog.dismiss();
                                    imageDisplayVisibility.setVisibility(View.GONE);
                                    btnTakeImage.setVisibility(View.VISIBLE);
                                    Toast.makeText(this, "Validation Successful", Toast.LENGTH_SHORT).show();
                                    //displayDialog(getResources().getString(R.string.invalid_username_password));



                            } catch (Exception ex) {
                                ex.printStackTrace();
                                progressDialog.dismiss();
                                //displayDialog(getResources().getString(R.string.unknown_error));
                            }
                        } else {
                            try {
                                String jsonString = result.getResult().toString();
                                JSONObject jsonObject = new JSONObject(jsonString);
                                progressDialog.dismiss();
                                Toast.makeText(this, jsonObject.getString("message"), Toast.LENGTH_SHORT).show();
                                //displayDialog(getResources().getString(R.string.unknown_error));
                            } catch (JSONException ex) {
                                progressDialog.dismiss();

                                ex.printStackTrace();
                            }


                        }
                    });
        } else {

            progressDialog.dismiss();
            // displayDialog(getResources().getString(R.string.network_unavailable));

            Toast.makeText(this, "Network unavailable. Please try again later.", Toast.LENGTH_SHORT).show();
        }

    }

    private HostnameVerifier getHostnameVerifier() {
        return (hostname, session) -> {
            return true;
            // or the following:
            // HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
            // return hv.verify("www.yourserver.com", session);
        };
    }

    private TrustManager[] getWrappedTrustManagers(TrustManager[] trustManagers) {
        final X509TrustManager originalTrustManager = (X509TrustManager) trustManagers[0];
        return new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return originalTrustManager.getAcceptedIssuers();
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        try {
                            if (certs != null && certs.length > 0){
                                certs[0].checkValidity();
                            } else {
                                originalTrustManager.checkClientTrusted(certs, authType);
                            }
                        } catch (CertificateException e) {
                            Log.w("checkClientTrusted", e.toString());
                        }
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        try {
                            if (certs != null && certs.length > 0){
                                certs[0].checkValidity();
                            } else {
                                originalTrustManager.checkServerTrusted(certs, authType);
                            }
                        } catch (CertificateException e) {
                            Log.w("checkServerTrusted", e.toString());
                        }
                    }
                }
        };
    }
}
