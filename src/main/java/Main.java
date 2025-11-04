import java.net.URL;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.files.FileObject;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;

import io.github.cdimascio.dotenv.Dotenv;
import static spark.Spark.get;
import static spark.Spark.post;

public class Main {
    private static OpenAIClient client;
    private static HandleFiles handleFiles;

    public static void main(String[] args) {
        Gson gson = new Gson();
        
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
            
        get("/hello", (req, res) -> {
            ResponseCreateParams params = ResponseCreateParams.builder()
                .input("Why is California great?")
                .model(ChatModel.GPT_4_1)
                .build();

            Response response = client.responses().create(params);

            res.type("application/json");
            
            return response.output();
        });

        post("/upload", (req, res) -> {
            try {
                String fileUrl = req.queryParams("url");
                URL url = new URL(fileUrl);

                FileObject fileObject = handleFiles.upload(client, url);
                
                res.type("application/json");

                return new Gson().toJson(Map.of(
                    "id", fileObject.id(),
                    "filename", fileObject.filename(),
                    "purpose", fileObject.purpose(),
                    "status", fileObject.status()
                ));
        
            } catch (Exception e) {
                res.status(500);
                return new Gson().toJson(Map.of("error", e.getMessage()));
            }
        });

        get("/view", (req, res) -> {
            List<FileObject> files = handleFiles.view(client);
            res.type("application/json");
            return new Gson().toJson(files);
        });
    }
}