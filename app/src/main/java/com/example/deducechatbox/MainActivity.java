package com.example.deducechatbox;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.room.Room;

import com.example.deducechatbox.AppDatabase;
import com.example.deducechatbox.MessageEntity;
import com.example.deducechatbox.MessageDao;
import com.example.deducechatbox.ChatAdapter;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public class MainActivity extends AppCompatActivity {

    private EditText userInput;
    private Button sendButton;
    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private List<MessageEntity> chatMessages = new ArrayList<>();
    private OpenAIApi openAIApi;
    private final String API_KEY = "Bearer sk-4bb15b7bb4044719aa3c1a1fc34263eb";
    private AppDatabase db;
    private MessageDao messageDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userInput = findViewById(R.id.userInput);
        sendButton = findViewById(R.id.sendButton);
        recyclerView = findViewById(R.id.recyclerView);

        chatAdapter = new ChatAdapter(chatMessages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "chat-db").allowMainThreadQueries().build();
        messageDao = db.messageDao();

        // Load previous messages
        chatMessages.addAll(messageDao.getAllMessages());
        chatAdapter.notifyDataSetChanged();
        recyclerView.scrollToPosition(chatMessages.size() - 1);

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .addInterceptor(chain -> {
                    return chain.proceed(
                            chain.request().newBuilder()
                                    .addHeader("Authorization", API_KEY)  // Aquí se añade el API Key
                                    .build()
                    );
                })
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://ollama.deducedata.solutions/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        openAIApi = retrofit.create(OpenAIApi.class);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String input = userInput.getText().toString();
                if (!input.isEmpty()) {
                    addMessageToChat("user", input);
                    sendMessage(input,0.0,0.0,0,0,0.0);
                    userInput.setText("");
                }
            }
        });
    }

    private void sendMessage(String inputText, Double temperature, Double topK, int topP, int max_token,double rep_penalty ) {
        Message userMessage = new Message("user", inputText);
        OpenAIRequest request = new OpenAIRequest("llama3.1:latest", Arrays.asList(userMessage));

        openAIApi.sendMessage(request).enqueue(new Callback<OpenAIResponse>() {
            @Override
            public void onResponse(Call<OpenAIResponse> call, Response<OpenAIResponse> response) {
                if (response.isSuccessful()) {
                    String reply = response.body().choices.get(0).message.content;
                    addMessageToChat("assistant", reply);
                } else {
                    addMessageToChat("system", "Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<OpenAIResponse> call, Throwable t) {
                addMessageToChat("system", "Fallo: " + t.getMessage());
            }
        });
    }

    private void addMessageToChat(String role, String content) {
        MessageEntity messageEntity = new MessageEntity(role, content);
        messageDao.insertMessage(messageEntity);
        chatMessages.add(messageEntity);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        recyclerView.scrollToPosition(chatMessages.size() - 1);
    }

    interface OpenAIApi {
        @POST("api/chat/completions")
        Call<OpenAIResponse> sendMessage(@Body OpenAIRequest body);
    }

    class OpenAIRequest {
        String model;
        List<Message> messages;

        OpenAIRequest(String model, List<Message> messages) {
            this.model = model;
            this.messages = messages;
        }
    }

    class Message {
        String role;
        String content;

        Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    class OpenAIResponse {
        List<Choice> choices;
    }

    class Choice {
        Message message;
    }
}