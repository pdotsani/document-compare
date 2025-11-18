import java.util.List;

import com.openai.client.OpenAIClient;
import com.openai.models.beta.assistants.Assistant;
import com.openai.models.beta.assistants.AssistantCreateParams;
import com.openai.models.beta.assistants.AssistantTool;
import com.openai.models.beta.assistants.FileSearchTool;
import com.openai.models.beta.threads.Thread;
import com.openai.models.beta.threads.ThreadCreateParams;
import com.openai.models.beta.threads.ThreadCreateParams.ToolResources;
import com.openai.models.beta.threads.ThreadCreateParams.ToolResources.FileSearch;
import com.openai.models.beta.threads.messages.MessageCreateParams;
import com.openai.models.beta.threads.runs.Run;
import com.openai.models.beta.threads.runs.RunCreateParams;

public class AssistantCompare {
  final private Assistant assistant;

  public AssistantCompare(OpenAIClient client) {
    assistant = client.beta().assistants().create(
      AssistantCreateParams.builder()
        .name("Document Comparison Assistant")
        .model("gpt-4-turbo-preview")
        .instructions("You are a document comparison assistant. Compare the documents in the vector store and provide a detailed summary of the key differences.")
        .tools(List.of(
          AssistantTool.ofFileSearch(
            FileSearchTool.builder().build()
          )
        ))
        .build()
    );
  }

  public String compare(OpenAIClient client, String vector) {
      try {
        System.out.println("Build Thread");
        Thread thread = client.beta().threads().create(
          ThreadCreateParams.builder()
              .toolResources(
                  ToolResources.builder()
                      .fileSearch(
                          FileSearch.builder()
                              .vectorStoreIds(List.of(vector))
                              .build()
                      )
                      .build()
              )
              .build()
        );

        System.out.println("Run Thread");

        client.beta().threads().messages().create(
            thread.id(),
            MessageCreateParams.builder()
                .role(MessageCreateParams.Role.USER)
                .content("Compare the documents in the vector store and summarize the key differences.")
                .build()
        );

        Run run = client.beta().threads().runs().create(
          thread.id(),
          RunCreateParams.builder()
              .assistantId(assistant.id())
              .build()
        );

        return run.toString();
          
      } catch (Exception e) {
        System.err.println("Error details: " + e.getMessage());
        e.printStackTrace();
        throw e;
      }
      
  }
}