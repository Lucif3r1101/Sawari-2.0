package com.app.sawaridriver;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.sawaridriver.Utils.UserUtils;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.app.sawaridriver.databinding.ActivityDriverHomeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

public class DriverHomeActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 7172;
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityDriverHomeBinding binding;
    private DrawerLayout drawer;
    private  NavController navController;
    private NavigationView navigationView;

    private AlertDialog waitingDialog;
    private StorageReference storageReference;

    private Uri imageUri;
    private ImageView img_avatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDriverHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarDriverHome.toolbar);

        drawer = binding.drawerLayout;
        navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home)
                .setOpenableLayout(drawer)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_driver_home);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        init();


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK){
            if(data != null && data.getData() != null){
                imageUri = data.getData();
                img_avatar.setImageURI(imageUri);
                
                showDialogUpload();
            }
        }
    }

    private void showDialogUpload() {
        AlertDialog.Builder builder = new AlertDialog.Builder(DriverHomeActivity.this);
        builder.setTitle("Change Avatar")
                .setMessage("Do you really want to change Avatar")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .setPositiveButton("Upload", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(imageUri != null)
                        {
                            waitingDialog.setMessage("uplaodin...");
                            waitingDialog.show();

                            String unique_name = FirebaseAuth.getInstance().getCurrentUser().getUid();
                            StorageReference avatarFolder = storageReference.child("avatars/"+unique_name);

                            avatarFolder.putFile(imageUri)
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            waitingDialog.dismiss();
                                            Snackbar.make(drawer, e.getMessage(),Snackbar.LENGTH_SHORT).show();
                                        }
                                    }).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                    if(task.isSuccessful())
                                    {
                                        avatarFolder.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                            @Override
                                            public void onSuccess(Uri uri) {
                                                Map<String, Object> updateData = new HashMap<>();
                                                updateData.put("avatar",uri.toString());

                                                UserUtils.updateUser(drawer, updateData);
                                            }
                                        });
                                    }waitingDialog.dismiss();
                                }

                            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                                    double progress = (100.0 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                                    waitingDialog.setMessage(new StringBuilder("Uploading: ").append(progress).append("%"));

                                }
                            });


                        }
                    }

                })
                .setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(getResources().getColor(R.color.colorAccent));

        });
        dialog.show();
    }

    private void init(){

        waitingDialog = new AlertDialog.Builder(this)
        .setCancelable(false)
                .setMessage("Waiting...")
                .create();

        storageReference = FirebaseStorage.getInstance().getReference();


        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if(item.getItemId() == R.id.nav_sign_out){
                    AlertDialog.Builder builder = new AlertDialog.Builder(DriverHomeActivity.this);
                    builder.setTitle("Sign OUT")
                            .setMessage("Do you really want to sign out")
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                            .setPositiveButton("SIGN OUT", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    FirebaseAuth.getInstance().signOut();
                                    Intent intent = new Intent(DriverHomeActivity.this, SplashScreenActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                }

                            })
                            .setCancelable(false);
                    AlertDialog dialog = builder.create();
                    dialog.setOnShowListener(dialogInterface -> {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                .setTextColor(ContextCompat.getColor(DriverHomeActivity.this,android.R.color.holo_red_dark));
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                                .setTextColor(ContextCompat.getColor(DriverHomeActivity.this,R.color.colorAccent));

                    });
                    dialog.show();
                }
                return true;
            }
        });
        //Set dta for user

        View headerView = navigationView.getHeaderView(0);
        TextView txt_name = (TextView)headerView.findViewById(R.id.txt_name);
        TextView txt_phone = (TextView)headerView.findViewById(R.id.txt_phone);
        TextView txt_star = (TextView)headerView.findViewById(R.id.txt_star);
        img_avatar = (ImageView)headerView.findViewById(R.id.img_avatar);


        txt_name.setText(Common.buildWelcomeMessage());
        txt_phone.setText(Common.currentUser!= null ? Common.currentUser.getPhoneNumber() : "");
        txt_star.setText(Common.currentUser!=null ? String.valueOf(Common.currentUser.getRating()) : "0.0");

        img_avatar.setOnClickListener(view -> {
            Intent intent =  new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(intent,PICK_IMAGE_REQUEST);
        });


        if(Common.currentUser != null && Common.currentUser.getAvatar() != null &&
        !TextUtils.isEmpty(Common.currentUser.getAvatar()))
        {
            Glide.with(this)
                    .load(Common.currentUser.getAvatar())
                    .into(img_avatar);
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.driver_home, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_driver_home);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}