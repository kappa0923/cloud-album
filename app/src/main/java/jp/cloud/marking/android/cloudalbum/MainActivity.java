package jp.cloud.marking.android.cloudalbum;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int READ_REQUEST_CODE = 42;
    private static final int COLUMN_NUM = 3;

    public static final String FIRESTORE_COLLECTION_IMAGES = "images";
    public static final String FIRESTORE_KEY_FILE_URL = "fileUrl";
    public static final String FIRESTORE_KEY_FILE_NAME = "fileName";
    public static final String FIRESTORE_KEY_UPDATE_DATE = "updateDate";

    private StorageReference mStorageRef;
    private FirebaseFirestore mFirestore;
    private RecyclerView mRecyclerView;
    private GridLayoutManager mLayoutManager;
    private ImageAdapter mImageAdapter;
    private List<Map<String, Object>> mImageList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStorageRef = FirebaseStorage.getInstance().getReference();
        mFirestore = FirebaseFirestore.getInstance();

        initView();

        loadImages();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case READ_REQUEST_CODE:
                Log.d(TAG, "onActivityResult:" + requestCode + ":" + resultCode + ":" + data);
                if (resultCode == RESULT_OK) {
                    Uri fileUri = data.getData();
                    if (fileUri != null) {
                        uploadFromUri(data.getData());
                    } else {
                        Log.w(TAG, "onActivityResult: " + requestCode + " File URI is null");
                    }
                } else {
                    Toast.makeText(this, "Select Image failed.", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    /**
     * Viewの初期化処理
     */
    private void initView() {
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mRecyclerView = (RecyclerView) findViewById(R.id.content_image_list);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new GridLayoutManager(getApplicationContext(), COLUMN_NUM);
        mRecyclerView.setLayoutManager(mLayoutManager);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage();
            }
        });
    }

    private void loadImages() {
        mFirestore.collection(FIRESTORE_COLLECTION_IMAGES)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot snapshot : task.getResult()) {
                                mImageList.add(snapshot.getData());
                            }
                            mImageAdapter = new ImageAdapter(getApplicationContext(), mImageList);
                            mRecyclerView.setAdapter(mImageAdapter);
                        }
                    }
                });
    }

    /**
     * ファイルブラウザを利用して画像を選択する
     */
    private void selectImage() {
        Log.d(TAG, "selectImage");

        // ACTION_OPEN_DOCUMENT でファイルブラウザを使用するように設定
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        // カテゴリフィルタを追加
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // 画像フィルタを追加
        intent.setType("image/*");

        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    /**
     * URIから画像をStorageにアップロードする
     *
     * @param uri 選択した画像のURI
     */
    private void uploadFromUri(Uri uri) {
        Log.d(TAG, "uploadFromUri: src: " + uri.toString());

        String fileName = getUploadFileNameFromUri(uri);

        StorageReference uploadRef = mStorageRef.child("images/" + fileName);
        uploadRef.putFile(uri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Uri downloadUri = taskSnapshot.getDownloadUrl();
                        Log.d(TAG, "onSuccess: " + downloadUri.toString());
                        updateUploadedFileInfo(downloadUri.toString(), taskSnapshot.getMetadata().getName());
                        Toast.makeText(MainActivity.this, "Upload Success", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "Upload Failure", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * URIからアップロード用のファイル名を取得する
     *
     * @param fileUri URI
     * @return ファイル名
     */
    private String getUploadFileNameFromUri(Uri fileUri) {
        long currentTime = System.currentTimeMillis();
        String mimeType = getContentResolver().getType(fileUri);
        String fileExtension = mimeType.substring(mimeType.lastIndexOf("/") + 1);

        Log.d(TAG, "uploadFromUri: " + currentTime + ": " + mimeType);

        return currentTime + "." + fileExtension;
    }

    /**
     * アップロード後の情報をデータベースに書き込む
     *
     * @param fileUrl  アップロードされたファイルのURL
     * @param fileName アップロードされたファイル名
     */
    private void updateUploadedFileInfo(String fileUrl, String fileName) {
        Map<String, Object> image = new HashMap<>();
        image.put(FIRESTORE_KEY_FILE_URL, fileUrl);
        image.put(FIRESTORE_KEY_FILE_NAME, fileName);
        image.put(FIRESTORE_KEY_UPDATE_DATE, new Date());

        mFirestore.collection(FIRESTORE_COLLECTION_IMAGES)
                .add(image)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "onSuccess: Firestore updated " + documentReference.toString());
                    }
                });

        mImageList.add(image);
        mImageAdapter.notifyDataSetChanged();
    }
}

