package com.codepunks.heartfit;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import android.Manifest;
import android.widget.Toast;

/**
 * Created by user on 12/31/15.
 */
public class SecondFragment extends Fragment implements View.OnClickListener{

    View myView;
    FirebaseAuth mAuth;
    DatabaseReference cust_data;
    String userId;String mNames,mPhones;
    private EditText mName,mPhone;
    private ImageView mProfileImage;
    private Uri resultUri;
    private String mProfileUrl;
    final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        myView = inflater.inflate(R.layout.second_layout, container, false);
        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();
        cust_data = FirebaseDatabase.getInstance().getReference().child("Users").child(userId);
        getUserInfo();
        Button save = (Button) myView.findViewById(R.id.save);
        mName = (EditText) myView.findViewById(R.id.name);
        mPhone = (EditText) myView.findViewById(R.id.phone);
        mProfileImage = myView.findViewById(R.id.profileImage);

        mProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                if(ActivityCompat.checkSelfPermission(getActivity(),
//                        Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
//                {
//                    requestPermissions(
//                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
//                            2000);
//                }
//                else {
//                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//                    intent.setType("image/*");
//                    startActivityForResult(Intent.createChooser(intent, "Select Picture"),1211 );
//                }
                intent.setType("image/*");
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                startActivityForResult(intent,1211);


                }



        });
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserInformation();
            }
        });
        Button mBack = (Button) myView.findViewById(R.id.back);
        mBack.setOnClickListener(this);
        return myView;
    }

    private void getUserInfo(){
        cust_data.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0){
                    Map<String, Object>  map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name")!=null){
                        mName.setText(map.get("name").toString());
                        mNames = map.get("name").toString();
                    }
                    if(map.get("phone")!=null){
                        mPhone.setText(map.get("phone").toString());
                        mPhones = map.get("phone").toString();
                    }
                    if(map.get("profileImageUrl")!=null){
                        mProfileUrl = map.get("phone").toString();
                        Glide.with(getActivity()).load(mProfileUrl).into(mProfileImage);
                    }

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void saveUserInformation() {

        mNames = mName.getText().toString();
        mPhones = mPhone.getText().toString();
        Map userInfo = new HashMap();
        userInfo.put("name",mNames);
        userInfo.put("phone",mPhones);
        cust_data.updateChildren(userInfo);

        if(resultUri != null){

            StorageReference filePath = FirebaseStorage.getInstance().getReference().child("profile_images").child(userId);
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), resultUri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            ByteArrayOutputStream boas = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 20, boas);

            byte[] data = boas.toByteArray();
            UploadTask upload = filePath.putBytes(data);

            upload.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    FragmentManager fm = getFragmentManager();
                    fm.beginTransaction().replace(R.id.content_frame, new FirstFragment()).commit();
                    return;
                }
            });

            upload.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri downloaduri  = taskSnapshot.getDownloadUrl();

                    Map newimage = new HashMap();
                    newimage.put("profileImageUrl", downloaduri.toString());
                    cust_data.updateChildren(newimage);

                    FragmentManager fm = getFragmentManager();
                    fm.beginTransaction().replace(R.id.content_frame, new FirstFragment()).commit();
                    return;
                }
            });
        }
        else{
            FragmentManager fm = getFragmentManager();
            fm.beginTransaction().replace(R.id.content_frame, new FirstFragment()).commit();
        }


    }

    @Override
    public void onClick(View v) {
        FragmentManager fm = getFragmentManager();
        fm.beginTransaction().replace(R.id.content_frame, new FirstFragment()).commit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == 1211 && resultCode == Activity.RESULT_OK){
            final Uri imageUri = data.getData();
            resultUri = imageUri;
            Toast.makeText(getContext(), "Activity Result", Toast.LENGTH_LONG).show();
            Bitmap bitmapImage = null;
            try {
                bitmapImage = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), resultUri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            InputStream is = null;
            try {
                is = getContext().getContentResolver().openInputStream(intent.getData());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            final Bitmap imageData = BitmapFactory.decodeStream(is, null,null);
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mProfileImage.setImageBitmap(imageData);
        }
    }
}
