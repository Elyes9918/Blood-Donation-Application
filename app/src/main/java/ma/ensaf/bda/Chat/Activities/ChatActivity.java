package ma.ensaf.bda.Chat.Activities;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import ma.ensaf.bda.Chat.Netwrok.ApiClient;
import ma.ensaf.bda.Chat.Netwrok.ApiService;
import ma.ensaf.bda.Chat.UtilitiesChat.ChatAdapter;
import ma.ensaf.bda.Models.Message;
import ma.ensaf.bda.Models.User;
import ma.ensaf.bda.Utilities.Constants;
import ma.ensaf.bda.databinding.ActivityChatBinding;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static ma.ensaf.bda.Utilities.Constants.KEY_FCM_TOKEN;
import static ma.ensaf.bda.Utilities.Constants.KEY_ID;
import static ma.ensaf.bda.Utilities.Constants.KEY_MESSAGE;
import static ma.ensaf.bda.Utilities.Constants.KEY_NAME;
import static ma.ensaf.bda.Utilities.Constants.KEY_RECEIVER_ID;
import static ma.ensaf.bda.Utilities.Constants.KEY_SENDER_ID;
import static ma.ensaf.bda.Utilities.Constants.KEY_TABLE_CHAT;
import static ma.ensaf.bda.Utilities.Constants.KEY_TIMESTAMP;
import static ma.ensaf.bda.Utilities.Constants.KEY_USER;
import static ma.ensaf.bda.Utilities.Constants.REMOTE_MSG_DATA;
import static ma.ensaf.bda.Utilities.Constants.REMOTE_MSG_REGISTRATION_IDS;

public class ChatActivity extends BaseActivity {

    ActivityChatBinding binding;

    private User receiverUser;
    private FirebaseUser senderUser;

    private ChatAdapter chatAdapter;
    private List<Message> chatList;

    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        super.onCreate(savedInstanceState);
        setContentView(binding.getRoot());
        init();
        setListeners();
        populateRecyclerView();
        checkOnline();

    }

    private void checkOnline() {
        if(receiverUser.getAvailability().equals("online")){
            binding.textAvailability.setVisibility(View.VISIBLE);
        }else{
            binding.textAvailability.setVisibility(View.GONE);
        }
    }


    private void init(){
        //get Sender User
        senderUser = FirebaseAuth.getInstance().getCurrentUser();
        //get Receiver User
        receiverUser = (User) getIntent().getSerializableExtra(KEY_USER);

        binding.textName.setText(receiverUser.getName());
    }

    private void showToast(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void setListeners() {
        binding.layoutSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(binding.inputMessage.getText().toString()==""){
                showToast("You cant send empty messages");
                }else{
                    createMessages();
                    binding.inputMessage.setText("");
                }

            }
        });

        binding.imageBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(),MainChatActivity.class);
                startActivity(intent);
                finish();
            }
        });

        binding.imageInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(),ReceiverProfile.class);
                intent.putExtra(KEY_USER, receiverUser);
                startActivity(intent);
            }
        });
    }


    private void populateRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        binding.chatRecyclerView.setLayoutManager(layoutManager);

        chatList = new ArrayList<>();
        chatAdapter = new ChatAdapter(getApplicationContext(),chatList,senderUser.getUid(), receiverUser.getProfilepictureurl());
        binding.chatRecyclerView.setAdapter(chatAdapter);

        readMessages();
    }

    private void readMessages() {
        DatabaseReference reference =FirebaseDatabase.getInstance().getReference().child(KEY_TABLE_CHAT);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatList.clear();
                for(DataSnapshot dataSnapshot : snapshot.getChildren()){
                    Message message = dataSnapshot.getValue(Message.class);

                    if(message != null) {
                        if ((message.getReceiverId().equals(receiverUser.getId()) && message.getSenderId().equals(senderUser.getUid())) ||
                                (message.getReceiverId().equals(senderUser.getUid()) && message.getSenderId().equals(receiverUser.getId()))) {
                            chatList.add(message);
                        }
                    }

                }
                chatAdapter.notifyDataSetChanged();
                binding.progressBar.setVisibility(View.GONE);


                if(chatList.isEmpty()){
                    binding.progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private String getReadableDateTime(Date date){
        return new SimpleDateFormat("MM, dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private void createMessages(){
        mDatabase = FirebaseDatabase.getInstance().getReference();

        HashMap<String,Object> chatInfo = new HashMap<>();
        chatInfo.put(KEY_MESSAGE,binding.inputMessage.getText().toString());
        chatInfo.put(KEY_SENDER_ID,senderUser.getUid());
        chatInfo.put(KEY_RECEIVER_ID, receiverUser.getId());
        chatInfo.put(KEY_TIMESTAMP,getReadableDateTime(new Date()));

        mDatabase.child(KEY_TABLE_CHAT).push().setValue(chatInfo);


        //Added For Notification Functionality
        if(receiverUser.getAvailability().equals("offline")){
            try{
                JSONArray tokens = new JSONArray();
                tokens.put(receiverUser.getToken());

                JSONObject data = new JSONObject();
                data.put(KEY_ID,receiverUser.getId());
                data.put(KEY_NAME,receiverUser.getName());
                data.put(KEY_FCM_TOKEN,receiverUser.getToken());
                data.put(KEY_MESSAGE,binding.inputMessage.getText().toString());

                JSONObject body = new JSONObject();
                body.put(REMOTE_MSG_DATA,data);
                body.put(REMOTE_MSG_REGISTRATION_IDS,tokens);

                sendNotification(body.toString());

            }catch(Exception exception){
                showToast(exception.getMessage());

            }
        }
        binding.inputMessage.setText(null);

    }

    private void sendNotification(String messageBody){
        ApiClient.getClient().create(ApiService.class).sendMessage(
                Constants.getRemoteMsgHeaders(),
                messageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if(response.isSuccessful()){
                    try{
                        if(response.body() != null){
                            JSONObject responseJson = new JSONObject(response.body());
                            JSONArray results=responseJson.getJSONArray("results");
                            if(responseJson.getInt("failure")==1){
                                JSONObject error = (JSONObject) results.get(0);
                                showToast(error.getString("error"));
                                return;
                            }
                        }
                        showToast("Notification sent Successfully");
                    }catch(JSONException e){
                       // e.printStackTrace();
                        showToast("Hello from catch");
                    }

                }else{
                    showToast("Error: "+response.code());
                }
            }

            @Override
            public void onFailure(@NonNull  Call<String> call,@NonNull Throwable t) {
                //showToast(t.getMessage());
                //showToast("From Faillure");
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }





}