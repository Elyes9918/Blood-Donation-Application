package ma.ensaf.bda.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;

import ma.ensaf.bda.R;
import ma.ensaf.bda.activities.NewMainActivity;
import ma.ensaf.bda.databinding.SignUpDonorTabFragmentBinding;
import ma.ensaf.bda.utilities.Constants;

import static android.app.Activity.RESULT_OK;
import static ma.ensaf.bda.utilities.Constants.KEY_PROFILE_PICTURE_URL;

public class SignUpDonorTabFragment extends Fragment {

    SignUpDonorTabFragmentBinding binding;

    private FirebaseAuth mAuth;
    private DatabaseReference userDatabaseRef;
    private Uri resultUri;

    boolean isPasswordVisible;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        binding = SignUpDonorTabFragmentBinding.inflate(getLayoutInflater());
        //ViewGroup root = (ViewGroup) inflater.inflate(R.layout.sign_up_donor_tab_fragment, container, false);

        init();
        setListeners();
        return binding.getRoot();
    }

    private void init()
    {
        mAuth = FirebaseAuth.getInstance();
    }

    private void setListeners()
    {
        setVisibilityListener(binding.registerPassword);
        setVisibilityListener(binding.confirmPassword);

        binding.registerButton.setOnClickListener(v -> {
            if(isValidSignUpDetails())
            {
                signUp();
            }
        });


    }


    private void showToast(String message)
    {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void signUp()
    {
        mAuth.createUserWithEmailAndPassword(
                binding.registerEmail.getText().toString().trim(),
                binding.registerPassword.getText().toString().trim()
        ).addOnCompleteListener(task -> {
            if(!task.isSuccessful())
            {
                showToast("Error " + task.getException().getMessage());
            } else {
                String currentUserId = mAuth.getCurrentUser().getUid();
                userDatabaseRef = FirebaseDatabase.getInstance().getReference()
                        .child(Constants.KEY_COLLECTION_USERS).child(currentUserId);
                HashMap<String, Object> userInfo = new HashMap<>();
                userInfo.put(Constants.KEY_ID, currentUserId);
                userInfo.put(Constants.KEY_NAME, binding.registerFullName.getText().toString());
                userInfo.put(Constants.KEY_EMAIL, binding.registerEmail.getText().toString().trim());
                userInfo.put(Constants.KEY_BLOOD_GROUP, binding.bloodGroupSpinner.getSelectedItem().toString());
                userInfo.put(Constants.KEY_TYPE, "donor");
                userInfo.put(Constants.KEY_SEARCH, "donor" + binding.bloodGroupSpinner.getSelectedItem().toString());
                userInfo.put(Constants.KEY_ANONYMOUS, binding.anonymousSwitch.isChecked());


                userDatabaseRef.updateChildren(userInfo);


                Intent intent = new Intent(getContext(), NewMainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                //getActivity().finish();
            }

        });

    }




    private Boolean isValidSignUpDetails()
    {
        if(TextUtils.isEmpty(binding.registerFullName.getText().toString().trim()))
        {
            binding.registerFullName.setError("Full name is required");
            return false;
        }

        if(TextUtils.equals(binding.bloodGroupSpinner.getSelectedItem().toString(), "Select your blood group"))
        {
            showToast("Select blood group");
            return false;
        }

        if(TextUtils.isEmpty(binding.registerEmail.getText().toString().trim()))
        {
            binding.registerEmail.setError("Email is required");
            return false;
        }

        if(!Patterns.EMAIL_ADDRESS.matcher(binding.registerEmail.getText().toString()).matches())
        {
            binding.registerEmail.setError("Enter a valid email");
            return false;
        }

        if(TextUtils.isEmpty(binding.registerPassword.getText().toString().trim()))
        {
            binding.registerPassword.setError("Password is required");
            return false;
        }
        if(TextUtils.isEmpty(binding.confirmPassword.getText().toString().trim()))
        {
            binding.confirmPassword.setError("Confirm your password");
            return false;
        }
        if(!binding.registerPassword.getText().toString().equals(binding.confirmPassword.getText().toString()))
        {
            showToast("Passwords do not match ");
            return false;
        }


        return true;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setVisibilityListener(EditText editText)
    {
        editText.setOnTouchListener((v1, event) -> {
            final int RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (editText.getRight() - editText.getCompoundDrawables()[RIGHT].getBounds().width())) {
                    int selection = editText.getSelectionEnd();
                    if (isPasswordVisible) {
                        // set drawable image
                        editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_visibility_24, 0);
                        // hide Password
                        editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                        isPasswordVisible = false;
                    } else  {
                        // set drawable image
                        editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_visibility_off_24, 0);
                        // show Password
                        editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                        isPasswordVisible = true;
                    }
                    editText.setSelection(selection);
                    return true;
                }
            }
            return false;
        });
    }
}
