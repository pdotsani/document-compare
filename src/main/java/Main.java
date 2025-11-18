import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.beta.threads.Thread;
import com.openai.models.beta.threads.messages.Message;
import com.openai.models.beta.threads.messages.MessageContent;
import com.openai.models.beta.threads.messages.MessageListPage;
import com.openai.models.beta.threads.messages.MessageListParams;
import com.openai.models.files.FileObject;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.vectorstores.VectorStore;
import com.openai.models.vectorstores.VectorStoreCreateParams;

import io.github.cdimascio.dotenv.Dotenv;
import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.post;

public class Main {
    private static OpenAIClient client;
    private static AssistantCompare assistantCompare;

    static class CompareRequestBody {
        @SerializedName("vector")
        public String vector;

        public String getVector() {
            return vector;
        }
    }

    public static void main(String[] args) {
        Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
        
        // Load environment variables
        Dotenv dotenv = Dotenv
            .configure()
            .filename(".env.local")
            .load();
        String apiKey = dotenv.get("OPENAI_API_KEY");

        // Initialize OpenAI client
        client = OpenAIOkHttpClient.builder()
            .apiKey(apiKey)
            .build();

        assistantCompare = new AssistantCompare(client);
            
        get("/hello", (req, res) -> {
            ResponseCreateParams params = ResponseCreateParams.builder()
                .input("Why is California great?")
                .model(ChatModel.GPT_4_1)
                .build();

            Response response = client.responses().create(params);

            res.type("application/json");
            
            return response.output();
        });

        get("/analyze", (req, res) -> {
            List<FileObject> files =HandleFiles.view(client);
            if (files.size() != 2) {
                res.status(400);
                return "Need 2 files to procsss";
            }

            List<String> fileIds = new ArrayList<>();
            
            fileIds.add(files.get(0).id());
            fileIds.add(files.get(1).id());

            VectorStore vectorStore = client.vectorStores().create(
                VectorStoreCreateParams.builder()
                    .fileIds(fileIds)
                    .build()
            );

            res.type("application/json");
            
            return gson.toJson(Map.of("response", vectorStore.id()));
        });

        post("/compare", (req, res) -> {
            CompareRequestBody body = gson.fromJson(req.body(), CompareRequestBody.class);
            String vector = body.getVector();

            String response = assistantCompare.compare(client, vector);

            res.type("application/json");
            
            return gson.toJson(Map.of("response", response));
        });

        post("/upload", (req, res) -> {
            try {
                req.raw().setAttribute("org.eclipse.jetty.multipartConfig", 
                    new MultipartConfigElement(""));
                
                Part file = req.raw().getPart("file");

                if (file == null) {
                    res.status(400);
                    return "{\"error\":\"No file provided\"}";
                }

                // If there are two files, delete the older file
                List<FileObject> files = HandleFiles.view(client);
                if (files.size() == 2) {
                    HandleFiles.delete(client, files.get(0).id());
                }

                FileObject fileObject = HandleFiles.upload(client, file);
                
                res.type("application/json");

                return gson.toJson(Map.of(
                    "id", fileObject.id(),
                    "filename", fileObject.filename(),
                    "purpose", fileObject.purpose().value()
                ));
        
            } catch (Exception e) {
                res.status(500);
                return gson.toJson(Map.of("error", e.getMessage()));
            }
        });

        get("/view-thread/:thread_id", (req, res) -> {
            try {
                String threadId = req.params(":thread_id");
                Thread thread = client.beta().threads().retrieve(threadId);
                
                MessageListPage messagesList = client.beta().threads().messages().list(
                    thread.id(),
                    MessageListParams.builder().build()
                );

                List<List<MessageContent>> content = new ArrayList<>();

                for (Message message : messagesList.data()) {
                    content.add(message.content());
                }
                

                res.type("application/json");

                return gson.toJson(Map.of(
                    "id", thread.id(),
                    "messages", content
                ));

            } catch (Exception e) {
                res.status(500);
                return gson.toJson(Map.of("error", e.getMessage()));
            }
        });

        get("/view", (req, res) -> {
            List<FileObject> files = HandleFiles.view(client);
            res.type("application/json");

            List<Map<String, Object>> fileList = new ArrayList<>();
            for (FileObject file : files) {
                fileList.add(Map.of(
                    "id", file.id(),
                    "filename", file.filename(),
                    "purpose", file.purpose().value(),
                    "created_at", file.createdAt()
                ));
            }
            return gson.toJson(fileList);
        });

        get("/view-vector-store", (req, res) -> {
            List<VectorStore> vectorStores = client.vectorStores().list().data();
            res.type("application/json");

            List<String> vectorList = new ArrayList<>();
            for (VectorStore vectorStore : vectorStores) {
                vectorList.add(vectorStore.id());
            }

            return gson.toJson(vectorList);
        });

        delete("/delete-vector-store/:file_id", (req, res) -> {
            String fileId = req.params(":file_id");
            System.out.println("Deleting file: " + fileId);
            client.vectorStores().delete(fileId);
            res.status(200);
            return "File deleted";
        });

        delete("/delete/:file_id", (req, res) -> {
            String fileId = req.params(":file_id");
            System.out.println("Deleting file: " + fileId);
            HandleFiles.delete(client, fileId);
            res.status(200);
            return "File deleted";
        });
    }
}