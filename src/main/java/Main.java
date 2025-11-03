import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;

import io.github.cdimascio.dotenv.Dotenv;
import static spark.Spark.get;

public class Main {
    private static OpenAIClient client;

    public static void main(String[] args) {
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
    }
}