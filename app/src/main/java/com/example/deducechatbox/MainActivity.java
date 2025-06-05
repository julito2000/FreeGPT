package com.example.deducechatbox;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;

import androidx.room.Room;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
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
    private Spinner selApi;
    private String model = "";
    private ProgressBar spinner;
    private ImageButton audio;
    private MediaRecorder recorder;
    private String audioFilePath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userInput = findViewById(R.id.userInput);
        sendButton = findViewById(R.id.sendButton);
        recyclerView = findViewById(R.id.recyclerView);
        selApi = findViewById(R.id.selecAPI);

        chatAdapter = new ChatAdapter(chatMessages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);
        spinner = findViewById(R.id.progressBar);
        audio = findViewById(R.id.sendAudio);


        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "chat-db").allowMainThreadQueries().build();
        messageDao = db.messageDao();

        // Creamos el Array adapter usando el string array y el aspecto por defecto del spinner.
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.api_array,
                android.R.layout.simple_spinner_item
        );

        //Especifica el aspecto a usar cuando se despliega la lista de opciones
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Aplicamos el layout al spinner
        selApi.setAdapter(adapter);


        //Carga los mensajes anteriores en el Recicler view
        chatMessages.addAll(messageDao.getAllMessages());
        chatAdapter.notifyDataSetChanged();
        recyclerView.scrollToPosition(chatMessages.size() - 1);

        //Cliente para acceder a la  API con la  API_KEY Correspondiente
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
                .connectTimeout(0, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://ollama.deducedata.solutions/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        openAIApi = retrofit.create(OpenAIApi.class);

        selApi.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                model = (String) selApi.getItemAtPosition(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String input = userInput.getText().toString();
                if (!input.isEmpty()) {
                    addMessageToChat("user", input);
                    sendMessage(input, model, 0.0, 0.0, 0, 0, 0.0);
                    userInput.setText("");
                }
            }
        });

/*        audio.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    startRecording();
                    return true;
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    stopRecording();
                    return true;
                }

                return false;
            }
        });*/
    }


    private void sendMessage(String inputText, String model, Double temperature, Double topK, int topP, int max_token, double rep_penalty) {
        Message userMessage = new Message("user", inputText);

        //Pasamos el historico de la conversacion a la API para que recuerde lo que hemos hablado
        List<Message> fullConversation = new ArrayList<>();
        for (MessageEntity msg : chatMessages) {
            fullConversation.add(new Message(msg.getRole(), msg.getContent()));
        }
        fullConversation.add(userMessage); // también añadimos el nuevo mensaje


        OpenAIRequest request = new OpenAIRequest(model, fullConversation);
        spinner.setVisibility(View.VISIBLE);

        openAIApi.sendMessage(request).enqueue(new Callback<OpenAIResponse>() {
            @Override
            public void onResponse(Call<OpenAIResponse> call, Response<OpenAIResponse> response) {
                spinner.setVisibility(View.GONE); // Ocultar spinner
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

    /// /////////////////////////////////////////////////////
    /// // Codigo de la grabacion de audio y envio a la API//
    /// /////////////////////////////////////////////////////
/*
    private void startRecording() {
        try {
            File audioFile = File.createTempFile("recording_", ".m4a", getCacheDir());
            audioFilePath = audioFile.getAbsolutePath();

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);

            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setOutputFile(audioFilePath);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.prepare();
            recorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        if (recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;

            sendAudioToWhisper(new File(audioFilePath));
        }
    }
    private void sendAudioToWhisper(File audioFile) {
        RequestBody requestFile = RequestBody.create(MediaType.parse("audio/m4a"), audioFile);
        MultipartBody.Part audioPart = MultipartBody.Part.createFormData("file", audioFile.getName(), requestFile);
        RequestBody model = RequestBody.create(MediaType.parse("text/plain"), "whisper-1");

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openai.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WhisperApi whisperApi = retrofit.create(WhisperApi.class);
        whisperApi.transcribeAudio("Bearer sk-4bb15b7bb4044719aa3c1a1fc34263eb", audioPart, model)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(Call<WhisperResponse> call, Response<WhisperResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String transcript = response.body().getText();
                            addMessageToChat("user", transcript);
                            sendMessage(transcript, 0.0, 0.0, 0, 0, 0.0);
                        } else {
                            Log.e("Whisper", "Error: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<WhisperResponse> call, Throwable t) {
                        Log.e("Whisper", "Error de red: " + t.getMessage());
                    }
                });
    }

    interface WhisperApi {
        @Multipart
        @POST("v1/audio/transcriptions")
        Call<WhisperResponse> transcribeAudio(
                @Header("Authorization") String authHeader,
                @Part MultipartBody.Part file,
                @Part("model") RequestBody model
        );
    }

    class WhisperResponse {
        @SerializedName("text")
        private String text;

        public String getText() {
            return text;
        }
    }*/

}